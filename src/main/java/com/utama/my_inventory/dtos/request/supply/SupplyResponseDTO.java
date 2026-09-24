package com.utama.my_inventory.dtos.request.supply;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SupplyResponseDTO(
        Long id,
        String name,
        String description,
        String unitMeasure,
        BigDecimal unitCost,
        Integer currentStock,
        Boolean active,
        LocalDateTime updatedAt
) {}