package com.hackathon.finance.controller;

import com.hackathon.finance.dto.assistant.AssistantRequest;
import com.hackathon.finance.dto.assistant.AssistantResponse;
import com.hackathon.finance.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/message")
    public AssistantResponse message(@Valid @RequestBody AssistantRequest request) {
        return assistantService.handle(request);
    }
}
