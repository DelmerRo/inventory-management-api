package com.utama.my_inventory.dtos.response.dashboard;

import java.math.BigDecimal;

public record ImmobilizedCapitalDTO(
        BigDecimal productsValueArs,
        BigDecimal suppliesValueArs,
        BigDecimal totalCombinedArs,
        BigDecimal totalCombinedUsd,
        BigDecimal currentExchangeRate
) {}
