package com.utama.my_inventory.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO de respuesta para subcategorías")
public record SubcategoryResponseDTO(

        @Schema(description = "ID de la subcategoría", example = "1")
        Long id,

        @Schema(description = "Nombre de la subcategoría", example = "Smartphones")
        String name,

        @Schema(description = "Estado activo", example = "true")
        Boolean active,

        @Schema(description = "ID de la categoría padre", example = "1")
        Long categoryId,

        @Schema(description = "Nombre de la categoría padre", example = "Electrónica")
        String categoryName,

        @Schema(description = "Cantidad de productos", example = "15")
        Integer productCount,

        @Schema(description = "Fecha de creación", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de última actualización", example = "2024-01-15T10:30:00")
        LocalDateTime updatedAt
) {}
