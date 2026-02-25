package com.utama.my_inventory.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de login exitoso")
public record LoginResponseDTO(

        @Schema(description = "Token de autenticación", example = "YWRtaW46TWlDbGF2ZVNlZ3VyYTEyMyE=")
        String token,

        @Schema(description = "Tipo de token", example = "Basic")
        String tokenType,

        @Schema(description = "Usuario autenticado", example = "admin")
        String username,

        @Schema(description = "Rol del usuario", example = "ADMIN")
        String role,

        @Schema(description = "Fecha y hora del login")
        LocalDateTime loginTime,

        @Schema(description = "Tiempo de expiración en minutos", example = "1440")
        Integer expiresIn
) {}