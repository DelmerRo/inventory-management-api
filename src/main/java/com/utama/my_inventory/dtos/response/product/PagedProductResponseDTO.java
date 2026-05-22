package com.utama.my_inventory.dtos.response.product;

import lombok.Builder;

import java.util.List;

@Builder
public record PagedProductResponseDTO(
        List<ProductSummaryResponseDTO> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious,
        long totalProducts,
        long activeProducts,
        long inactiveProducts,
        long lowStockProducts,
        long outOfStockProducts
) {}