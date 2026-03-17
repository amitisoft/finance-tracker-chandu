package com.hackathon.finance.dto.account;

import com.hackathon.finance.entity.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull AccountType type,
        @NotNull @DecimalMin(value = "0.00") BigDecimal openingBalance,
        @Size(max = 120) String institutionName
) {
}
