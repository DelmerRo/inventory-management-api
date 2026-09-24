package com.utama.my_inventory.dtos.request.dashboard;

public record PredictionDTO(
        int salesLast30Days,
        int estimatedStockOutDays,
        boolean criticalStockAlert
) {}
