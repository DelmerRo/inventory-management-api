package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.ProductRequestDTO;
import com.utama.my_inventory.dtos.response.ProductResponseDTO;
import com.utama.my_inventory.dtos.response.ProductSummaryResponseDTO;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.mapper.ProductMapper;
import com.utama.my_inventory.repositories.ProductRepository;
import com.utama.my_inventory.services.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, allEntries = true)
    public ProductResponseDTO createProduct(ProductRequestDTO requestDTO) {
        log.info("Creating new product with SKU: {}", requestDTO.sku());

        validateUniqueSku(requestDTO.sku());

        Product product = productMapper.toEntity(requestDTO);
        Product savedProduct = productRepository.save(product);

        log.info("Product created with ID: {} and SKU: {}", savedProduct.getId(), savedProduct.getSku());
        return productMapper.toResponseDTO(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponseDTO getProductById(Long id) {
        log.info("Retrieving product with ID: {}", id);
        Product product = findActiveProductById(id);
        return productMapper.toResponseDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductBySku(String sku) {
        log.info("Retrieving product with SKU: {}", sku);
        Product product = productRepository.findBySkuAndActiveTrue(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return productMapper.toResponseDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'all'")
    public List<ProductResponseDTO> getAllProducts() {
        log.info("Retrieving all active products");
        List<Product> products = productRepository.findByActiveTrueOrderByNameAsc();
        return productMapper.toResponseDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "productSummary", key = "'all'")
    public List<ProductSummaryResponseDTO> getAllProductsSummary() {
        log.info("Retrieving all products summary");
        List<Product> products = productRepository.findByActiveTrueOrderByNameAsc();
        return productMapper.toSummaryDTOList(products);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, key = "#id")
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO) {
        log.info("Updating product with ID: {}", id);

        Product product = findActiveProductById(id);

        // Validar SKU único si cambió
        if (!product.getSku().equals(requestDTO.sku())) {
            validateUniqueSku(requestDTO.sku());
        }

        // Validar que el precio de venta no sea menor al costo
        validatePricing(requestDTO.costPrice(), requestDTO.salePrice());

        productMapper.updateEntityFromDTO(requestDTO, product);
        Product updatedProduct = productRepository.save(product);

        log.info("Product updated with ID: {}", id);
        return productMapper.toResponseDTO(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, key = "#id")
    public void deleteProduct(Long id) {
        log.info("Soft deleting product with ID: {}", id);

        Product product = findActiveProductById(id);
        product.setActive(false);
        productRepository.save(product);

        log.info("Product soft deleted with ID: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, key = "#id")
    public ProductResponseDTO toggleProductStatus(Long id) {
        log.info("Toggling status for product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setActive(!product.getActive());
        Product updatedProduct = productRepository.save(product);

        String status = updatedProduct.getActive() ? "activated" : "deactivated";
        log.info("Product {} with ID: {}", status, id);
        return productMapper.toResponseDTO(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsBySubcategory(Long subcategoryId) {
        log.info("Retrieving products by subcategory ID: {}", subcategoryId);
        List<Product> products = productRepository.findBySubcategoryIdAndActiveTrue(subcategoryId);
        return productMapper.toResponseDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsBySupplier(Long supplierId) {
        log.info("Retrieving products by supplier ID: {}", supplierId);
        List<Product> products = productRepository.findBySupplierIdAndActiveTrue(supplierId);
        return productMapper.toResponseDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {
        log.info("Retrieving products by category ID: {}", categoryId);
        List<Product> products = productRepository.findBySubcategoryCategoryIdAndActiveTrue(categoryId);
        return productMapper.toResponseDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getLowStockProducts(int threshold) {
        log.info("Retrieving low stock products (threshold: {})", threshold);
        List<Product> products = productRepository.findLowStockProducts(threshold);
        return productMapper.toResponseDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> searchProducts(String name, String sku, BigDecimal minPrice,
                                                   BigDecimal maxPrice, Long subcategoryId, Long supplierId) {
        log.info("Searching products with filters - name: {}, sku: {}, minPrice: {}, maxPrice: {}, subcategoryId: {}, supplierId: {}",
                name, sku, minPrice, maxPrice, subcategoryId, supplierId);

        List<Product> products = productRepository.searchProducts(name, sku, minPrice, maxPrice, subcategoryId, supplierId);
        return productMapper.toResponseDTOList(products);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, key = "#productId")
    public ProductResponseDTO addStock(Long productId, int quantity, String reason, String user) {
        log.info("Adding {} units to product ID: {}, reason: {}", quantity, productId, reason);

        Product product = findActiveProductById(productId);

        if (quantity <= 0) {
            throw new BusinessException("Quantity must be greater than 0");
        }

        product.addStock(quantity, reason, user);
        Product updatedProduct = productRepository.save(product);

        log.info("Stock added to product ID: {}. New stock: {}", productId, updatedProduct.getCurrentStock());
        return productMapper.toResponseDTO(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, key = "#productId")
    public ProductResponseDTO removeStock(Long productId, int quantity, String reason, String user) {
        log.info("Removing {} units from product ID: {}, reason: {}", quantity, productId, reason);

        Product product = findActiveProductById(productId);

        if (quantity <= 0) {
            throw new BusinessException("Quantity must be greater than 0");
        }

        if (product.getCurrentStock() < quantity) {
            throw new BusinessException("Insufficient stock. Available: " + product.getCurrentStock());
        }

        product.removeStock(quantity, reason, user);
        Product updatedProduct = productRepository.save(product);

        log.info("Stock removed from product ID: {}. New stock: {}", productId, updatedProduct.getCurrentStock());
        return productMapper.toResponseDTO(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, key = "#productId")
    public ProductResponseDTO updateStock(Long productId, int newStock, String reason, String user) {
        log.info("Updating stock for product ID: {} to {}", productId, newStock);

        Product product = findActiveProductById(productId);

        if (newStock < 0) {
            throw new BusinessException("Stock cannot be negative");
        }

        int difference = newStock - product.getCurrentStock();

        if (difference > 0) {
            product.addStock(difference, reason, user);
        } else if (difference < 0) {
            product.removeStock(Math.abs(difference), reason, user);
        }

        Product updatedProduct = productRepository.save(product);

        log.info("Stock updated for product ID: {}. New stock: {}", productId, updatedProduct.getCurrentStock());
        return productMapper.toResponseDTO(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalProductCount() {
        return productRepository.countActiveProducts();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalStock() {
        Long totalStock = productRepository.getTotalStock();
        return totalStock != null ? totalStock : 0L;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalInventoryValue() {
        BigDecimal totalValue = productRepository.getTotalInventoryValue();
        return totalValue != null ? totalValue : BigDecimal.ZERO;
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Product findActiveProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found or inactive with ID: " + id
                ));
    }

    private void validateUniqueSku(String sku) {
        if (productRepository.existsBySkuAndActiveTrue(sku)) {
            throw new BusinessException("Product already exists with SKU: " + sku);
        }
    }

    private void validatePricing(BigDecimal costPrice, BigDecimal salePrice) {
        if (costPrice != null && salePrice != null && salePrice.compareTo(costPrice) < 0) {
            throw new BusinessException("Sale price cannot be less than cost price");
        }
    }
}