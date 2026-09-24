// com/utama/my_inventory/services/PackagingCostService.java
package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.response.product.PackagingBreakdownDTO;
import com.utama.my_inventory.entities.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PackagingCostService {
    PackagingCostResult calculatePackagingCost(Product product);

    record PackagingCostResult(BigDecimal totalCost, List<PackagingBreakdownDTO> breakdown) {}

    Map<Long, PackagingCostResult> calculatePackagingCostBatch(List<Product> products);
}