package com.utama.my_inventory.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para login")
public record LoginRequestDTO(

        @NotBlank(message = "El usuario es requerido")
        @Schema(
                description = "Nombre de usuario administrador",
                example = "admin",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String username,

        @NotBlank(message = "La contraseña es requerida")
        @Schema(
                description = "Contraseña del administrador",
                example = "MiClaveSegura123!",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String password
) {}