package com.hackathon.finance.controller;

import com.hackathon.finance.dto.report.AccountBalanceTrendItem;
import com.hackathon.finance.dto.report.CategorySpendItem;
import com.hackathon.finance.dto.report.IncomeExpenseTrendItem;
import com.hackathon.finance.dto.report.NetWorthPointResponse;
import com.hackathon.finance.dto.report.TrendsReportResponse;
import com.hackathon.finance.service.InsightsService;
import com.hackathon.finance.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final InsightsService insightsService;

    @GetMapping("/category-spend")
    public List<CategorySpendItem> categorySpend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return reportService.getCategorySpend(fromDate, toDate);
    }

    @GetMapping("/income-vs-expense")
    public List<IncomeExpenseTrendItem> incomeVsExpense(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return reportService.getIncomeVsExpense(fromDate, toDate);
    }

    @GetMapping("/account-balance-trend")
    public List<AccountBalanceTrendItem> accountBalanceTrend() {
        return reportService.getAccountBalanceTrend();
    }

    @GetMapping("/trends")
    public TrendsReportResponse trends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return insightsService.getTrends(fromDate, toDate);
    }

    @GetMapping("/net-worth")
    public List<NetWorthPointResponse> netWorth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return insightsService.getNetWorthTrend(fromDate, toDate);
    }

    @GetMapping("/export.csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return reportService.exportTransactionsCsv(fromDate, toDate);
    }
}
