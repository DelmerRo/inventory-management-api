// com/utama/my_inventory/services/CostCalculationService.java
package com.utama.my_inventory.services;

import com.utama.my_inventory.entities.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CostCalculationService {
    BigDecimal calculateCPP(Product product);
    Map<Long, BigDecimal> calculateCPPBatch(List<Long> productIds);
}