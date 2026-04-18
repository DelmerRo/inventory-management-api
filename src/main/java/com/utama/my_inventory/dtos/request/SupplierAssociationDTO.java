package com.utama.my_inventory.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para asociar un proveedor a un producto")
public record SupplierAssociationDTO(

        @NotNull(message = "ID del proveedor es obligatorio")
        @Schema(description = "ID del proveedor", example = "1")
        Long supplierId,

        @Size(max = 50, message = "SKU del proveedor no puede exceder 50 caracteres")
        @Schema(description = "SKU que usa el proveedor para este producto", example = "HP-ELITE-001")
        String supplierSku,

        @Schema(description = "Indica si es el proveedor principal", example = "true")
        Boolean isPrimary,

        @Size(max = 255, message = "Nota no puede exceder 255 caracteres")
        @Schema(description = "Notas adicionales sobre la relación con el proveedor", example = "Proveedor oficial con garantía extendida")
        String notes
) {}