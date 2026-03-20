package com.hackathon.finance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.finance.config.AssistantAiProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiAssistantClient {

    private final AssistantAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    public boolean isConfigured() {
        return properties.enabled()
                && properties.apiKey() != null
                && !properties.apiKey().isBlank()
                && properties.baseUrl() != null
                && !properties.baseUrl().isBlank()
                && properties.model() != null
                && !properties.model().isBlank();
    }

    public AssistantIntentResult interpret(String message) {
        if (!isConfigured()) {
            return null;
        }
        try {
            String payload = objectMapper.writeValueAsString(buildRequestBody(message));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.baseUrl()))
                    .timeout(Duration.ofSeconds(Math.max(properties.timeoutSeconds(), 5)))
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI assistant request failed with status " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.isNull() || contentNode.asText().isBlank()) {
                throw new IllegalStateException("OpenAI assistant returned an empty content payload.");
            }
            return objectMapper.readValue(extractJson(contentNode.asText()), AssistantIntentResult.class);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("OpenAI assistant call failed.", exception);
        }
    }

    private String extractJson(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private OpenAiChatRequest buildRequestBody(String message) {
        String systemPrompt = """
                You are an intent parser for a personal finance assistant.
                Convert the user's message into a strict JSON object only.
                Never include markdown, prose, or extra keys.
                Use these intents only:
                CREATE_EXPENSE, ACCOUNT_BALANCE, REMAINING_BUDGET, UPDATE_BUDGET, BUDGET_STATUS, EXPENSE_SUMMARY, IRRELEVANT.
                Rules:
                - If the user is asking to record or register a spending item, use CREATE_EXPENSE even if grammar is broken.
                - If the user asks to add, increase, reduce, change, set, or update a budget, use UPDATE_BUDGET.
                - If the user asks whether any budget is exceeded, over budget, or near limit, use BUDGET_STATUS.
                - Infer category names from finance context when obvious, for example rent -> Rent, biryani/restaurant/meal -> Food, uber/metro/cab -> Transport.
                - For balance questions, populate accountName when mentioned.
                - For budget questions, populate budgetCategory only when clearly mentioned.
                - For expense summary, set timeRange to WEEK or MONTH when identifiable.
                - For budget updates, set budgetOperation to INCREASE, DECREASE, or SET when identifiable.
                - If the message is too noisy, incomplete, or ambiguous for a safe write, set needsClarification=true and include clarificationQuestion.
                - Return amounts as plain decimal strings without currency symbols.
                JSON schema:
                {
                  "intent": "CREATE_EXPENSE|ACCOUNT_BALANCE|REMAINING_BUDGET|UPDATE_BUDGET|BUDGET_STATUS|EXPENSE_SUMMARY|IRRELEVANT",
                  "needsClarification": true|false,
                  "clarificationQuestion": "string or null",
                  "amount": "decimal string or null",
                  "merchant": "string or null",
                  "category": "string or null",
                  "accountName": "string or null",
                  "budgetCategory": "string or null",
                  "timeRange": "WEEK|MONTH|null",
                  "budgetOperation": "INCREASE|DECREASE|SET|null"
                }
                """;
        return new OpenAiChatRequest(
                properties.model(),
                properties.temperature(),
                new Message[]{
                        new Message("system", systemPrompt),
                        new Message("user", message)
                },
                new ResponseFormat(
                        "json_schema",
                        new JsonSchema(
                                "assistant_intent",
                                true,
                                """
                                        {
                                          "type": "object",
                                          "additionalProperties": false,
                                          "properties": {
                                            "intent": {
                                              "type": "string",
                                              "enum": ["CREATE_EXPENSE", "ACCOUNT_BALANCE", "REMAINING_BUDGET", "UPDATE_BUDGET", "BUDGET_STATUS", "EXPENSE_SUMMARY", "IRRELEVANT"]
                                            },
                                            "needsClarification": { "type": "boolean" },
                                            "clarificationQuestion": { "type": ["string", "null"] },
                                            "amount": { "type": ["string", "null"] },
                                            "merchant": { "type": ["string", "null"] },
                                            "category": { "type": ["string", "null"] },
                                            "accountName": { "type": ["string", "null"] },
                                            "budgetCategory": { "type": ["string", "null"] },
                                            "timeRange": {
                                              "type": ["string", "null"],
                                              "enum": ["WEEK", "MONTH", null]
                                            },
                                            "budgetOperation": {
                                              "type": ["string", "null"],
                                              "enum": ["INCREASE", "DECREASE", "SET", null]
                                            }
                                          },
                                          "required": [
                                            "intent",
                                            "needsClarification",
                                            "clarificationQuestion",
                                            "amount",
                                            "merchant",
                                            "category",
                                            "accountName",
                                            "budgetCategory",
                                            "timeRange",
                                            "budgetOperation"
                                          ]
                                        }
                                        """
                        )
                )
        );
    }

    public record AssistantIntentResult(
            String intent,
            boolean needsClarification,
            String clarificationQuestion,
            String amount,
            String merchant,
            String category,
            String accountName,
            String budgetCategory,
            String timeRange,
            String budgetOperation
    ) {
    }

    private record OpenAiChatRequest(
            String model,
            double temperature,
            Message[] messages,
            ResponseFormat response_format
    ) {
    }

    private record Message(String role, String content) {
    }

    private record ResponseFormat(String type, JsonSchema json_schema) {
    }

    private record JsonSchema(String name, boolean strict, String schema) {
    }
}
