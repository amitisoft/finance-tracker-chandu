package com.hackathon.finance.controller;

import com.hackathon.finance.dto.budget.BudgetRequest;
import com.hackathon.finance.dto.budget.BudgetResponse;
import com.hackathon.finance.service.BudgetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public List<BudgetResponse> getBudgets(@RequestParam int month, @RequestParam int year) {
        return budgetService.getBudgets(month, year);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@Valid @RequestBody BudgetRequest request) {
        return budgetService.create(request);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(@PathVariable UUID id, @Valid @RequestBody BudgetRequest request) {
        return budgetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        budgetService.delete(id);
    }
}
