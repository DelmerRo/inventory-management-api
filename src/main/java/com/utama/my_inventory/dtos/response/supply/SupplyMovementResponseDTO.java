package com.utama.my_inventory.dtos.response.supply;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SupplyMovementResponseDTO(
        Long id,
        Long supplyId,
        String supplyName,
        Integer quantity,
        String movementType,
        String reason,
        BigDecimal unitCost,
        BigDecimal totalValue,
        LocalDateTime movementDate,
        String registeredBy
) {}