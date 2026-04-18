package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // ========== BÁSICOS ==========
    Optional<Product> findByIdAndActiveTrue(Long id);

    Optional<Product> findBySkuAndActiveTrue(String sku);

    List<Product> findByActiveTrueOrderByNameAsc();

    boolean existsBySkuAndActiveTrue(String sku);

    // ========== POR RELACIONES ==========
    List<Product> findBySubcategoryIdAndActiveTrue(Long subcategoryId);

    // Buscar productos por ID de proveedor a través de ProductSupplier
    @Query("SELECT DISTINCT p FROM Product p JOIN p.productSuppliers ps WHERE ps.supplier.id = :supplierId AND p.active = true")
    List<Product> findProductsBySupplierId(@Param("supplierId") Long supplierId);

    // ========== STOCK ==========
    @Query("SELECT p FROM Product p WHERE p.currentStock < :threshold AND p.active = true")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    // ========== BÚSQUEDA CON FILTROS ==========
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.productSuppliers ps " +
            "WHERE p.active = true " +
            "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:sku IS NULL OR p.sku LIKE CONCAT('%', :sku, '%')) " +
            "AND (:minPrice IS NULL OR p.salePrice >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.salePrice <= :maxPrice) " +
            "AND (:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId) " +
            "AND (:supplierId IS NULL OR ps.supplier.id = :supplierId)")
    List<Product> searchProducts(@Param("name") String name,
                                 @Param("sku") String sku,
                                 @Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 @Param("subcategoryId") Long subcategoryId,
                                 @Param("supplierId") Long supplierId);

    // ========== ESTADÍSTICAS ==========
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    Long countActiveProducts();

    @Query("SELECT COALESCE(SUM(p.currentStock), 0) FROM Product p WHERE p.active = true")
    Long getTotalStock();

    @Query("SELECT COALESCE(SUM(p.currentStock * p.costPrice), 0) FROM Product p WHERE p.active = true")
    BigDecimal getTotalInventoryValue();

    Optional<Product> findBySku(String sku);
}