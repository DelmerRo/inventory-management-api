package com.utama.my_inventory.dtos.response.inventory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.utama.my_inventory.entities.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de operación de inventario")
public record InventoryOperationResponseDTO(

        @Schema(description = "ID del movimiento generado", example = "1")
        Long movementId,

        @Schema(description = "ID del producto", example = "1")
        Long productId,

        @Schema(description = "Nombre del producto", example = "Laptop HP EliteBook")
        String productName,

        @Schema(description = "SKU del producto", example = "PROD-001")
        String sku,

        @Schema(description = "Tipo de operación", example = "ENTRADA")
        MovementType operationType,

        @Schema(description = "Cantidad operada", example = "10")
        Integer quantity,

        @Schema(description = "Stock antes de la operación", example = "40")
        Integer stockBefore,

        @Schema(description = "Stock después de la operación", example = "50")
        Integer stockAfter,

        @Schema(description = "Costo unitario aplicado", example = "25.50")
        BigDecimal unitCost,

        @Schema(description = "Valor total", example = "255.00")
        BigDecimal totalValue,

        @Schema(description = "Motivo de la operación", example = "Compra de proveedor")
        String reason,

        @Schema(description = "Fecha de la operación")
        LocalDateTime operationDate,

        @Schema(description = "Mensaje de confirmación", example = "Entrada registrada exitosamente")
        String message
) {}