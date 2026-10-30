package org.haqdarshak.publishing.service;

import org.haqdarshak.publishing.config.JwtTokenProvider;
import org.haqdarshak.publishing.domain.enums.UserRole;
import org.haqdarshak.publishing.dto.request.AuthRequest;
import org.haqdarshak.publishing.dto.response.AuthResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse generateToken(AuthRequest request) {
        UserRole role = request.getRole() != null ? request.getRole() : UserRole.FIELD_AGENT;
        String token = jwtTokenProvider.createToken(request.getUsername(), role);
        return new AuthResponse(token, request.getUsername(), role.name());
    }
}
