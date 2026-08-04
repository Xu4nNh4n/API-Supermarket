package com.api.supermarket.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.supermarket.dto.response.AuthMeResponse;
import com.api.supermarket.dto.request.LoginRequest;
import com.api.supermarket.dto.request.RefreshTokenRequest;
import com.api.supermarket.dto.response.LoginResponse;
import com.api.supermarket.dto.response.TokenResponse;
import com.api.supermarket.service.AuthService;
import com.api.supermarket.service.RefreshTokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth") 
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService){
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        return authService.login(request);
    }
    
    @GetMapping("/me")
    public AuthMeResponse me(Authentication authentication){
        return authService.me(authentication.getName());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(
        @Valid @RequestBody RefreshTokenRequest request
    ){
        return refreshTokenService.refresh(request.getRefreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request){
        refreshTokenService.revoke(request.getRefreshToken());
        return  ResponseEntity.noContent().build();
    }
}
