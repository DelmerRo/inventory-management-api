package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.LoginRequestDTO;
import com.utama.my_inventory.dtos.response.LoginResponseDTO;
import com.utama.my_inventory.security.JwtTokenProvider;
import com.utama.my_inventory.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider; // Inyectar

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        String username = request.username();
        String password = request.password();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String token = jwtTokenProvider.generateToken(username, "ADMIN");

        log.info("Login exitoso para usuario: {}", username);

        return new LoginResponseDTO(
                token,
                "Bearer",
                username,
                "ADMIN",
                LocalDateTime.now(),
                1440  // minutos de expiración
        );
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
        log.info("Usuario deslogueado");
    }
}