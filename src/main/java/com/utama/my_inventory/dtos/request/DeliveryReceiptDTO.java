package com.utama.my_inventory.dtos.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryReceiptDTO {

    @NotNull(message = "ID del pedido es obligatorio")
    private Long purchaseOrderId;

    private LocalDateTime deliveryDate;

    @Size(max = 500)
    private String notes;

    @NotEmpty(message = "Debe ingresar al menos un producto recibido")
    private List<ReceivedItemDTO> receivedItems;
}