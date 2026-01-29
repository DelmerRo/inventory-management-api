package com.utama.my_inventory.dtos.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para crear o actualizar un proveedor")
public record SupplierRequestDTO(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
        @Schema(description = "Nombre del proveedor", example = "Tecnología S.A.")
        String name,

        @Size(max = 150, message = "El contacto no puede exceder 150 caracteres")
        @Schema(description = "Persona de contacto", example = "Juan Pérez")
        String contactPerson,

        @Email(message = "Email inválido")
        @Size(max = 100, message = "El email no puede exceder 100 caracteres")
        @Schema(description = "Email de contacto", example = "contacto@tecnologia.com")
        String email,

        @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$", message = "Teléfono inválido")
        @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
        @Schema(description = "Teléfono de contacto", example = "+51 999 888 777")
        String phone,

        @Size(max = 500, message = "La dirección no puede exceder 500 caracteres")
        @Schema(description = "Dirección física", example = "Av. Tecnología 123, Lima")
        String address,

        @Size(max = 2000, message = "Las notas no pueden exceder 2000 caracteres")
        @Schema(description = "Notas adicionales", example = "Proveedor oficial de componentes electrónicos")
        String notes
) {}