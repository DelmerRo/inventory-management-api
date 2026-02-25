package com.utama.my_inventory.dtos.response.inventory;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para estadísticas de movimientos de inventario")
public record InventoryStatisticsResponseDTO(

        @Schema(description = "ID del producto", example = "1")
        Long productId,

        @Schema(description = "Total de entradas", example = "150")
        Integer totalEntries,

        @Schema(description = "Total de salidas", example = "75")
        Integer totalExits,

        @Schema(description = "Cantidad total de movimientos", example = "225")
        Long movementCount,

        @Schema(description = "Movimiento neto (entradas - salidas)", example = "75")
        Integer netMovement
) {}