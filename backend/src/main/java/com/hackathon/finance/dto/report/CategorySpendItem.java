package com.hackathon.finance.dto.report;

import java.math.BigDecimal;

public record CategorySpendItem(String categoryName, BigDecimal total) {
}
