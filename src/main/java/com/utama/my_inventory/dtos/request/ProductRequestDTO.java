package com.utama.my_inventory.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "DTO para crear o actualizar un producto")
public record ProductRequestDTO(

        @NotBlank(message = "Nombre es obligatorio")
        @Size(min = 2, max = 200, message = "Nombre debe tener entre 2 y 200 caracteres")
        @Schema(description = "Nombre del producto", example = "Laptop HP EliteBook")
        String name,

        @Size(max = 50000, message = "Descripción no puede exceder 2000 caracteres")
        @Schema(description = "Descripción detallada", example = "Laptop empresarial con 16GB RAM, 512GB SSD")
        String description,

        @Size(max = 50, message = "SKU de proveedor no puede exceder 50 caracteres")
        @Schema(description = "SKU del proveedor para este producto", example = "HP-ELITE-001")
        String supplierSku,

        @DecimalMin(value = "0.00", inclusive = true, message = "Precio costo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Precio costo debe tener máximo 10 enteros y 2 decimales")
        @Schema(description = "Precio de costo", example = "1200.50")
        BigDecimal costPrice,

        @DecimalMin(value = "0.00", inclusive = true, message = "Precio venta no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Precio venta debe tener máximo 10 enteros y 2 decimales")
        @Schema(description = "Precio de venta", example = "1500.00")
        BigDecimal salePrice,

        @Min(value = 0, message = "Stock no puede ser negativo")
        @Schema(description = "Stock inicial", example = "10")
        Integer currentStock,

        @NotNull(message = "Subcategoría es obligatoria")
        @Schema(description = "ID de la subcategoría", example = "1")
        Long subcategoryId,

        @NotEmpty(message = "Debe tener al menos un proveedor")
        @Schema(description = "Lista de proveedores asociados al producto")
        List<SupplierAssociationDTO> suppliers,

        @DecimalMin(value = "0.001", message = "Peso debe ser mayor a 0")
        @Digits(integer = 5, fraction = 3, message = "Peso debe tener máximo 5 enteros y 3 decimales")
        @Schema(description = "Peso en kg", example = "1.500")
        BigDecimal weight,

        @DecimalMin(value = "0.01", message = "Largo debe ser mayor a 0")
        @Digits(integer = 5, fraction = 2, message = "Largo debe tener máximo 5 enteros y 2 decimales")
        @Schema(description = "Largo en cm", example = "35.50")
        BigDecimal length,

        @DecimalMin(value = "0.01", message = "Ancho debe ser mayor a 0")
        @Digits(integer = 5, fraction = 2, message = "Ancho debe tener máximo 5 enteros y 2 decimales")
        @Schema(description = "Ancho en cm", example = "25.00")
        BigDecimal width,

        @DecimalMin(value = "0.01", message = "Alto debe ser mayor a 0")
        @Digits(integer = 5, fraction = 2, message = "Alto debe tener máximo 5 enteros y 2 decimales")
        @Schema(description = "Alto en cm", example = "2.50")
        BigDecimal height,

        @Size(max = 20, message = "Unidad de medida no puede exceder 20 caracteres")
        @Schema(description = "Unidad de medida", example = "cm")
        String measureUnit
) {}