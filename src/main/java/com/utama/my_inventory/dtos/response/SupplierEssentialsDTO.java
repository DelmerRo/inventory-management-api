package com.utama.my_inventory.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Información esencial de proveedor")
public record SupplierEssentialsDTO(

        @Schema(description = "ID del proveedor", example = "1")
        Long id,

        @Schema(description = "Nombre del proveedor", example = "Textures Home")
        String name,

        @Schema(description = "Persona de contacto", example = "Sofía Martínez")
        String contactPerson,

        @Schema(description = "Email de contacto", example = "contacto@textures.com")
        String email,

        @Schema(description = "Teléfono", example = "+54 381 234 5678")
        String phone,

        @Schema(description = "Dirección", example = "Av. Sarmiento 321, Tucumán")
        String address,

        @Schema(description = "Fecha de registro")
         LocalDateTime registeredAt,

        @Schema(description = "Indica si está activo", example = "true")
        Boolean active
) {}