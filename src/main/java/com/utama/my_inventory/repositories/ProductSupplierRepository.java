package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.ProductSupplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, Long> {
    List<ProductSupplier> findByProductId(Long productId);
    List<ProductSupplier> findBySupplierId(Long supplierId);
    Optional<ProductSupplier> findByProductIdAndSupplierId(Long productId, Long supplierId);
    Optional<ProductSupplier> findByProductIdAndIsPrimaryTrue(Long productId);
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductSupplier ps WHERE ps.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}