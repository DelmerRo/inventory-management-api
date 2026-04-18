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
                @Index(name = "idx_poi_product", columnList = "product_id")
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

    @NotNull(message = "Producto es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_poi_product"))
    private Product product;

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
        return quantity - quantityReceived;
    }

    public boolean isFullyReceived() {
        return quantityReceived != null && quantityReceived >= quantity;
    }

    public void receiveQuantity(int receivedQty) {
        if (receivedQty <= 0) return;
        int newReceived = (quantityReceived == null ? 0 : quantityReceived) + receivedQty;
        this.quantityReceived = Math.min(newReceived, quantity);
    }

    // SKU y nombre del producto (para consultas rápidas, no persistido)
    @Transient
    public String getProductSku() {
        return product != null ? product.getSku() : null;
    }

    @Transient
    public String getProductName() {
        return product != null ? product.getName() : null;
    }
}