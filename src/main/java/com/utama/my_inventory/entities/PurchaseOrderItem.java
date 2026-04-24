package com.utama.my_inventory.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items",
        indexes = {
                @Index(name = "idx_poi_order", columnList = "purchase_order_id"),
                @Index(name = "idx_poi_product", columnList = "product_id"),
                @Index(name = "idx_poi_sku", columnList = "sku")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Pedido es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_poi_order"))
    private PurchaseOrder purchaseOrder;

    // Producto puede ser NULL si el producto aún no existe en el sistema
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_poi_product"))
    private Product product;

    // SKU es obligatorio y se guarda siempre (incluso si el producto existe)
    @NotBlank(message = "SKU es obligatorio")
    @Size(max = 50, message = "SKU no puede exceder 50 caracteres")
    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    // Nombre del producto en el momento del pedido (para referencia histórica)
    @Size(max = 200, message = "Nombre no puede exceder 200 caracteres")
    @Column(name = "product_name", length = 200)
    private String productName;

    @Min(value = 1, message = "Cantidad debe ser al menos 1")
    @Max(value = 99999, message = "Cantidad no puede exceder 99999")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Precio unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "Precio unitario debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "Precio debe tener máximo 10 enteros y 2 decimales")
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Min(value = 0, message = "Cantidad recibida no puede ser negativa")
    @Column(name = "quantity_received")
    @Builder.Default
    private Integer quantityReceived = 0;

    // Métodos auxiliares
    public BigDecimal getSubtotal() {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Integer getPendingQuantity() {
        return quantity - (quantityReceived != null ? quantityReceived : 0);
    }

    public boolean isFullyReceived() {
        int received = quantityReceived != null ? quantityReceived : 0;
        return received >= quantity;
    }

    public void receiveQuantity(int receivedQty) {
        if (receivedQty <= 0) return;
        int currentReceived = quantityReceived != null ? quantityReceived : 0;
        this.quantityReceived = Math.min(currentReceived + receivedQty, quantity);
    }

    // Método para obtener el nombre del producto (prioriza el guardado, luego el de la entidad Product)
    public String getDisplayProductName() {
        if (productName != null && !productName.isBlank()) {
            return productName;
        }
        return product != null ? product.getName() : "Producto " + sku;
    }
}