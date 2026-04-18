package com.utama.my_inventory.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "suppliers",
        uniqueConstraints = @UniqueConstraint(name = "uk_supplier_name", columnNames = "name"),
        indexes = {
                @Index(name = "idx_supplier_active", columnList = "active"),
                @Index(name = "idx_supplier_email", columnList = "email")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 150, message = "El contacto no puede exceder 150 caracteres")
    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PurchaseOrder> purchaseOrders = new ArrayList<>();

    @Email(message = "Email inválido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    @Column(length = 100)
    private String email;

    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$", message = "Teléfono inválido")
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Column(length = 20)
    private String phone;

    @Size(max = 500, message = "La dirección no puede exceder 500 caracteres")
    @Column(length = 500)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "registered_at", updatable = false)
    private LocalDateTime registeredAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProductSupplier> productSuppliers = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public long countActiveProducts() {
        return productSuppliers.stream()
                .filter(ps -> ps.getProduct().getActive())
                .count();
    }

    @PreRemove
    private void preRemove() {
        this.active = false;
    }
}
