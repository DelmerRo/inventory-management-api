package com.utama.my_inventory.dtos.response.dashboard;

import java.math.BigDecimal;

public record ProfitabilityDTO(
        BigDecimal averageProductCost,
        BigDecimal averagePackagingCost,
        BigDecimal potentialRevenue,
        BigDecimal potentialNetProfit
) {}