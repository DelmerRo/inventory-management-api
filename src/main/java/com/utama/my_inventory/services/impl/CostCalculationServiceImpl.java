// com/utama/my_inventory/services/impl/CostCalculationServiceImpl.java
package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.entities.InventoryMovement;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.enums.MovementType;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.repositories.InventoryMovementRepository;
import com.utama.my_inventory.services.CostCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CostCalculationServiceImpl implements CostCalculationService {

    private final InventoryMovementRepository movementRepository;

    @Override
    public BigDecimal calculateCPP(Product product) {
        if (product == null || product.getId() == null) {
            throw new BusinessException("No se puede calcular el CPP de un producto nulo.");
        }

        try {
            List<InventoryMovement> entries = movementRepository
                    .findTop10ByProductAndMovementTypeOrderByMovementDateDesc(product, MovementType.ENTRADA);

            if (entries.isEmpty()) {
                return product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            }

            BigDecimal totalCost = BigDecimal.ZERO;
            int totalQuantity = 0;

            for (InventoryMovement entry : entries) {
                totalCost = totalCost.add(entry.getTotalValue());
                totalQuantity += entry.getQuantity();
            }

            if (totalQuantity == 0) return BigDecimal.ZERO;

            return totalCost.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("Error al calcular el CPP para el producto ID {}: {}", product.getId(), e.getMessage());
            throw new BusinessException("Error en el cálculo financiero del inventario.");
        }
    }

    @Override
    public Map<Long, BigDecimal> calculateCPPBatch(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return new HashMap<>();

        // 1. Traemos TODAS las entradas de todos los productos en una sola consulta
        List<InventoryMovement> allEntries = movementRepository
                .findByProductIdInAndMovementTypeOrderByMovementDateDesc(productIds, MovementType.ENTRADA);

        // 2. Agrupamos los movimientos por ID de producto en la memoria RAM
        Map<Long, List<InventoryMovement>> movementsByProduct = allEntries.stream()
                .collect(Collectors.groupingBy(m -> m.getProduct().getId()));

        Map<Long, BigDecimal> cppMap = new HashMap<>();

        // 3. Calculamos el CPP para cada producto limitando a las últimas 10 entradas
        for (Long productId : productIds) {
            List<InventoryMovement> entries = movementsByProduct.getOrDefault(productId, List.of());

            // Tomamos solo las top 10 (la lista ya viene ordenada por fecha desde la BD)
            List<InventoryMovement> top10Entries = entries.stream().limit(10).toList();

            if (top10Entries.isEmpty()) {
                cppMap.put(productId, BigDecimal.ZERO);
                continue;
            }

            BigDecimal totalCost = BigDecimal.ZERO;
            int totalQuantity = 0;

            for (InventoryMovement entry : top10Entries) {
                totalCost = totalCost.add(entry.getTotalValue());
                totalQuantity += entry.getQuantity();
            }

            BigDecimal cpp = totalQuantity == 0 ? BigDecimal.ZERO
                    : totalCost.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP);

            cppMap.put(productId, cpp);
        }

        return cppMap;
    }
}