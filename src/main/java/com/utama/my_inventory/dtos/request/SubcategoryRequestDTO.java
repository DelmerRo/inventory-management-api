package com.utama.my_inventory.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para crear o actualizar una subcategoría")
public record SubcategoryRequestDTO(

        @NotBlank(message = "El nombre de la subcategoría es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        @Schema(
                description = "Nombre de la subcategoría",
                example = "Smartphones",
                minLength = 2,
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @NotNull(message = "El ID de la categoría es obligatorio")
        @Schema(
                description = "ID de la categoría padre",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long categoryId
) {}
