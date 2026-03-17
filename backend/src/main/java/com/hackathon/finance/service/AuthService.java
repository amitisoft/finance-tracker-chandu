package com.hackathon.finance.service;

import com.hackathon.finance.dto.auth.AuthResponse;
import com.hackathon.finance.dto.auth.ForgotPasswordRequest;
import com.hackathon.finance.dto.auth.LoginRequest;
import com.hackathon.finance.dto.auth.MessageResponse;
import com.hackathon.finance.dto.auth.RefreshTokenRequest;
import com.hackathon.finance.dto.auth.RegisterRequest;
import com.hackathon.finance.dto.auth.ResetPasswordRequest;
import com.hackathon.finance.entity.PasswordResetTokenEntity;
import com.hackathon.finance.entity.RefreshTokenEntity;
import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.entity.enums.CategoryType;
import com.hackathon.finance.exception.ConflictException;
import com.hackathon.finance.exception.UnauthorizedException;
import com.hackathon.finance.mapper.EntityMapper;
import com.hackathon.finance.repository.CategoryRepository;
import com.hackathon.finance.repository.PasswordResetTokenRepository;
import com.hackathon.finance.repository.RefreshTokenRepository;
import com.hackathon.finance.repository.UserRepository;
import com.hackathon.finance.security.AppUserPrincipal;
import com.hackathon.finance.security.JwtService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final List<String> EXPENSE_CATEGORIES = List.of("Food", "Rent", "Utilities", "Transport", "Entertainment", "Shopping", "Health", "Education", "Travel", "Subscriptions", "Miscellaneous");
    private static final List<String> INCOME_CATEGORIES = List.of("Salary", "Freelance", "Bonus", "Investment", "Gift", "Refund", "Other");

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHashService tokenHashService;
    private final EntityMapper mapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists.");
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.email().trim().toLowerCase());
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        seedDefaultCategories(user);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials."));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = tokenHashService.hash(request.refreshToken());
        RefreshTokenEntity refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(token -> !token.isRevoked() && token.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or expired."));
        refreshToken.setRevoked(true);
        return issueTokens(refreshToken.getUser());
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();
            PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
            resetToken.setUser(user);
            resetToken.setTokenHash(tokenHashService.hash(rawToken));
            resetToken.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
            passwordResetTokenRepository.save(resetToken);
        });
        return new MessageResponse("If the account exists, a password reset token has been generated.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetTokenEntity token = passwordResetTokenRepository.findByTokenHash(tokenHashService.hash(request.token()))
                .filter(candidate -> !candidate.isUsed() && candidate.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)))
                .orElseThrow(() -> new UnauthorizedException("Reset token is invalid or expired."));
        token.setUsed(true);
        token.getUser().setPasswordHash(passwordEncoder.encode(request.newPassword()));
        return new MessageResponse("Password reset successfully.");
    }

    private AuthResponse issueTokens(UserEntity user) {
        AppUserPrincipal principal = AppUserPrincipal.from(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshTokenRaw = UUID.randomUUID() + "-" + UUID.randomUUID();
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHashService.hash(refreshTokenRaw));
        refreshToken.setExpiresAt(jwtService.refreshExpiry());
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(accessToken, refreshTokenRaw, jwtService.accessExpiry(), mapper.toUserSummary(user));
    }

    private void seedDefaultCategories(UserEntity user) {
        EXPENSE_CATEGORIES.forEach(name -> categoryRepository.save(defaultCategory(user, name, CategoryType.EXPENSE)));
        INCOME_CATEGORIES.forEach(name -> categoryRepository.save(defaultCategory(user, name, CategoryType.INCOME)));
    }

    private com.hackathon.finance.entity.CategoryEntity defaultCategory(UserEntity user, String name, CategoryType type) {
        com.hackathon.finance.entity.CategoryEntity category = new com.hackathon.finance.entity.CategoryEntity();
        category.setUser(user);
        category.setName(name);
        category.setType(type);
        category.setColor(type == CategoryType.EXPENSE ? "#ef4444" : "#10b981");
        category.setIcon(name.toLowerCase().replace(" ", "-"));
        return category;
    }
}
