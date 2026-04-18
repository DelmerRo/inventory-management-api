package com.utama.my_inventory.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_suppliers",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_supplier", columnNames = {"product_id", "supplier_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Size(max = 50, message = "SKU del proveedor no puede exceder 50 caracteres")
    @Column(name = "supplier_sku", length = 50)
    private String supplierSku;

    @Builder.Default
    private Boolean isPrimary = false;  // Marcar si es el proveedor principal

    @Size(max = 255, message = "Nota no puede exceder 255 caracteres")
    private String notes;

    @CreationTimestamp
    @Column(name = "associated_at", updatable = false)
    private LocalDateTime associatedAt;
}