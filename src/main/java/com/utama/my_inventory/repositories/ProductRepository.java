package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Búsquedas básicas
    Optional<Product> findByIdAndActiveTrue(Long id);
    Optional<Product> findBySkuAndActiveTrue(String sku);
    List<Product> findByActiveTrue();
    List<Product> findByActiveTrueOrderByNameAsc();

    // Búsquedas por relación
    List<Product> findBySubcategoryIdAndActiveTrue(Long subcategoryId);
    List<Product> findBySupplierIdAndActiveTrue(Long supplierId);
    List<Product> findBySubcategoryCategoryIdAndActiveTrue(Long categoryId);

    // Búsquedas por stock
    List<Product> findByCurrentStockGreaterThanAndActiveTrue(int stock);
    List<Product> findByCurrentStockLessThanAndActiveTrue(int stock);
    List<Product> findByCurrentStockEqualsAndActiveTrue(int stock);

    // Búsquedas por precio
    List<Product> findBySalePriceBetweenAndActiveTrue(BigDecimal minPrice, BigDecimal maxPrice);
    List<Product> findBySalePriceGreaterThanEqualAndActiveTrue(BigDecimal minPrice);
    List<Product> findBySalePriceLessThanEqualAndActiveTrue(BigDecimal maxPrice);

    // Validaciones
    boolean existsBySkuAndActiveTrue(String sku);
    boolean existsBySkuAndIdNotAndActiveTrue(String sku, Long id);

    // Consultas personalizadas
    @Query("SELECT p FROM Product p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:sku IS NULL OR p.sku LIKE CONCAT('%', :sku, '%')) AND " +
            "(:minPrice IS NULL OR p.salePrice >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.salePrice <= :maxPrice) AND " +
            "(:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId) AND " +
            "(:supplierId IS NULL OR p.supplier.id = :supplierId) AND " +
            "p.active = true")
    List<Product> searchProducts(
            @Param("name") String name,
            @Param("sku") String sku,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("subcategoryId") Long subcategoryId,
            @Param("supplierId") Long supplierId);

    // Estadísticas
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    Long countActiveProducts();

    @Query("SELECT SUM(p.currentStock) FROM Product p WHERE p.active = true")
    Long getTotalStock();

    @Query("SELECT SUM(p.currentStock * p.costPrice) FROM Product p WHERE p.active = true AND p.costPrice IS NOT NULL")
    BigDecimal getTotalInventoryValue();

    // Productos con stock bajo (umbral configurable)
    @Query("SELECT p FROM Product p WHERE p.currentStock < :threshold AND p.active = true")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);
}