package com.utama.my_inventory.dtos.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemDTO {

    private Long id;

    @NotNull(message = "ID del producto es obligatorio")
    private Long productId;

    private String productSku;
    private String productName;

    @Min(value = 1, message = "Cantidad debe ser al menos 1")
    @Max(value = 99999, message = "Cantidad no puede exceder 99999")
    private Integer quantity;

    @NotNull(message = "Precio unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "Precio unitario debe ser mayor a 0")
    private BigDecimal unitPrice;

    @Builder.Default
    private Integer quantityReceived = 0;

    private BigDecimal subtotal;
    private Integer pendingQuantity;
    private Boolean fullyReceived;
}