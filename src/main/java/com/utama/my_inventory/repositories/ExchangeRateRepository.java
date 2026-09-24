package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    // Trae la cotización exacta de una fecha, o la última cotización registrada hacia atrás
    Optional<ExchangeRate> findFirstByRateDateLessThanEqualOrderByRateDateDesc(LocalDate date);
}