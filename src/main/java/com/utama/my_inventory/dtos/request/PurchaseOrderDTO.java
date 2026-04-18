package com.utama.my_inventory.dtos.request;

import jakarta.validation.constraints.*;
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
public class PurchaseOrderDTO {

    private Long id;

    @NotBlank(message = "Número de pedido es obligatorio")
    @Size(min = 3, max = 50, message = "Número de pedido debe tener entre 3 y 50 caracteres")
    private String orderNumber;

    @NotNull(message = "ID del proveedor es obligatorio")
    private Long supplierId;

    private String supplierName;

    @NotNull(message = "Fecha del pedido es obligatoria")
    private LocalDateTime orderDate;

    private LocalDateTime expectedDeliveryDate;

    @Builder.Default
    private String status = "PENDIENTE";

    @Size(max = 500)
    private String notes;

    @Builder.Default
    private List<PurchaseOrderItemDTO> items = new ArrayList<>();

    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}