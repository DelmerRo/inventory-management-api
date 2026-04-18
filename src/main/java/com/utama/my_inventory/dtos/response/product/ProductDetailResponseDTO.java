package com.utama.my_inventory.dtos.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.utama.my_inventory.dtos.response.SubcategoryEssentialsDTO;
import com.utama.my_inventory.dtos.response.SupplierAssociationResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta con información detallada pero simplificada del producto")
public record ProductDetailResponseDTO(

        @Schema(description = "ID del producto", example = "1")
        Long id,

        @Schema(description = "Código único del producto", example = "LIV-DOR-00001")
        String sku,

        @Schema(description = "Nombre del producto", example = "Laptop HP EliteBook")
        String name,

        @Schema(description = "Descripción detallada", example = "Laptop empresarial con 16GB RAM, 512GB SSD")
        String description,

        @Schema(description = "Precio de costo", example = "1200.50")
        BigDecimal costPrice,

        @Schema(description = "Precio de venta", example = "1500.00")
        BigDecimal salePrice,

        @Schema(description = "Stock actual", example = "10")
        Integer currentStock,

        @Schema(description = "Margen de ganancia")
        BigDecimal margin,

        @Schema(description = "Porcentaje de margen", example = "25.00")
        BigDecimal marginPercentage,

        @Schema(description = "Volumen calculado", example = "2218.75")
        BigDecimal volume,

        @JsonProperty("subcategory")
        @Schema(description = "Subcategoría (solo campos esenciales)")
        SubcategoryEssentialsDTO subcategory,

        @JsonProperty("suppliers")
        @Schema(description = "Lista de proveedores asociados al producto")
        List<SupplierAssociationResponseDTO> suppliers,

        @Schema(description = "Proveedor principal (para conveniencia)")
        String primarySupplierName,

        @Schema(description = "SKU del proveedor principal (para conveniencia)")
        String primarySupplierSku,

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

        @Schema(description = "Indica si hay stock disponible", example = "true")
        Boolean hasStock,

        @Schema(description = "Indica si el stock es bajo", example = "false")
        Boolean lowStock
) {}