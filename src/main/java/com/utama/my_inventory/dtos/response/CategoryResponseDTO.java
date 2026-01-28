package com.utama.my_inventory.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO de respuesta para categorías")
public record CategoryResponseDTO(

        @Schema(description = "ID de la categoría", example = "1")
        Long id,

        @Schema(description = "Nombre de la categoría", example = "Electrónica")
        String name,

        @Schema(description = "Estado activo", example = "true")
        Boolean active,

        @Schema(description = "Cantidad de subcategorías", example = "5")
        Integer subcategoryCount,

        @Schema(description = "Subcategorías (solo en detalle)")
        List<SubcategoryResponseDTO> subcategories,

        @Schema(description = "Fecha de creación", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de última actualización", example = "2024-01-15T10:30:00")
        LocalDateTime updatedAt
) {}
