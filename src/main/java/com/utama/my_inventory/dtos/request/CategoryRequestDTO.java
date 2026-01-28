package com.utama.my_inventory.dtos.request;



import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para crear o actualizar una categoría")
public record CategoryRequestDTO(

        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        @Schema(
                description = "Nombre de la categoría",
                example = "Electrónica",
                minLength = 2,
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @Schema(
                description = "Estado activo de la categoría",
                example = "true",
                defaultValue = "true"
        )
        Boolean active
) {
    public CategoryRequestDTO {
        if (active == null) {
            active = true;
        }
    }
}