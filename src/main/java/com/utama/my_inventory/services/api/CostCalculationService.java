package com.utama.my_inventory.services.api;

import com.utama.my_inventory.entities.InventoryMovement;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.enums.MovementType;
import com.utama.my_inventory.repositories.InventoryMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CostCalculationService {

    private final InventoryMovementRepository movementRepository;

    /**
     * Calcula el CPP basado en los últimos ingresos para no depender de un solo precio estático.
     */
    public BigDecimal calculateCPP(Product product) {
        List<InventoryMovement> entries = movementRepository
                .findTop10ByProductAndMovementTypeOrderByMovementDateDesc(product, MovementType.ENTRADA);

        if (entries.isEmpty()) return BigDecimal.ZERO;

        BigDecimal totalCost = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (InventoryMovement entry : entries) {
            totalCost = totalCost.add(entry.getTotalValue());
            totalQuantity += entry.getQuantity();
        }

        return totalCost.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el costo de empaque basado en el área superficial (Caja) + 2cm de excedente por lado.
     * @param product El producto del cual se extraen las dimensiones.
     * @param costPerCm2 El costo del insumo por centímetro cuadrado.
     * @return Costo total del empaque.
     */
    public BigDecimal calculatePackagingCost(Product product, BigDecimal costPerCm2) {
        // 1. Prevención de nulos: si el producto no tiene medidas, el costo de empaque es 0.
        if (product.getWidth() == null || product.getHeight() == null || product.getLength() == null) {
            return BigDecimal.ZERO;
        }

        // 2. Regla de Negocio: 2cm de excedente por lado = +4cm en la dimensión total.
        // Convertimos el BigDecimal a double usando .doubleValue() para la matemática de área.
        double w = product.getWidth().doubleValue() + 4.0;
        double h = product.getHeight().doubleValue() + 4.0;
        double l = product.getLength().doubleValue() + 4.0;

        // 3. Fórmula Área Superficial de un prisma rectangular: 2 * (ancho*alto + alto*largo + ancho*largo)
        double surfaceAreaCm2 = 2 * ((w * h) + (h * l) + (w * l));

        // 4. Multiplicamos los cm2 totales por la tarifa del insumo (ej: cartón corrugado)
        return costPerCm2.multiply(BigDecimal.valueOf(surfaceAreaCm2));
    }
}