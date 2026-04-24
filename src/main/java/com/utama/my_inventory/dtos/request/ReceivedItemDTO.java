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
public class ReceivedItemDTO {

    @NotBlank(message = "SKU del proveedor es obligatorio")
    @Size(max = 50, message = "SKU no puede exceder 50 caracteres")
    private String supplierSku;  // ← Cambiado de 'sku' a 'supplierSku'

    private String productName;  // ← Agregado para productos nuevos

    @NotNull(message = "Cantidad recibida es obligatoria")
    @Min(value = 1, message = "Cantidad recibida debe ser al menos 1")
    @Max(value = 99999, message = "Cantidad no puede exceder 99999")
    private Integer additionalQuantity;

    @DecimalMin(value = "0.01", message = "Precio unitario debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "Precio debe tener máximo 10 enteros y 2 decimales")
    private BigDecimal unitPrice;
}