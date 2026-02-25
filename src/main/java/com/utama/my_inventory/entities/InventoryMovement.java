package com.utama.my_inventory.entities;

import com.utama.my_inventory.entities.enums.MovementType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movements",
        indexes = {
                @Index(name = "idx_movement_product", columnList = "product_id"),
                @Index(name = "idx_movement_date", columnList = "movement_date"),
                @Index(name = "idx_movement_type", columnList = "movement_type"),
                @Index(name = "idx_movement_user", columnList = "registered_by")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Producto es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movement_product"))
    private Product product;

    @Min(value = 1, message = "Cantidad debe ser al menos 1")
    @Max(value = 10000, message = "Cantidad no puede exceder 10000")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Tipo de movimiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Size(max = 200, message = "Motivo no puede exceder 200 caracteres")
    @Column(length = 200)
    private String reason;

    @CreationTimestamp
    @Column(name = "movement_date", nullable = false, updatable = false)
    private LocalDateTime movementDate;

    @NotBlank(message = "Usuario registrador es obligatorio")
    @Size(min = 3, max = 100, message = "Usuario debe tener entre 3 y 100 caracteres")
    @Column(name = "registered_by", nullable = false, length = 100)
    private String registeredBy;

    @DecimalMin(value = "0.00", inclusive = true, message = "Costo unitario no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "Costo unitario debe tener máximo 10 enteros y 2 decimales")
    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    public BigDecimal getTotalValue() {
        if (unitCost == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    @AssertTrue(message = "Movimientos de ajuste requieren un motivo")
    private boolean isValidAdjustment() {
        if (movementType != MovementType.AJUSTE) return true;
        return reason != null && !reason.isBlank();
    }

    @PrePersist
    private void prePersist() {
        if (movementType == MovementType.ENTRADA) {
            product.setLastPurchaseAt(movementDate);
        }
    }
}
