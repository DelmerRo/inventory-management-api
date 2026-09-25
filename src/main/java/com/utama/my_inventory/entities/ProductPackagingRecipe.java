// com/utama/my_inventory/entities/ProductPackagingRecipe.java
package com.utama.my_inventory.entities;

import com.utama.my_inventory.entities.enums.PackagingCalculationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "product_packaging_recipes",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_supply", columnNames = {"product_id", "supply_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPackagingRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_id", nullable = false)
    private Supply supply; // El insumo que provee el CPP actual

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false)
    private PackagingCalculationType calculationType;

    // Un factor multiplicador (Ej: si usa 1.5 metros de cinta por cada CM del perímetro)
    @Column(precision = 8, scale = 4, nullable = false)
    private BigDecimal multiplier;
}