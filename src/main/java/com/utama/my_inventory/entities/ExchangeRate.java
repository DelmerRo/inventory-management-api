package com.utama.my_inventory.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "exchange_rates", indexes = {@Index(name = "idx_exchange_date", columnList = "rate_date")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_date", nullable = false, unique = true)
    private LocalDate rateDate;

    @Column(name = "usd_to_ars", nullable = false, precision = 10, scale = 2)
    private BigDecimal usdToArs;
}