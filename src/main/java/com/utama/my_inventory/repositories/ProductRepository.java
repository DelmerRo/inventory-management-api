package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.Product;
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

    // ✅ NUEVOS: Para mostrar TODOS los productos (activos e inactivos)
    List<Product> findAllByOrderByNameAsc();

    @Query("SELECT p FROM Product p ORDER BY p.createdAt DESC")
    List<Product> findAllOrderByCreatedAtDesc();

    @Query("SELECT p FROM Product p WHERE p.active = :active ORDER BY p.createdAt DESC")
    List<Product> findAllByActiveOrderByCreatedAtDesc(@Param("active") Boolean active);

    // ========== POR RELACIONES ==========
    List<Product> findBySubcategoryIdAndActiveTrue(Long subcategoryId);

    // ✅ NUEVO: Productos por subcategoría (todos, sin filtrar por active)
    List<Product> findBySubcategoryId(Long subcategoryId);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.productSuppliers ps WHERE ps.supplier.id = :supplierId AND p.active = true")
    List<Product> findProductsBySupplierId(@Param("supplierId") Long supplierId);

    // ✅ NUEVO: Productos por proveedor (todos, sin filtrar por active)
    @Query("SELECT DISTINCT p FROM Product p JOIN p.productSuppliers ps WHERE ps.supplier.id = :supplierId")
    List<Product> findAllProductsBySupplierId(@Param("supplierId") Long supplierId);

    // ========== STOCK ==========
    @Query("SELECT p FROM Product p WHERE p.currentStock < :threshold AND p.active = true")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    // ✅ NUEVO: Productos con stock bajo (todos, sin filtrar por active)
    @Query("SELECT p FROM Product p WHERE p.currentStock < :threshold ORDER BY p.createdAt DESC")
    List<Product> findAllLowStockProducts(@Param("threshold") int threshold);

    // ========== BÚSQUEDA CON FILTROS (sin fechas) - VERSIÓN COMPLETA ==========
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.productSuppliers ps " +
            "WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:sku IS NULL OR p.sku LIKE CONCAT('%', :sku, '%')) " +
            "AND (:minPrice IS NULL OR p.salePrice >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.salePrice <= :maxPrice) " +
            "AND (:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId) " +
            "AND (:supplierId IS NULL OR ps.supplier.id = :supplierId)")
    List<Product> searchAllProducts(@Param("name") String name,
                                    @Param("sku") String sku,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("subcategoryId") Long subcategoryId,
                                    @Param("supplierId") Long supplierId);

    // ========== BÚSQUEDA CON FILTROS + FECHAS - VERSIÓN COMPLETA ==========
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.productSuppliers ps " +
            "WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:sku IS NULL OR p.sku LIKE CONCAT('%', :sku, '%')) " +
            "AND (:minPrice IS NULL OR p.salePrice >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.salePrice <= :maxPrice) " +
            "AND (:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId) " +
            "AND (:supplierId IS NULL OR ps.supplier.id = :supplierId) " +
            "AND (:dateFrom IS NULL OR p.createdAt >= :dateFrom) " +
            "AND (:dateTo IS NULL OR p.createdAt <= :dateTo) " +
            "ORDER BY p.createdAt DESC")
    List<Product> searchAllProductsWithDates(@Param("name") String name,
                                             @Param("sku") String sku,
                                             @Param("minPrice") BigDecimal minPrice,
                                             @Param("maxPrice") BigDecimal maxPrice,
                                             @Param("subcategoryId") Long subcategoryId,
                                             @Param("supplierId") Long supplierId,
                                             @Param("dateFrom") LocalDateTime dateFrom,
                                             @Param("dateTo") LocalDateTime dateTo);

    // ========== BÚSQUEDA CON FILTROS (solo activos) - MANTENER PARA COMPATIBILIDAD ==========
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
    List<Product> searchProductsWithDates(@Param("name") String name,
                                          @Param("sku") String sku,
                                          @Param("minPrice") BigDecimal minPrice,
                                          @Param("maxPrice") BigDecimal maxPrice,
                                          @Param("subcategoryId") Long subcategoryId,
                                          @Param("supplierId") Long supplierId,
                                          @Param("dateFrom") LocalDateTime dateFrom,
                                          @Param("dateTo") LocalDateTime dateTo);

    // ========== ESTADÍSTICAS ==========
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    Long countActiveProducts();

    @Query("SELECT COUNT(p) FROM Product p")
    Long countAllProducts();

    @Query("SELECT COALESCE(SUM(p.currentStock), 0) FROM Product p WHERE p.active = true")
    Long getTotalStock();

    @Query("SELECT COALESCE(SUM(p.currentStock), 0) FROM Product p")
    Long getTotalStockAll();

    @Query("SELECT COALESCE(SUM(p.currentStock * p.costPrice), 0) FROM Product p WHERE p.active = true")
    BigDecimal getTotalInventoryValue();

    @Query("SELECT COALESCE(SUM(p.currentStock * p.costPrice), 0) FROM Product p")
    BigDecimal getTotalInventoryValueAll();

    // ========== SKU Y PROVEEDORES ==========
    Optional<Product> findBySku(String sku);
    @Query("SELECT MAX(p.id) FROM Product p")
    Long getMaxId();

    // ⚠️ DEPRECADO - Usar ProductSupplierRepository.findBySupplierSku en su lugar
    @Deprecated
    boolean existsBySupplierSku(String supplierSku);

    @Deprecated
    Optional<Product> findBySupplierSku(String supplierSku);
}