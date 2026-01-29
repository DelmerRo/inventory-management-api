package com.utama.my_inventory.dtos.request.inventory;

import com.utama.my_inventory.entities.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "DTO para registrar movimiento de inventario")
public record InventoryMovementRequestDTO(

        @NotNull(message = "ID del producto es obligatorio")
        @Schema(description = "ID del producto", example = "1")
        Long productId,

        @Min(value = 1, message = "Cantidad debe ser al menos 1")
        @Max(value = 10000, message = "Cantidad no puede exceder 10000")
        @Schema(description = "Cantidad de unidades", example = "10")
        Integer quantity,

        @NotNull(message = "Tipo de movimiento es obligatorio")
        @Schema(description = "Tipo de movimiento", example = "ENTRADA")
        MovementType movementType,

        @Size(max = 200, message = "Motivo no puede exceder 200 caracteres")
        @Schema(description = "Motivo del movimiento", example = "Compra de proveedor")
        String reason,

        @Size(min = 3, max = 100, message = "Usuario debe tener entre 3 y 100 caracteres")
        @Schema(description = "Usuario que registra el movimiento", example = "admin")
        String registeredBy,

        @DecimalMin(value = "0.00", inclusive = true, message = "Costo unitario no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Costo unitario debe tener máximo 10 enteros y 2 decimales")
        @Schema(description = "Costo unitario (opcional)", example = "25.50")
        BigDecimal unitCost
) {}