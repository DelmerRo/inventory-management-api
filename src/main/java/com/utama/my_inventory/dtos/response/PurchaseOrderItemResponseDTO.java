package com.utama.my_inventory.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemResponseDTO {

    private Long id;
    private String sku;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Integer quantityReceived;   // ✅ Aquí tiene sentido
    private BigDecimal subtotal;
    private Integer pendingQuantity;
    private Boolean fullyReceived;
}