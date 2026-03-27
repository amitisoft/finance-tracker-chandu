package com.hackathon.finance.controller;

import com.hackathon.finance.dto.insight.HealthScoreResponse;
import com.hackathon.finance.dto.insight.InsightItemResponse;
import com.hackathon.finance.service.InsightsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;

    @GetMapping("/health-score")
    public HealthScoreResponse healthScore() {
        return insightsService.getHealthScore();
    }

    @GetMapping
    public List<InsightItemResponse> insights() {
        return insightsService.getInsights();
    }
}
