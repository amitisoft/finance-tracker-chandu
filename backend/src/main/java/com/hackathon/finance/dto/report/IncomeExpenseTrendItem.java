package com.hackathon.finance.dto.report;

import java.math.BigDecimal;

public record IncomeExpenseTrendItem(String month, BigDecimal income, BigDecimal expense) {
}
