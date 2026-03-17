package com.hackathon.finance.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void accountAndTransactionFlowShouldUpdateBalances() throws Exception {
        String token = bearerToken(registerUser("sam@example.com", "Password1", "Sam"));

        String accountId = createAccount(token, "Salary Account", "BANK_ACCOUNT", BigDecimal.valueOf(5000));
        String categoryId = findFirstExpenseCategory(token);

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "EXPENSE",
                                "amount", 1250.50,
                                "date", LocalDate.now().toString(),
                                "accountId", accountId,
                                "categoryId", categoryId,
                                "merchant", "QA Cafe",
                                "note", "Office lunch",
                                "tags", new String[]{"team"}
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchant").value("QA Cafe"));

        mockMvc.perform(get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentBalance").value(3749.5));
    }

    private JsonNode registerUser(String email, String password, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password,
                                "displayName", displayName
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearerToken(JsonNode response) {
        return "Bearer " + response.get("accessToken").asText();
    }

    private String createAccount(String token, String name, String type, BigDecimal openingBalance) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "type", type,
                                "openingBalance", openingBalance,
                                "institutionName", "HDFC"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String findFirstExpenseCategory(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode categories = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode category : categories) {
            if ("EXPENSE".equals(category.get("type").asText())) {
                return category.get("id").asText();
            }
        }
        throw new IllegalStateException("No expense category found");
    }
}
