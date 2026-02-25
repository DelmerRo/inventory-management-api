package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.ProductRequestDTO;
import com.utama.my_inventory.dtos.response.ProductResponseDTO;
import com.utama.my_inventory.dtos.response.ProductSummaryResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponseDTO createProduct(ProductRequestDTO requestDTO);
    ProductResponseDTO getProductById(Long id);
    ProductResponseDTO getProductBySku(String sku);
    List<ProductResponseDTO> getAllProducts();
    List<ProductSummaryResponseDTO> getAllProductsSummary();
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO);
    void deleteProduct(Long id);
    ProductResponseDTO toggleProductStatus(Long id);

    List<ProductResponseDTO> getProductsBySubcategory(Long subcategoryId);
    List<ProductResponseDTO> getProductsBySupplier(Long supplierId);
    List<ProductResponseDTO> getLowStockProducts(int threshold);
    List<ProductResponseDTO> searchProducts(String name, String sku, BigDecimal minPrice,
                                            BigDecimal maxPrice, Long subcategoryId, Long supplierId);

    ProductResponseDTO addStock(Long productId, int quantity, String reason, String user);
    ProductResponseDTO removeStock(Long productId, int quantity, String reason, String user);

    Long getTotalProductCount();
    Long getTotalStock();
    BigDecimal getTotalInventoryValue();
}