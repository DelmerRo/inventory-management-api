package com.utama.my_inventory.dtos.request.supply;

import java.math.BigDecimal;

public record SupplyRequestDTO(
        String name,
        String description,
        String unitMeasure,
        BigDecimal unitCost,
        Integer initialStock
) {}