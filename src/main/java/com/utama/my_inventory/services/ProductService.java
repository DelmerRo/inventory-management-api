package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.ProductRequestDTO;
import com.utama.my_inventory.dtos.request.QuickProductRequestDTO;
import com.utama.my_inventory.dtos.request.SupplierAssociationDTO;
import com.utama.my_inventory.dtos.response.SupplierAssociationResponseDTO;
import com.utama.my_inventory.dtos.response.product.PagedProductResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductDetailResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductSummaryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    // ========== CRUD BÁSICO ==========
    ProductResponseDTO createProduct(ProductRequestDTO requestDTO);
    ProductDetailResponseDTO getProductDetailById(Long id);
    ProductDetailResponseDTO getProductDetailBySku(String sku);
    ProductResponseDTO getProductById(Long id);
    ProductResponseDTO getProductBySku(String sku);
    List<ProductResponseDTO> getAllProducts();
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO);
    void deleteProduct(Long id);
    ProductResponseDTO toggleProductStatus(Long id);

    // ========== PAGINACIÓN CON FILTROS (VERSIÓN COMPLETA) ==========
    // Modificar esta firma en ProductService.java
    // ProductService.java
    PagedProductResponseDTO getProductsPaged(
            String name, String sku, String supplierSku,
            BigDecimal minPrice, BigDecimal maxPrice,
            Long subcategoryId, Long categoryId, // <-- Añadido aquí
            Long supplierId, Boolean active, Integer minStock, Integer maxStock,
            Pageable pageable);

    // ========== BÚSQUEDA GENERAL ==========
    Page<ProductSummaryResponseDTO> searchProductsGeneral(
            String query, BigDecimal minPrice, BigDecimal maxPrice,
            Long subcategoryId, Long supplierId, Boolean active, Pageable pageable);

    // ========== VERSIONES RESUMEN (legacy) ==========
    List<ProductSummaryResponseDTO> getAllProductsSummary();
    List<ProductSummaryResponseDTO> getProductsBySubcategorySummary(Long subcategoryId);
    List<ProductSummaryResponseDTO> getProductsBySupplierSummary(Long supplierId);
    List<ProductSummaryResponseDTO> getLowStockProductsSummary(int threshold);
    List<ProductSummaryResponseDTO> searchProductsSummary(String name, String sku, String supplierSku,
                                                          BigDecimal minPrice, BigDecimal maxPrice,
                                                          Long subcategoryId, Long supplierId,
                                                          String dateFrom, String dateTo);

    // ========== GESTIÓN DE STOCK ==========
    ProductResponseDTO addStock(Long productId, int quantity, String reason, String user);
    ProductResponseDTO removeStock(Long productId, int quantity, String reason, String user);

    // ========== ESTADÍSTICAS ==========
    Long getTotalProductCount();
    Long getTotalStock();
    BigDecimal getTotalInventoryValue();

    // ========== CONSULTAS (legacy) ==========
    List<ProductResponseDTO> getProductsBySubcategory(Long subcategoryId);
    List<ProductResponseDTO> getProductsBySupplier(Long supplierId);
    List<ProductResponseDTO> getLowStockProducts(int threshold);
    List<ProductResponseDTO> searchProducts(String name, String sku, BigDecimal minPrice,
                                            BigDecimal maxPrice, Long subcategoryId, Long supplierId);

    // ========== PROVEEDORES ==========
    List<SupplierAssociationResponseDTO> getProductSuppliers(Long productId);
    ProductResponseDTO addSupplierToProduct(Long productId, SupplierAssociationDTO supplierDTO);
    void removeSupplierFromProduct(Long productId, Long supplierId);
    ProductResponseDTO updateSupplierSku(Long productId, Long supplierId, String supplierSku);
    ProductResponseDTO getProductBySupplierSku(String supplierSku);
    List<ProductSummaryResponseDTO> findByProductSupplierSku(String supplierSku);
    ProductResponseDTO createQuickProduct(QuickProductRequestDTO requestDTO);
}