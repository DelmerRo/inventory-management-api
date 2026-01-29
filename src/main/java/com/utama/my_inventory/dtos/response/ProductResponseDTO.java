package com.utama.my_inventory.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta con información detallada del producto")
public record ProductResponseDTO(

        @Schema(description = "ID del producto", example = "1")
        Long id,

        @Schema(description = "Código único del producto", example = "PROD-001")
        String sku,

        @Schema(description = "Nombre del producto", example = "Laptop HP EliteBook")
        String name,

        @Schema(description = "Descripción detallada", example = "Laptop empresarial con 16GB RAM, 512GB SSD")
        String description,

        @Schema(description = "Precio de costo", example = "1200.50")
        BigDecimal costPrice,

        @Schema(description = "Precio de venta", example = "1500.00")
        BigDecimal salePrice,

        @Schema(description = "Precio de promoción", example = "1400.00")
        BigDecimal promoPrice,

        @Schema(description = "Stock actual", example = "10")
        Integer currentStock,

        @Schema(description = "Margen de ganancia")
        BigDecimal margin,

        @Schema(description = "Porcentaje de margen", example = "25.00")
        BigDecimal marginPercentage,

        @Schema(description = "Volumen calculado", example = "2218.75")
        BigDecimal volume,

        @JsonProperty("subcategory")
        @Schema(description = "Subcategoría del producto")
        SubcategoryResponseDTO subcategory,

        @JsonProperty("supplier")
        @Schema(description = "Proveedor del producto")
        SupplierResponseDTO supplier,

        @Schema(description = "Peso en kg", example = "1.500")
        BigDecimal weight,

        @Schema(description = "Largo en cm", example = "35.50")
        BigDecimal length,

        @Schema(description = "Ancho en cm", example = "25.00")
        BigDecimal width,

        @Schema(description = "Alto en cm", example = "2.50")
        BigDecimal height,

        @Schema(description = "Unidad de medida", example = "cm")
        String measureUnit,

        @Schema(description = "Indica si el producto está activo", example = "true")
        Boolean active,

        @Schema(description = "Fecha de creación")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de última actualización")
        LocalDateTime updatedAt,

        @Schema(description = "Fecha de última compra")
        LocalDateTime lastPurchaseAt,

        @Schema(description = "Indica si hay stock disponible", example = "true")
        Boolean hasStock,

        @Schema(description = "Indica si el stock es bajo", example = "false")
        Boolean lowStock
) {

    public static ProductResponseDTOBuilder builder() {
        return new ProductResponseDTOBuilder();
    }
}