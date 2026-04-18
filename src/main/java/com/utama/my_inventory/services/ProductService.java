package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.ProductRequestDTO;
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

    // Detalle esencial (sin campos pesados)
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
    List<ProductSummaryResponseDTO> getAllProductsSummary();
    List<ProductSummaryResponseDTO> getProductsBySubcategorySummary(Long subcategoryId);
    List<ProductSummaryResponseDTO> getProductsBySupplierSummary(Long supplierId);  // ✅ Mantiene supplierId
    List<ProductSummaryResponseDTO> getLowStockProductsSummary(int threshold);
    List<ProductSummaryResponseDTO> searchProductsSummary(String name, String sku,
                                                          BigDecimal minPrice, BigDecimal maxPrice,
                                                          Long subcategoryId, Long supplierId);

    // ========== GESTIÓN DE STOCK ==========
    ProductResponseDTO addStock(Long productId, int quantity, String reason, String user);
    ProductResponseDTO removeStock(Long productId, int quantity, String reason, String user);

    // ========== ESTADÍSTICAS ==========
    Long getTotalProductCount();
    Long getTotalStock();
    BigDecimal getTotalInventoryValue();

    // ========== CONSULTAS (versión completa) ==========
    List<ProductResponseDTO> getProductsBySubcategory(Long subcategoryId);
    List<ProductResponseDTO> getProductsBySupplier(Long supplierId);  // ✅ Mantiene supplierId
    List<ProductResponseDTO> getLowStockProducts(int threshold);
    List<ProductResponseDTO> searchProducts(String name, String sku, BigDecimal minPrice,
                                            BigDecimal maxPrice, Long subcategoryId, Long supplierId);

    // ========== MÉTODOS ADICIONALES PARA MÚLTIPLES PROVEEDORES ==========

    // Obtener todos los proveedores de un producto con sus SKUs
    List<SupplierAssociationResponseDTO> getProductSuppliers(Long productId);

    // Agregar un nuevo proveedor a un producto existente
    ProductResponseDTO addSupplierToProduct(Long productId, SupplierAssociationDTO supplierDTO);

    // Eliminar un proveedor de un producto
    void removeSupplierFromProduct(Long productId, Long supplierId);

    // Actualizar SKU de proveedor
    ProductResponseDTO updateSupplierSku(Long productId, Long supplierId, String supplierSku);
}