package com.hackathon.finance.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantRequest(
        @NotBlank @Size(max = 2000) String message
) {
}
