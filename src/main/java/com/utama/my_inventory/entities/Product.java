package com.utama.my_inventory.entities;


import com.utama.my_inventory.entities.enums.MovementType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_sku", columnNames = "sku"),
        indexes = {
                @Index(name = "idx_product_active", columnList = "active"),
                @Index(name = "idx_product_sku", columnList = "sku"),
                @Index(name = "idx_product_subcategory", columnList = "subcategory_id"),
                @Index(name = "idx_product_supplier", columnList = "supplier_id"),
                @Index(name = "idx_product_stock", columnList = "current_stock")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Pattern(regexp = "^[A-Z0-9\\-._]{3,50}$",
            message = "SKU debe contener solo letras mayúsculas, números, guiones, puntos y guiones bajos")
    @Column(length = 30, unique = true, nullable = true)  // nullable = true permite null temporal
    private String sku;

    @NotBlank(message = "Nombre es obligatorio")
    @Size(min = 2, max = 200, message = "Nombre debe tener entre 2 y 200 caracteres")
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 50000, message = "Descripción no puede exceder 50000 caracteres")
    @Column(columnDefinition = "TEXT")
    private String description;

    @DecimalMin(value = "0.00", inclusive = true, message = "Precio costo no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "Precio costo debe tener máximo 10 enteros y 2 decimales")
    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @DecimalMin(value = "0.00", inclusive = true, message = "Precio venta no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "Precio venta debe tener máximo 10 enteros y 2 decimales")
    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Min(value = 0, message = "Stock no puede ser negativo")
    @Column(name = "current_stock", nullable = false)
    @Builder.Default
    private Integer currentStock = 0;

    @NotNull(message = "Subcategoría es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subcategory_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_subcategory"))
    private Subcategory subcategory;

    @DecimalMin(value = "0.001", message = "Peso debe ser mayor a 0")
    @Digits(integer = 5, fraction = 3, message = "Peso debe tener máximo 5 enteros y 3 decimales")
    @Column(precision = 8, scale = 3)
    private BigDecimal weight; // kg

    @DecimalMin(value = "0.01", message = "Largo debe ser mayor a 0")
    @Digits(integer = 5, fraction = 2, message = "Largo debe tener máximo 5 enteros y 2 decimales")
    @Column(precision = 7, scale = 2)
    private BigDecimal length; // cm

    @DecimalMin(value = "0.01", message = "Ancho debe ser mayor a 0")
    @Digits(integer = 5, fraction = 2, message = "Ancho debe tener máximo 5 enteros y 2 decimales")
    @Column(precision = 7, scale = 2)
    private BigDecimal width; // cm

    @DecimalMin(value = "0.01", message = "Alto debe ser mayor a 0")
    @Digits(integer = 5, fraction = 2, message = "Alto debe tener máximo 5 enteros y 2 decimales")
    @Column(precision = 7, scale = 2)
    private BigDecimal height; // cm

    @Size(max = 20, message = "Unidad de medida no puede exceder 20 caracteres")
    @Column(name = "measure_unit", length = 20)
    @Builder.Default
    private String measureUnit = "cm";

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_purchase_at")
    private LocalDateTime lastPurchaseAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PurchaseOrderItem> purchaseOrderItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryMovement> inventoryMovements = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MultimediaFile> multimediaFiles = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductSupplier> productSuppliers = new ArrayList<>();

    public Supplier getPrimarySupplier() {
        return productSuppliers.stream()
                .filter(ProductSupplier::getIsPrimary)
                .findFirst()
                .map(ProductSupplier::getSupplier)
                .orElse(null);
    }

    public String getPrimarySupplierSku() {
        return productSuppliers.stream()
                .filter(ProductSupplier::getIsPrimary)
                .findFirst()
                .map(ProductSupplier::getSupplierSku)
                .orElse(null);
    }
    // Business methods with modern Java
    public BigDecimal calculateMargin() {
        if (costPrice == null || salePrice == null) {
            return BigDecimal.ZERO;
        }
        return salePrice.subtract(costPrice);
    }

    public BigDecimal calculateMarginPercentage() {
        if (costPrice == null || costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal margin = calculateMargin();
        return margin.divide(costPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal calculateVolume() {
        if (length == null || width == null || height == null) {
            return BigDecimal.ZERO;
        }
        return length.multiply(width).multiply(height);
    }

    public boolean hasStock() {
        return currentStock > 0;
    }

    public boolean isLowStock(int threshold) {
        return currentStock < threshold;
    }

    public void addStock(int quantity, String reason, String user) {
        if (quantity <= 0) return;

        InventoryMovement movement = InventoryMovement.builder()
                .product(this)
                .quantity(quantity)
                .movementType(MovementType.ENTRADA)
                .reason(reason)
                .registeredBy(user)
                .unitCost(costPrice)
                .build();

        inventoryMovements.add(movement);
        currentStock += quantity;
        lastPurchaseAt = LocalDateTime.now();
    }

    public void removeStock(int quantity, String reason, String user) {
        if (quantity <= 0 || quantity > currentStock) return;

        InventoryMovement movement = InventoryMovement.builder()
                .product(this)
                .quantity(quantity)
                .movementType(MovementType.SALIDA)
                .reason(reason)
                .registeredBy(user)
                .build();

        inventoryMovements.add(movement);
        currentStock -= quantity;
    }

    public void addMultimediaFile(MultimediaFile file) {
        multimediaFiles.add(file);
        file.setProduct(this);
    }

    @AssertTrue(message = "Precio de venta debe ser mayor o igual al precio de costo")
    private boolean isValidPricing() {
        if (salePrice == null || costPrice == null) return true;
        return salePrice.compareTo(costPrice) >= 0;
    }

    @PreRemove
    private void preRemove() {
        this.active = false;
    }
}