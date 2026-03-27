package com.hackathon.finance.dto.insight;

import java.util.List;

public record HealthScoreResponse(
        double score,
        String band,
        List<HealthScoreBreakdownItem> breakdown,
        List<String> suggestions
) {
}
