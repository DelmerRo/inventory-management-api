// com/utama/my_inventory/services/impl/DashboardServiceImpl.java
package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.response.dashboard.*;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.Supply;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.repositories.InventoryMovementRepository;
import com.utama.my_inventory.repositories.ProductRepository;
import com.utama.my_inventory.repositories.SupplyRepository;
import com.utama.my_inventory.services.CostCalculationService;
import com.utama.my_inventory.services.ExchangeRateService;
import com.utama.my_inventory.services.PackagingCostService; // ✅ Usamos la nueva interfaz
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final PackagingCostService packagingCostService;

    public DashboardResponseDTO getConsolidatedDashboard(BigDecimal expectedMarginPercentage) {
        log.info("Generando métricas consolidadas del Dashboard (Modo Batch)...");

        try {
            LocalDate today = LocalDate.now();
            BigDecimal currentUsdRate = exchangeRateService.getUsdRateForDate(today);

            // 1. CAPITAL EN INSUMOS
            List<Supply> activeSupplies = supplyRepository.findByActiveTrueOrderByNameAsc();
            BigDecimal totalSuppliesArs = activeSupplies.stream()
                    .filter(s -> s.getCurrentStock() > 0)
                    .map(s -> s.getUnitCost().multiply(BigDecimal.valueOf(s.getCurrentStock())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 2. OBTENER PRODUCTOS
            List<Product> activeProducts = productRepository.findByActiveTrueOrderByNameAsc();
            List<Long> productIds = activeProducts.stream().map(Product::getId).toList();

            // ==========================================
            // 🔥 LA MAGIA DEL BATCH FETCHING 🔥
            // Hacemos 2 consultas grandes en vez de miles pequeñas
            // ==========================================
            Map<Long, BigDecimal> cppMap = costCalculationService.calculateCPPBatch(productIds);
            Map<Long, PackagingCostService.PackagingCostResult> packagingMap = packagingCostService.calculatePackagingCostBatch(activeProducts);

            BigDecimal totalProductsArs = BigDecimal.ZERO;
            BigDecimal totalExpectedRevenue = BigDecimal.ZERO;
            BigDecimal sumCpps = BigDecimal.ZERO;
            BigDecimal sumPackagingCosts = BigDecimal.ZERO;
            int productsWithStockCount = 0;

            // 3. RECORRER PRODUCTOS (Cero consultas SQL aquí dentro)
            for (Product p : activeProducts) {
                int stock = p.getCurrentStock();
                if (stock > 0) {
                    // Leer de los Mapas en RAM (Tiempo de respuesta: ~0.001 ms)
                    BigDecimal cpp = cppMap.getOrDefault(p.getId(), p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO);
                    PackagingCostService.PackagingCostResult packaging = packagingMap.get(p.getId());
                    BigDecimal packagingCost = packaging != null ? packaging.totalCost() : BigDecimal.ZERO;

                    BigDecimal totalUnitCost = cpp.add(packagingCost);
                    totalProductsArs = totalProductsArs.add(cpp.multiply(BigDecimal.valueOf(stock)));

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

            // 4. PREDICCIONES
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            int salesLast30Days = movementRepository.sumQuantityByOutcomesSince(thirtyDaysAgo).orElse(0);
            int totalCurrentProductStock = activeProducts.stream().mapToInt(Product::getCurrentStock).sum();

            int estimatedStockOutDays = salesLast30Days > 0
                    ? (int) (totalCurrentProductStock / (salesLast30Days / 30.0))
                    : 999;

            // 5. RESPUESTA
            ImmobilizedCapitalDTO capitalDTO = new ImmobilizedCapitalDTO(
                    totalProductsArs, totalSuppliesArs, totalCombinedArs, totalCombinedUsd, currentUsdRate);

            ProfitabilityDTO profitabilityDTO = new ProfitabilityDTO(
                    productsWithStockCount > 0 ? sumCpps.divide(BigDecimal.valueOf(productsWithStockCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                    productsWithStockCount > 0 ? sumPackagingCosts.divide(BigDecimal.valueOf(productsWithStockCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                    totalExpectedRevenue,
                    totalExpectedRevenue.subtract(totalProductsArs).subtract(totalSuppliesArs)
            );

            PredictionDTO predictionDTO = new PredictionDTO(
                    salesLast30Days, estimatedStockOutDays, estimatedStockOutDays < 15);

            return new DashboardResponseDTO(capitalDTO, profitabilityDTO, predictionDTO);

        } catch (Exception e) {
            log.error("Fallo crítico en la generación del Dashboard: {}", e.getMessage(), e);
            throw new BusinessException("No fue posible generar el Dashboard. Contacte a soporte.");
        }
    }
}