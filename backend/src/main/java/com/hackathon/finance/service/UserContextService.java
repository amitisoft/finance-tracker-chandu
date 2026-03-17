package com.hackathon.finance.service;

import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.exception.UnauthorizedException;
import com.hackathon.finance.repository.UserRepository;
import com.hackathon.finance.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserContextService {

    private final UserRepository userRepository;

    public UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new UnauthorizedException("Authentication is required.");
        }
        return userRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists."));
    }
}
