package com.utama.my_inventory.dtos.response.inventory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.utama.my_inventory.dtos.response.ProductSummaryResponseDTO;
import com.utama.my_inventory.entities.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta con información del movimiento de inventario")
public record InventoryMovementResponseDTO(

        @Schema(description = "ID del movimiento", example = "1")
        Long id,

        @JsonProperty("product")
        @Schema(description = "Producto asociado")
        ProductSummaryResponseDTO product,

        @Schema(description = "Cantidad de unidades", example = "10")
        Integer quantity,

        @Schema(description = "Tipo de movimiento", example = "ENTRADA")
        MovementType movementType,

        @Schema(description = "Motivo del movimiento", example = "Compra de proveedor")
        String reason,

        @Schema(description = "Fecha del movimiento")
        LocalDateTime movementDate,

        @Schema(description = "Usuario que registró el movimiento", example = "admin")
        String registeredBy,

        @Schema(description = "Costo unitario", example = "25.50")
        BigDecimal unitCost,

        @Schema(description = "Valor total (cantidad * costo unitario)", example = "255.00")
        BigDecimal totalValue,

        @Schema(description = "Stock después del movimiento", example = "50")
        Integer stockAfterMovement,

        @Schema(description = "Stock antes del movimiento", example = "40")
        Integer stockBeforeMovement,

        @Schema(description = "Diferencia de stock", example = "10")
        Integer stockDifference
) {}