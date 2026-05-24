package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // ========== BÁSICOS ==========
    Optional<Product> findByIdAndActiveTrue(Long id);
    Optional<Product> findBySkuAndActiveTrue(String sku);
    boolean existsBySkuAndActiveTrue(String sku);
    List<Product> findByActiveTrueOrderByNameAsc();

    // ========== MÉTODO JPQL (ALTERNATIVA MÁS SEGURA) ==========
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.productSuppliers ps " +
            "LEFT JOIN p.subcategory sub " + // <-- Agregamos explícitamente el LEFT JOIN de la subcategoría
            "LEFT JOIN sub.category cat " +   // <-- Agregamos el LEFT JOIN de la categoría para evitar el Inner Join implícito
            "WHERE (" +
            "  (COALESCE(:name, '') = '' AND COALESCE(:sku, '') = '') " +
            "  OR (COALESCE(:name, '') != '' AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "  OR (COALESCE(:sku, '') != '' AND LOWER(p.sku) LIKE LOWER(CONCAT('%', :sku, '%')))" +
            ") " +
            "AND (COALESCE(:supplierSku, '') = '' OR LOWER(ps.supplierSku) LIKE LOWER(CONCAT('%', :supplierSku, '%'))) " +
            "AND (:minPrice IS NULL OR p.salePrice >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.salePrice <= :maxPrice) " +
            "AND (:subcategoryId IS NULL OR sub.id = :subcategoryId) " +  // <-- Usamos el alias 'sub'
            "AND (:categoryId IS NULL OR cat.id = :categoryId) " +        // <-- Usamos el alias 'cat'
            "AND (:supplierId IS NULL OR ps.supplier.id = :supplierId) " +
            "AND (:active IS NULL OR p.active = :active) " +
            "AND (:minStock IS NULL OR p.currentStock >= :minStock) " +
            "AND (:maxStock IS NULL OR p.currentStock <= :maxStock)")
    Page<Product> findProductsWithFilters(
            @Param("name") String name,
            @Param("sku") String sku,
            @Param("supplierSku") String supplierSku,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("subcategoryId") Long subcategoryId,
            @Param("categoryId") Long categoryId,
            @Param("supplierId") Long supplierId,
            @Param("active") Boolean active,
            @Param("minStock") Integer minStock,
            @Param("maxStock") Integer maxStock,
            Pageable pageable);

    // ========== MÉTODOS LEGACY ==========
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.productSuppliers ps " +
            "WHERE p.active = true " +
            "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:sku IS NULL OR p.sku LIKE CONCAT('%', :sku, '%')) " +
            "AND (:minPrice IS NULL OR p.salePrice >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.salePrice <= :maxPrice) " +
            "AND (:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId) " +
            "AND (:supplierId IS NULL OR ps.supplier.id = :supplierId) " +
            "AND (:dateFrom IS NULL OR p.createdAt >= :dateFrom) " +
            "AND (:dateTo IS NULL OR p.createdAt <= :dateTo)")
    List<Product> searchAllProductsWithDates(
            @Param("name") String name,
            @Param("sku") String sku,
            @Param("supplierSku") String supplierSku,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("subcategoryId") Long subcategoryId,
            @Param("supplierId") Long supplierId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo);

    @Query("SELECT p FROM Product p ORDER BY p.createdAt DESC")
    List<Product> findAllOrderByCreatedAtDesc();

    List<Product> findBySubcategoryIdAndActiveTrue(Long subcategoryId);
    List<Product> findBySubcategoryId(Long subcategoryId);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.productSuppliers ps WHERE ps.supplier.id = :supplierId AND p.active = true")
    List<Product> findProductsBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.productSuppliers ps WHERE ps.supplier.id = :supplierId")
    List<Product> findAllProductsBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT p FROM Product p WHERE p.currentStock < :threshold AND p.active = true")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    @Query("SELECT p FROM Product p WHERE p.currentStock < :threshold ORDER BY p.createdAt DESC")
    List<Product> findAllLowStockProducts(@Param("threshold") int threshold);

    // ========== ESTADÍSTICAS ==========
    @Query("SELECT COUNT(p) FROM Product p")
    Long countAllProducts();

    @Query("SELECT COALESCE(SUM(p.currentStock), 0) FROM Product p")
    Long getTotalStockAll();

    @Query("SELECT COALESCE(SUM(p.currentStock * p.costPrice), 0) FROM Product p")
    BigDecimal getTotalInventoryValueAll();

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

    // ========== SKU Y PROVEEDORES ==========
    @Query("SELECT MAX(p.id) FROM Product p")
    Long getMaxId();
}