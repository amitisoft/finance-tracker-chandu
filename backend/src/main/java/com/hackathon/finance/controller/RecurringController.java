package com.hackathon.finance.controller;

import com.hackathon.finance.dto.recurring.RecurringRequest;
import com.hackathon.finance.dto.recurring.RecurringResponse;
import com.hackathon.finance.service.RecurringService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringController {

    private final RecurringService recurringService;

    @GetMapping
    public List<RecurringResponse> getAll() {
        return recurringService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringResponse create(@Valid @RequestBody RecurringRequest request) {
        return recurringService.create(request);
    }

    @PutMapping("/{id}")
    public RecurringResponse update(@PathVariable UUID id, @Valid @RequestBody RecurringRequest request) {
        return recurringService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        recurringService.delete(id);
    }
}
