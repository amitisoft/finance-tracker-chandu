package com.hackathon.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.assistant.gemini")
public record AssistantAiProperties(
        boolean enabled,
        String apiKey,
        String model,
        String baseUrl,
        int timeoutSeconds,
        double temperature
) {
}
