package com.hackathon.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record SecurityProperties(Jwt jwt, String frontendUrl) {
    public record Jwt(String issuer, String secret, long accessTokenMinutes, long refreshTokenDays) {
    }
}
