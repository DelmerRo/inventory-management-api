package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByActiveTrue();

    Page<Product> findByActiveTrue(Pageable pageable);

    List<Product> findBySubcategoryId(Long subcategoryId);

    List<Product> findBySupplierId(Long supplierId);

    List<Product> findByCurrentStockLessThan(Integer stockThreshold);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.currentStock = 0")
    List<Product> findOutOfStock();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.salePrice BETWEEN :minPrice AND :maxPrice")
    List<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    long countActiveProducts();

    @Query("SELECT COALESCE(SUM(p.currentStock), 0) FROM Product p WHERE p.active = true")
    Integer sumTotalStock();

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.currentStock ASC")
    List<Product> findAllOrderByStockAsc();

    boolean existsBySku(String sku);

    boolean existsByIdAndActiveTrue(Long id);
}
