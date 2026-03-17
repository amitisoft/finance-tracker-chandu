package com.hackathon.finance.security;

import com.hackathon.finance.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecurityProperties properties;
    private final SecretKey key;

    public JwtService(SecurityProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secretBytes.length >= 32 ? secretBytes : Decoders.BASE64.decode(properties.jwt().secret()));
    }

    public String generateAccessToken(AppUserPrincipal principal) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiry = now.plusMinutes(properties.jwt().accessTokenMinutes());
        return Jwts.builder()
                .issuer(properties.jwt().issuer())
                .subject(principal.getUsername())
                .claim("uid", principal.id().toString())
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(expiry.toInstant()))
                .signWith(key)
                .compact();
    }

    public OffsetDateTime accessExpiry() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(properties.jwt().accessTokenMinutes());
    }

    public OffsetDateTime refreshExpiry() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(properties.jwt().refreshTokenDays());
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).get("uid", String.class));
    }
}
