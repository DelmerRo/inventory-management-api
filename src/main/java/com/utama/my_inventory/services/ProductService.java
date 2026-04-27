package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.ProductRequestDTO;
import com.utama.my_inventory.dtos.request.QuickProductRequestDTO;
import com.utama.my_inventory.dtos.request.SupplierAssociationDTO;
import com.utama.my_inventory.dtos.response.SupplierAssociationResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductDetailResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductSummaryResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    // ========== CRUD BÁSICO ==========
    ProductResponseDTO createProduct(ProductRequestDTO requestDTO);

    // Detalle esencial
    ProductDetailResponseDTO getProductDetailById(Long id);
    ProductDetailResponseDTO getProductDetailBySku(String sku);

    // Versiones completas
    ProductResponseDTO getProductById(Long id);
    ProductResponseDTO getProductBySku(String sku);
    List<ProductResponseDTO> getAllProducts();
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO);
    void deleteProduct(Long id);
    ProductResponseDTO toggleProductStatus(Long id);

    // ========== VERSIONES RESUMEN (para listados) ==========
    default List<ProductSummaryResponseDTO> getAllProductsSummary() {
        return null;
    }

    List<ProductSummaryResponseDTO> getProductsBySubcategorySummary(Long subcategoryId);
    List<ProductSummaryResponseDTO> getProductsBySupplierSummary(Long supplierId);
    List<ProductSummaryResponseDTO> getLowStockProductsSummary(int threshold);

    // ✅ BÚSQUEDA CON FILTROS (incluye fechas)
    List<ProductSummaryResponseDTO> searchProductsSummary(String name, String sku,
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

    // ========== CONSULTAS (versión completa) ==========
    List<ProductResponseDTO> getProductsBySubcategory(Long subcategoryId);
    List<ProductResponseDTO> getProductsBySupplier(Long supplierId);
    List<ProductResponseDTO> getLowStockProducts(int threshold);
    List<ProductResponseDTO> searchProducts(String name, String sku, BigDecimal minPrice,
                                            BigDecimal maxPrice, Long subcategoryId, Long supplierId);

    // ========== MÉTODOS PARA MÚLTIPLES PROVEEDORES ==========
    List<SupplierAssociationResponseDTO> getProductSuppliers(Long productId);
    ProductResponseDTO addSupplierToProduct(Long productId, SupplierAssociationDTO supplierDTO);
    void removeSupplierFromProduct(Long productId, Long supplierId);
    ProductResponseDTO updateSupplierSku(Long productId, Long supplierId, String supplierSku);

    // ========== PRODUCTOS RÁPIDOS ==========
    ProductResponseDTO createQuickProduct(QuickProductRequestDTO requestDTO);
    ProductResponseDTO getProductBySupplierSku(String supplierSku);
}