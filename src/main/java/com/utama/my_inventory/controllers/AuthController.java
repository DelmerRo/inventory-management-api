package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.BaseResponse;
import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.LoginRequestDTO;
import com.utama.my_inventory.dtos.response.LoginResponseDTO;
import com.utama.my_inventory.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "Endpoints para login y gestión de sesión")
public class AuthController {

    private final AuthService authService; // Interface, no implementación

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Autentica un usuario con username y password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    })
    public ResponseEntity<ExtendedBaseResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO login = authService.login(request);
        return ExtendedBaseResponse.ok(login, "Login exitoso").toResponseEntity();
    }


    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión",
            description = "Termina la sesión actual del usuario")
    public ResponseEntity<BaseResponse> logout() {

        authService.logout();

        return BaseResponse.ok("Sesión cerrada exitosamente")
                .toResponseEntity();
    }

}