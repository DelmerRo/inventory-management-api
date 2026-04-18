package com.utama.my_inventory.dtos.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedItemDTO {

    @NotBlank(message = "SKU del producto es obligatorio")
    private String sku; // SKU del producto para identificar

    @Min(value = 1, message = "Cantidad recibida debe ser al menos 1")
    @Max(value = 99999, message = "Cantidad no puede exceder 99999")
    private Integer receivedQuantity;
}