package com.utama.my_inventory.services.api;

import com.utama.my_inventory.entities.ExchangeRate;
import com.utama.my_inventory.repositories.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public BigDecimal getUsdRateForDate(LocalDate date) {
        return exchangeRateRepository.findFirstByRateDateLessThanEqualOrderByRateDateDesc(date)
                .map(ExchangeRate::getUsdToArs)
                .orElse(BigDecimal.valueOf(1000.0)); // Fallback de seguridad si no hay registros
    }

    public BigDecimal convertToUsd(BigDecimal amountArs, LocalDate date) {
        BigDecimal rate = getUsdRateForDate(date);
        if (rate.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return amountArs.divide(rate, 2, RoundingMode.HALF_UP);
    }
}
