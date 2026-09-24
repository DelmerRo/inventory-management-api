package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.response.dashboard.*;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.Supply;
import com.utama.my_inventory.repositories.InventoryMovementRepository;
import com.utama.my_inventory.repositories.ProductRepository;
import com.utama.my_inventory.repositories.SupplyRepository;
import com.utama.my_inventory.services.api.CostCalculationService;
import com.utama.my_inventory.services.api.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl {

    private final ProductRepository productRepository;
    private final SupplyRepository supplyRepository;
    private final InventoryMovementRepository movementRepository;
    private final CostCalculationService costCalculationService;
    private final ExchangeRateService exchangeRateService;

    public DashboardResponseDTO getConsolidatedDashboard(BigDecimal expectedMarginPercentage) {
        log.info("Generando métricas consolidadas del Dashboard...");

        LocalDate today = LocalDate.now();
        BigDecimal currentUsdRate = exchangeRateService.getUsdRateForDate(today);

        // 1. CAPITAL EN INSUMOS
        List<Supply> activeSupplies = supplyRepository.findByActiveTrueOrderByNameAsc();
        BigDecimal totalSuppliesArs = activeSupplies.stream()
                .filter(s -> s.getCurrentStock() > 0)
                .map(s -> s.getUnitCost().multiply(BigDecimal.valueOf(s.getCurrentStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Buscar tarifa de empaque (Busca dinámicamente un insumo tipo cartón/caja/burbuja)
        BigDecimal packagingMaterialCost = activeSupplies.stream()
                .filter(s -> s.getName().toLowerCase().matches(".*(carton|caja|burbuja|empaque).*"))
                .findFirst()
                .map(Supply::getUnitCost)
                .orElse(new BigDecimal("0.05")); // Costo por defecto si no lo tienes cargado aún

        // 2. CAPITAL EN PRODUCTOS Y RENTABILIDAD
        // ✅ SOLUCIÓN AQUÍ: Usamos el método exacto que existe en tu ProductRepository
        List<Product> activeProducts = productRepository.findByActiveTrueOrderByNameAsc();

        BigDecimal totalProductsArs = BigDecimal.ZERO;
        BigDecimal totalExpectedRevenue = BigDecimal.ZERO;
        BigDecimal sumCpps = BigDecimal.ZERO;
        BigDecimal sumPackagingCosts = BigDecimal.ZERO;
        int productsWithStockCount = 0;

        for (Product p : activeProducts) {
            int stock = p.getCurrentStock();
            if (stock > 0) {
                BigDecimal cpp = costCalculationService.calculateCPP(p);
                BigDecimal packagingCost = costCalculationService.calculatePackagingCost(p, packagingMaterialCost);
                BigDecimal totalUnitCost = cpp.add(packagingCost);

                totalProductsArs = totalProductsArs.add(cpp.multiply(BigDecimal.valueOf(stock)));

                // Calculo de Ganancia Proyectada: Costo Total * (1 + Margen Promedio)
                BigDecimal multiplier = BigDecimal.ONE.add(expectedMarginPercentage.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                BigDecimal expectedSalePrice = totalUnitCost.multiply(multiplier);
                totalExpectedRevenue = totalExpectedRevenue.add(expectedSalePrice.multiply(BigDecimal.valueOf(stock)));

                sumCpps = sumCpps.add(cpp);
                sumPackagingCosts = sumPackagingCosts.add(packagingCost);
                productsWithStockCount++;
            }
        }

        BigDecimal totalCombinedArs = totalProductsArs.add(totalSuppliesArs);
        BigDecimal totalCombinedUsd = exchangeRateService.convertToUsd(totalCombinedArs, today);

        // 3. PREDICCIONES LINEALES
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int salesLast30Days = movementRepository.sumQuantityByOutcomesSince(thirtyDaysAgo).orElse(0);
        int totalCurrentProductStock = activeProducts.stream().mapToInt(Product::getCurrentStock).sum();

        // Días de stock = (Stock actual / (Ventas mensuales / 30 días))
        int estimatedStockOutDays = salesLast30Days > 0
                ? (int) (totalCurrentProductStock / (salesLast30Days / 30.0))
                : 999;

        // 4. CONSTRUCCIÓN DE RESPUESTA
        ImmobilizedCapitalDTO capitalDTO = new ImmobilizedCapitalDTO(
                totalProductsArs, totalSuppliesArs, totalCombinedArs, totalCombinedUsd, currentUsdRate);

        ProfitabilityDTO profitabilityDTO = new ProfitabilityDTO(
                productsWithStockCount > 0 ? sumCpps.divide(BigDecimal.valueOf(productsWithStockCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                productsWithStockCount > 0 ? sumPackagingCosts.divide(BigDecimal.valueOf(productsWithStockCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                totalExpectedRevenue,
                totalExpectedRevenue.subtract(totalProductsArs).subtract(totalSuppliesArs) // Neto
        );

        PredictionDTO predictionDTO = new PredictionDTO(
                salesLast30Days, estimatedStockOutDays, estimatedStockOutDays < 15);

        return new DashboardResponseDTO(capitalDTO, profitabilityDTO, predictionDTO);
    }
}