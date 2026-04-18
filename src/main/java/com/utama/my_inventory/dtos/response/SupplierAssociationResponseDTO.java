package com.utama.my_inventory.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "DTO para mostrar la relación producto-proveedor")
public record SupplierAssociationResponseDTO(

        @Schema(description = "ID de la relación")
        Long id,

        @Schema(description = "ID del proveedor")
        Long supplierId,

        @Schema(description = "Nombre del proveedor")
        String supplierName,

        @Schema(description = "SKU del proveedor para este producto")
        String supplierSku,

        @Schema(description = "Es proveedor principal", example = "true")
        Boolean isPrimary,

        @Schema(description = "Notas adicionales")
        String notes
) {}