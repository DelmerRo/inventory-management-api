// com/utama/my_inventory/services/impl/ExchangeRateServiceImpl.java
package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.entities.ExchangeRate;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.repositories.ExchangeRateRepository;
import com.utama.my_inventory.services.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public BigDecimal getUsdRateForDate(LocalDate date) {
        try {
            return exchangeRateRepository.findFirstByRateDateLessThanEqualOrderByRateDateDesc(date)
                    .map(ExchangeRate::getUsdToArs)
                    .orElseGet(() -> {
                        log.warn("No se encontró tasa de cambio para la fecha {}. Usando fallback de seguridad.", date);
                        return BigDecimal.valueOf(1000.0);
                    });
        } catch (Exception e) {
            log.error("Error al consultar la tasa de cambio en la base de datos: {}", e.getMessage());
            throw new BusinessException("No se pudo obtener la tasa de cambio actual.");
        }
    }

    @Override
    public BigDecimal convertToUsd(BigDecimal amountArs, LocalDate date) {
        if (amountArs == null || amountArs.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal rate = getUsdRateForDate(date);
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("La tasa de cambio no puede ser cero para realizar conversiones.");
        }
        return amountArs.divide(rate, 2, RoundingMode.HALF_UP);
    }
}