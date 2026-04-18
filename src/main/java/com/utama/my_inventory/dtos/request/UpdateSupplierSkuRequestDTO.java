package com.utama.my_inventory.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para actualizar SKU de proveedor")
public record UpdateSupplierSkuRequestDTO(
        @NotBlank(message = "SKU es obligatorio")
        @Size(max = 50, message = "SKU no puede exceder 50 caracteres")
        @Schema(description = "Nuevo SKU del proveedor", example = "HP-ELITE-002")
        String supplierSku
) {}