package com.utama.my_inventory.dtos.request.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "DTO para salida de inventario")
public record StockExitRequestDTO(

        @NotNull(message = "ID del producto es obligatorio")
        @Schema(description = "ID del producto", example = "1")
        Long productId,

        @Min(value = 1, message = "Cantidad debe ser al menos 1")
        @Max(value = 10000, message = "Cantidad no puede exceder 10000")
        @Schema(description = "Cantidad de unidades a retirar", example = "5")
        Integer quantity,

        @NotBlank(message = "Motivo es obligatorio")
        @Size(max = 200, message = "Motivo no puede exceder 200 caracteres")
        @Schema(description = "Motivo de la salida", example = "Venta a cliente")
        String reason
) {}