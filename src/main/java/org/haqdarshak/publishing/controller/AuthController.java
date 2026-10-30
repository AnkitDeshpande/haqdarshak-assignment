package org.haqdarshak.publishing.controller;

import jakarta.validation.Valid;
import org.haqdarshak.publishing.dto.request.AuthRequest;
import org.haqdarshak.publishing.dto.response.ApiResponse;
import org.haqdarshak.publishing.dto.response.AuthResponse;
import org.haqdarshak.publishing.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/token")
    public ResponseEntity<ApiResponse<AuthResponse>> generateToken(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.generateToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token generated successfully", response));
    }
}
