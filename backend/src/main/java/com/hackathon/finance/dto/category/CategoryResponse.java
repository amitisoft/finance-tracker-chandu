package com.hackathon.finance.dto.category;

import com.hackathon.finance.entity.enums.CategoryType;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, CategoryType type, String color, String icon, boolean archived) {
}
