package com.utama.my_inventory.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_order_number", columnNames = "order_number"),
        indexes = {
                @Index(name = "idx_order_supplier", columnList = "supplier_id"),
                @Index(name = "idx_order_date", columnList = "order_date"),
                @Index(name = "idx_order_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Número de pedido es obligatorio")
    @Size(min = 3, max = 50, message = "Número de pedido debe tener entre 3 y 50 caracteres")
    @Column(name = "order_number", nullable = false, length = 50, unique = true)
    private String orderNumber;

    @NotNull(message = "Proveedor es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_supplier"))
    private Supplier supplier;

    @NotNull(message = "Fecha del pedido es obligatoria")
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;

    @NotBlank(message = "Estado es obligatorio")
    @Size(max = 30, message = "Estado no puede exceder 30 caracteres")
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDIENTE"; // PENDIENTE, ENVIADO, PARCIAL, COMPLETADO, CANCELADO

    @Size(max = 500, message = "Observaciones no pueden exceder 500 caracteres")
    @Column(length = 500)
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Métodos de negocio
    public void addItem(PurchaseOrderItem item) {
        items.add(item);
        item.setPurchaseOrder(this);
    }

    public void removeItem(PurchaseOrderItem item) {
        items.remove(item);
        item.setPurchaseOrder(null);
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(PurchaseOrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isCompleted() {
        return "COMPLETADO".equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return "PENDIENTE".equalsIgnoreCase(status);
    }

    @PreUpdate
    private void preUpdate() {
        if ("COMPLETADO".equalsIgnoreCase(status) && deliveryDate == null) {
            deliveryDate = LocalDateTime.now();
        }
    }
}