// com/utama/my_inventory/services/ExchangeRateService.java
package com.utama.my_inventory.services;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExchangeRateService {
    BigDecimal getUsdRateForDate(LocalDate date);
    BigDecimal convertToUsd(BigDecimal amountArs, LocalDate date);
}