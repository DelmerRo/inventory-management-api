package com.utama.my_inventory.dtos.request.supply;

import java.math.BigDecimal;

public record SupplyMovementRequestDTO(
        Integer quantity,
        String movementType, // ENTRADA, SALIDA, AJUSTE
        String reason,
        BigDecimal unitCost // Opcional, si no se envía se toma el actual del insumo
) {}