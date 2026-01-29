package com.utama.my_inventory.dtos.request.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "DTO para salida de inventario")
public record StockExitRequestDTO(

        @NotNull(message = "ID del producto es obligatorio")
        @Schema(description = "ID del producto", example = "1")
        Long productId,

        @NotNull(message = "Cantidad es obligatoria")
        @Min(value = 1, message = "Cantidad debe ser al menos 1")
        @Max(value = 10000, message = "Cantidad no puede exceder 10000")
        Integer quantity,

        @NotBlank(message = "Motivo es obligatorio")
        @Size(max = 200, message = "Motivo no puede exceder 200 caracteres")
        @Schema(description = "Razón por la cual se saca el stock", example = "Ajuste de stock")
        String reason,

        @NotBlank(message = "Usuario es obligatorio")
        @Size(max = 100, message = "Usuario no puede exceder 100 caracteres")
        @Schema(description = "Nombre de la persona que agrego stock", example = "Jorge Diaz")
        String user
) {}

