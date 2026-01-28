package com.utama.my_inventory.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Respuesta estándar de la API")
public record ApiResponseDTO<T>(

        @Schema(description = "Indica si la operación fue exitosa", example = "true")
        boolean success,

        @Schema(description = "Mensaje descriptivo de la operación")
        String message,

        @Schema(description = "Datos de la respuesta")
        T data,

        @Schema(description = "Timestamp de la respuesta")
        LocalDateTime timestamp
) {

    public static <T> ApiResponseDTO<T> success(T data, String message) {
        return new ApiResponseDTO<>(true, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponseDTO<T> success(T data) {
        return success(data, "Operación exitosa");
    }

    public static <T> ApiResponseDTO<T> error(String message) {
        return new ApiResponseDTO<>(false, message, null, LocalDateTime.now());
    }
}