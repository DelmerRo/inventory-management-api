package com.utama.my_inventory.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponseDTO {

    private Long id;
    private String orderNumber;
    private Long supplierId;
    private String supplierName;
    private LocalDateTime orderDate;
    private LocalDateTime expectedDeliveryDate;
    private String status;
    private String notes;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<PurchaseOrderItemResponseDTO> items = new ArrayList<>();
}