package com.utama.my_inventory.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta con información detallada del proveedor")
public record SupplierResponseDTO(

        @Schema(description = "ID del proveedor", example = "1")
        Long id,

        @Schema(description = "Nombre del proveedor", example = "Tecnología S.A.")
        String name,

        @Schema(description = "Persona de contacto", example = "Juan Pérez")
        String contactPerson,

        @Schema(description = "Email de contacto", example = "contacto@tecnologia.com")
        String email,

        @Schema(description = "Teléfono de contacto", example = "+51 999 888 777")
        String phone,

        @Schema(description = "Dirección física", example = "Av. Tecnología 123, Lima")
        String address,

        @Schema(description = "Notas adicionales")
        String notes,

        @Schema(description = "Fecha de registro")
        LocalDateTime registeredAt,

        @Schema(description = "Indica si el proveedor está activo", example = "true")
        Boolean active,

        @Schema(description = "Fecha de creación")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de última actualización")
        LocalDateTime updatedAt,

        @Schema(description = "Cantidad de productos activos", example = "15")
        Long productCount
) {}