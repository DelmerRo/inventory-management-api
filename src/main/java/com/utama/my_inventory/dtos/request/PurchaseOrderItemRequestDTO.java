package com.utama.my_inventory.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Item de producto dentro de un pedido de compra")
public class PurchaseOrderItemRequestDTO {

    @NotBlank(message = "SKU del proveedor es obligatorio")
    @Size(max = 50, message = "SKU no puede exceder 50 caracteres")
    @Schema(description = "SKU del proveedor", example = "PROV-123")
    private String supplierSku;

    @Schema(description = "Nombre del producto (opcional)", example = "Alfombra Gris 120x160cm")
    private String productName;

    @Min(value = 1, message = "Cantidad debe ser al menos 1")
    @Max(value = 99999, message = "Cantidad no puede exceder 99999")
    @Schema(description = "Cantidad de unidades", example = "10")
    private Integer quantity;

    @NotNull(message = "Precio unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "Precio unitario debe ser mayor a 0")
    @Schema(description = "Precio unitario", example = "1250.50")
    private BigDecimal unitPrice;
}