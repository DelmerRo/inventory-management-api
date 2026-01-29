package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.LoginRequestDTO;
import com.utama.my_inventory.dtos.response.LoginResponseDTO;
import com.utama.my_inventory.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        String username = request.username();
        String password = request.password();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // Establecer autenticación en contexto
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String credentials = username + ":" + password;
        String token = Base64.getEncoder().encodeToString(credentials.getBytes());

        log.info("Login exitoso para usuario: {}. Token generado.", username);

        return new LoginResponseDTO(
                token,                     // token
                "Bearer",                  // tokenType: CAMBIADO a "Bearer"
                username,                  // username
                "ADMIN",                   // role
                LocalDateTime.now(),       // loginTime
                1440                       // expiresIn
        );
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
        log.info("Usuario deslogueado");
    }
}