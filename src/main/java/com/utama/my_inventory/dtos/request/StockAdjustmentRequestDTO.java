package com.utama.my_inventory.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "DTO para ajuste de inventario")
public record StockAdjustmentRequestDTO(

        @NotNull(message = "ID del producto es obligatorio")
        @Schema(description = "ID del producto", example = "1")
        Long productId,

        @Schema(description = "Nuevo stock objetivo", example = "50")
        Integer newStock,

        @Min(value = 1, message = "Cantidad ajuste debe ser al menos 1")
        @Max(value = 10000, message = "Cantidad ajuste no puede exceder 10000")
        @Schema(description = "Cantidad de unidades para ajuste", example = "10")
        Integer adjustmentQuantity,

        @NotNull(message = "Tipo de ajuste es obligatorio (ENTRADA o SALIDA)")
        @Schema(description = "Tipo de ajuste", example = "ENTRADA")
        String adjustmentType,

        @NotBlank(message = "Motivo es obligatorio para ajustes")
        @Size(max = 200, message = "Motivo no puede exceder 200 caracteres")
        @Schema(description = "Motivo del ajuste", example = "Ajuste por inventario físico")
        String reason
) {}