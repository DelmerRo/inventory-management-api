// com/utama/my_inventory/dtos/response/product/PackagingBreakdownDTO.java
package com.utama.my_inventory.dtos.response.product;

import java.math.BigDecimal;

public record PackagingBreakdownDTO(
        Long supplyId,
        String supplyName,
        String unitMeasure,
        BigDecimal calculatedQuantity,
        BigDecimal currentUnitCost, // El CPP
        BigDecimal totalCost
) {}