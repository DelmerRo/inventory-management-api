package com.utama.my_inventory.dtos.request.product;

import com.utama.my_inventory.entities.enums.PackagingCalculationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PackagingRecipeItemDTO(
        @NotNull(message = "El ID del insumo es obligatorio")
        Long supplyId,

        @NotNull(message = "El tipo de cálculo es obligatorio")
        PackagingCalculationType calculationType,

        @NotNull(message = "El multiplicador de consumo es obligatorio")
        @DecimalMin(value = "0.0001", message = "El multiplicador debe ser mayor a 0")
        BigDecimal multiplier
) {}