package com.hackathon.finance.dto.report;

import java.math.BigDecimal;

public record AccountBalanceTrendItem(String accountName, BigDecimal balance) {
}
