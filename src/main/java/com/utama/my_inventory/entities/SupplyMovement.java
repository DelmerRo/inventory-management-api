package com.utama.my_inventory.entities;

import com.utama.my_inventory.entities.enums.MovementType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supply_movements",
        indexes = {
                @Index(name = "idx_supply_movement_supply", columnList = "supply_id"),
                @Index(name = "idx_supply_movement_date", columnList = "movement_date"),
                @Index(name = "idx_supply_movement_type", columnList = "movement_type")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El insumo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supply_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movement_supply"))
    private Supply supply;

    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Size(max = 200, message = "El motivo no puede exceder 200 caracteres")
    @Column(length = 200)
    private String reason;

    @CreationTimestamp
    @Column(name = "movement_date", nullable = false, updatable = false)
    private LocalDateTime movementDate;

    @NotBlank(message = "Usuario registrador es obligatorio")
    @Column(name = "registered_by", nullable = false, length = 100)
    private String registeredBy;

    @DecimalMin(value = "0.00", inclusive = true, message = "El costo unitario no puede ser negativo")
    @Column(name = "unit_cost", precision = 12, scale = 4)
    private BigDecimal unitCost;

    public BigDecimal getTotalValue() {
        if (unitCost == null || quantity == null) return BigDecimal.ZERO;
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    @AssertTrue(message = "Los movimientos de AJUSTE requieren un motivo")
    private boolean isValidAdjustment() {
        if (movementType != MovementType.AJUSTE) return true;
        return reason != null && !reason.isBlank();
    }
}