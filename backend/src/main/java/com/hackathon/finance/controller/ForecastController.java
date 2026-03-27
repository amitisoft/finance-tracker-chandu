package com.hackathon.finance.controller;

import com.hackathon.finance.dto.forecast.CashFlowForecastResponse;
import com.hackathon.finance.dto.forecast.DailyForecastPointResponse;
import com.hackathon.finance.dto.forecast.MonthlyForecastResponse;
import com.hackathon.finance.service.CashFlowForecastService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final CashFlowForecastService cashFlowForecastService;

    @GetMapping("/cash-flow")
    public CashFlowForecastResponse cashFlowForecast(@RequestParam(defaultValue = "6") int months) {
        return cashFlowForecastService.getForecast(months);
    }

    @GetMapping("/month")
    public MonthlyForecastResponse monthForecast() {
        return cashFlowForecastService.getMonthlyForecast();
    }

    @GetMapping("/daily")
    public List<DailyForecastPointResponse> dailyForecast() {
        return cashFlowForecastService.getDailyForecast();
    }
}
