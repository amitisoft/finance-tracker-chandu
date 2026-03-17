package com.hackathon.finance.dto.category;

import com.hackathon.finance.entity.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull CategoryType type,
        @Size(max = 20) String color,
        @Size(max = 50) String icon
) {
}
