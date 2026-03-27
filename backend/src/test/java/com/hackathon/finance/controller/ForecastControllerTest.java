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
class ForecastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void cashFlowForecastShouldReturnProjectedPeriods() throws Exception {
        String token = bearerToken(registerUser("forecast@example.com", "Password1", "Forecast"));
        String accountId = createAccount(token, "Main", "BANK_ACCOUNT", BigDecimal.valueOf(12000));
        String incomeCategoryId = findCategoryByType(token, "INCOME");
        String expenseCategoryId = findCategoryByType(token, "EXPENSE");

        createTransaction(token, Map.of(
                "type", "INCOME",
                "amount", 5000,
                "date", LocalDate.now().minusMonths(1).withDayOfMonth(5).toString(),
                "accountId", accountId,
                "categoryId", incomeCategoryId,
                "merchant", "Salary"
        ));
        createTransaction(token, Map.of(
                "type", "EXPENSE",
                "amount", 1500,
                "date", LocalDate.now().minusMonths(1).withDayOfMonth(12).toString(),
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "merchant", "Rent"
        ));
        createRecurring(token, Map.of(
                "title", "Monthly Rent",
                "type", "EXPENSE",
                "amount", 1200,
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "frequency", "MONTHLY",
                "startDate", LocalDate.now().withDayOfMonth(10).toString(),
                "autoCreateTransaction", true,
                "paused", false
        ));

        mockMvc.perform(get("/api/forecasts/cash-flow")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("months", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecastMonths").value(4))
                .andExpect(jsonPath("$.periods.length()").value(4))
                .andExpect(jsonPath("$.averageMonthlyIncome").isNumber())
                .andExpect(jsonPath("$.averageMonthlyExpense").isNumber())
                .andExpect(jsonPath("$.periods[0].projectedExpense").isNumber())
                .andExpect(jsonPath("$.healthSignal").isString());
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
                                "institutionName", "ICICI"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void createTransaction(String token, Map<String, Object> payload) throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    private void createRecurring(String token, Map<String, Object> payload) throws Exception {
        mockMvc.perform(post("/api/recurring")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    private String findCategoryByType(String token, String type) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode categories = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode category : categories) {
            if (type.equals(category.get("type").asText())) {
                return category.get("id").asText();
            }
        }
        throw new IllegalStateException("No category found for type " + type);
    }
}
