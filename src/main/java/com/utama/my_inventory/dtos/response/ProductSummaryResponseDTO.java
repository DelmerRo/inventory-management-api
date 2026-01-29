package com.utama.my_inventory.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta resumida del producto para listados")
public record ProductSummaryResponseDTO(

        @Schema(description = "ID del producto", example = "1")
        Long id,

        @Schema(description = "SKU del producto", example = "PROD-001")
        String sku,

        @Schema(description = "Nombre del producto", example = "Laptop HP EliteBook")
        String name,

        @Schema(description = "Precio de venta", example = "1500.00")
        BigDecimal salePrice,

        @Schema(description = "Stock actual", example = "10")
        Integer currentStock,

        @Schema(description = "Subcategoría", example = "Laptops")
        String subcategoryName,

        @Schema(description = "Proveedor", example = "HP Inc.")
        String supplierName,

        @Schema(description = "Indica si hay stock disponible", example = "true")
        Boolean hasStock,

        @Schema(description = "Indica si está activo", example = "true")
        Boolean active
) {}