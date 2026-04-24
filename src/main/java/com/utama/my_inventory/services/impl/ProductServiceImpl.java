package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.ProductRequestDTO;
import com.utama.my_inventory.dtos.request.QuickProductRequestDTO;
import com.utama.my_inventory.dtos.request.SupplierAssociationDTO;
import com.utama.my_inventory.dtos.response.product.ProductDetailResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductSummaryResponseDTO;
import com.utama.my_inventory.dtos.response.SupplierAssociationResponseDTO;
import com.utama.my_inventory.entities.*;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.mapper.ProductMapper;
import com.utama.my_inventory.repositories.ProductRepository;
import com.utama.my_inventory.repositories.ProductSupplierRepository;
import com.utama.my_inventory.repositories.SubcategoryRepository;
import com.utama.my_inventory.repositories.SupplierRepository;
import com.utama.my_inventory.services.ProductService;
import jakarta.persistence.EntityNotFoundException;
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
    private final SubcategoryRepository subcategoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductSupplierRepository productSupplierRepository;

    // ========== CRUD BÁSICO ==========

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, allEntries = true)
    public ProductResponseDTO createProduct(ProductRequestDTO requestDTO) {
        validatePricing(requestDTO.costPrice(), requestDTO.salePrice());

        boolean hasPrimary = requestDTO.suppliers().stream().anyMatch(SupplierAssociationDTO::isPrimary);
        if (!hasPrimary) {
            throw new BusinessException("Debe especificar al menos un proveedor como principal");
        }

        Product product = productMapper.toEntity(requestDTO);

        Subcategory subcategory = subcategoryRepository.findById(requestDTO.subcategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with ID: " + requestDTO.subcategoryId()));

        // ✅ Validación adicional: asegurar que la subcategoría tiene categoría
        if (subcategory.getCategory() == null) {
            throw new BusinessException("La subcategoría '" + subcategory.getName() + "' no tiene una categoría asociada");
        }

        product.setSubcategory(subcategory);
        product.setSku(null);
        product.setActive(true);

        Product savedProduct = productRepository.save(product);

        String primarySupplierSku = null;

        for (SupplierAssociationDTO supplierDTO : requestDTO.suppliers()) {
            Supplier supplier = supplierRepository.findById(supplierDTO.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + supplierDTO.supplierId()));

            ProductSupplier productSupplier = ProductSupplier.builder()
                    .product(savedProduct)
                    .supplier(supplier)
                    .supplierSku(supplierDTO.supplierSku())
                    .isPrimary(supplierDTO.isPrimary())
                    .notes(supplierDTO.notes())
                    .build();

            savedProduct.getProductSuppliers().add(productSupplier);

            if (supplierDTO.isPrimary() && supplierDTO.supplierSku() != null) {
                primarySupplierSku = supplierDTO.supplierSku();
            }
        }

        if (primarySupplierSku != null) {
            savedProduct.setSupplierSku(primarySupplierSku);
        }

        String sku = generateSkuFromSubcategory(subcategory, savedProduct.getId());
        savedProduct.setSku(sku);

        Product finalProduct = productRepository.save(savedProduct);

        log.info("Product created with ID: {} and SKU: {}, Supplier SKU: {}",
                finalProduct.getId(), finalProduct.getSku(), finalProduct.getSupplierSku());
        return productMapper.toResponseDTO(finalProduct);
    }

    private String generateSku(ProductRequestDTO requestDTO, Long productId) {
        Subcategory subcategory = subcategoryRepository.findById(requestDTO.subcategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with ID: " + requestDTO.subcategoryId()));

        String categoryCode = subcategory.getCategory().getName().substring(0, 3).toUpperCase();
        String subcategoryCode = subcategory.getName().substring(0, 3).toUpperCase();
        String sequentialNumber = String.format("%05d", productId);

        return String.format("%s-%s-%s", categoryCode, subcategoryCode, sequentialNumber);
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
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, key = "#id")
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO) {
        log.info("Updating product with ID: {}", id);

        Product product = findActiveProductById(id);
        validatePricing(requestDTO.costPrice(), requestDTO.salePrice());

        // Actualizar campos básicos
        productMapper.updateEntityFromDTO(requestDTO, product);

        // Actualizar subcategoría si cambió
        if (requestDTO.subcategoryId() != null && !requestDTO.subcategoryId().equals(product.getSubcategory().getId())) {
            Subcategory subcategory = subcategoryRepository.findById(requestDTO.subcategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with ID: " + requestDTO.subcategoryId()));
            product.setSubcategory(subcategory);

            // Regenerar SKU si cambió la subcategoría
            String newSku = generateSkuFromSubcategory(subcategory, product.getId());
            product.setSku(newSku);
        }

        // Actualizar proveedores
        // Limpiar proveedores existentes
        productSupplierRepository.deleteByProductId(product.getId());
        product.getProductSuppliers().clear();

        String primarySupplierSku = null;

        for (SupplierAssociationDTO supplierDTO : requestDTO.suppliers()) {
            Supplier supplier = supplierRepository.findById(supplierDTO.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + supplierDTO.supplierId()));

            ProductSupplier productSupplier = ProductSupplier.builder()
                    .product(product)
                    .supplier(supplier)
                    .supplierSku(supplierDTO.supplierSku())
                    .isPrimary(supplierDTO.isPrimary())
                    .notes(supplierDTO.notes())
                    .build();

            product.getProductSuppliers().add(productSupplier);

            if (supplierDTO.isPrimary() && supplierDTO.supplierSku() != null) {
                primarySupplierSku = supplierDTO.supplierSku();
            }
        }

        // Actualizar supplierSku en el producto
        product.setSupplierSku(primarySupplierSku);

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

    // ========== VERSIONES RESUMEN ==========

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "productSummary", key = "'all'")
    public List<ProductSummaryResponseDTO> getAllProductsSummary() {
        log.info("Retrieving all products summary");
        List<Product> products = productRepository.findByActiveTrueOrderByNameAsc();
        return productMapper.toSummaryDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponseDTO> getProductsBySubcategorySummary(Long subcategoryId) {
        log.info("Retrieving products summary by subcategory ID: {}", subcategoryId);
        List<Product> products = productRepository.findBySubcategoryIdAndActiveTrue(subcategoryId);
        return productMapper.toSummaryDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponseDTO> getProductsBySupplierSummary(Long supplierId) {
        log.info("Retrieving products summary by supplier ID: {}", supplierId);
        List<Product> products = productRepository.findProductsBySupplierId(supplierId);
        return productMapper.toSummaryDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponseDTO> getLowStockProductsSummary(int threshold) {
        log.info("Retrieving low stock products summary (threshold: {})", threshold);
        List<Product> products = productRepository.findLowStockProducts(threshold);
        return productMapper.toSummaryDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponseDTO> searchProductsSummary(String name, String sku,
                                                                 BigDecimal minPrice, BigDecimal maxPrice,
                                                                 Long subcategoryId, Long supplierId) {
        log.info("Searching products summary with filters");
        List<Product> products = productRepository.searchProducts(name, sku, minPrice, maxPrice, subcategoryId, supplierId);
        return productMapper.toSummaryDTOList(products);
    }

    // ========== GESTIÓN DE STOCK ==========

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

    // ========== ESTADÍSTICAS ==========

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

    // ========== CONSULTAS (VERSIÓN COMPLETA) ==========

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
        List<Product> products = productRepository.findProductsBySupplierId(supplierId);
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
        log.info("Searching products with filters");
        List<Product> products = productRepository.searchProducts(name, sku, minPrice, maxPrice, subcategoryId, supplierId);
        return productMapper.toResponseDTOList(products);
    }

    // ========== DETALLES ==========

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "productDetails", key = "#id")
    public ProductDetailResponseDTO getProductDetailById(Long id) {
        log.info("Retrieving product detail for ID: {}", id);
        Product product = findActiveProductById(id);
        return productMapper.toDetailDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponseDTO getProductDetailBySku(String sku) {
        log.info("Retrieving product detail for SKU: {}", sku);
        Product product = productRepository.findBySkuAndActiveTrue(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return productMapper.toDetailDTO(product);
    }

    // ========== MÉTODOS PARA MÚLTIPLES PROVEEDORES ==========

    @Override
    @Transactional(readOnly = true)
    public List<SupplierAssociationResponseDTO> getProductSuppliers(Long productId) {
        log.info("Getting suppliers for product ID: {}", productId);
        findActiveProductById(productId);

        List<ProductSupplier> productSuppliers = productSupplierRepository.findByProductId(productId);

        return productSuppliers.stream()
                .map(this::mapToSupplierAssociationResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, key = "#productId")
    public ProductResponseDTO addSupplierToProduct(Long productId, SupplierAssociationDTO supplierDTO) {
        log.info("Adding supplier {} to product {}", supplierDTO.supplierId(), productId);

        Product product = findActiveProductById(productId);

        if (productSupplierRepository.findByProductIdAndSupplierId(productId, supplierDTO.supplierId()).isPresent()) {
            throw new BusinessException("Supplier already associated with this product");
        }

        Supplier supplier = supplierRepository.findById(supplierDTO.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + supplierDTO.supplierId()));

        if (supplierDTO.isPrimary()) {
            productSupplierRepository.findByProductIdAndIsPrimaryTrue(productId)
                    .ifPresent(ps -> {
                        ps.setIsPrimary(false);
                        productSupplierRepository.save(ps);
                    });
        }

        ProductSupplier productSupplier = ProductSupplier.builder()
                .product(product)
                .supplier(supplier)
                .supplierSku(supplierDTO.supplierSku())
                .isPrimary(supplierDTO.isPrimary())
                .notes(supplierDTO.notes())
                .build();

        product.getProductSuppliers().add(productSupplier);
        Product updatedProduct = productRepository.save(product);

        log.info("Supplier added successfully to product {}", productId);
        return productMapper.toResponseDTO(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, key = "#productId")
    public void removeSupplierFromProduct(Long productId, Long supplierId) {
        log.info("Removing supplier {} from product {}", supplierId, productId);

        Product product = findActiveProductById(productId);

        ProductSupplier productSupplier = productSupplierRepository
                .findByProductIdAndSupplierId(productId, supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not associated with this product"));

        if (productSupplier.getIsPrimary() && product.getProductSuppliers().size() <= 1) {
            throw new BusinessException("Cannot remove the only supplier. Product must have at least one supplier");
        }

        product.getProductSuppliers().remove(productSupplier);
        productSupplierRepository.delete(productSupplier);

        if (productSupplier.getIsPrimary() && !product.getProductSuppliers().isEmpty()) {
            ProductSupplier newPrimary = product.getProductSuppliers().get(0);
            newPrimary.setIsPrimary(true);
            productSupplierRepository.save(newPrimary);
        }

        productRepository.save(product);
        log.info("Supplier removed successfully from product {}", productId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, key = "#productId")
    public ProductResponseDTO updateSupplierSku(Long productId, Long supplierId, String supplierSku) {
        log.info("Updating supplier SKU for product {} and supplier {}", productId, supplierId);

        ProductSupplier productSupplier = productSupplierRepository
                .findByProductIdAndSupplierId(productId, supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not associated with this product"));

        productSupplier.setSupplierSku(supplierSku);
        productSupplierRepository.save(productSupplier);

        Product product = findActiveProductById(productId);
        log.info("Supplier SKU updated successfully");
        return productMapper.toResponseDTO(product);
    }

    // ========== MÉTODOS PRIVADOS ==========

    private Product findActiveProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or inactive with ID: " + id));
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

    private SupplierAssociationResponseDTO mapToSupplierAssociationResponseDTO(ProductSupplier ps) {
        return new SupplierAssociationResponseDTO(
                ps.getId(),
                ps.getSupplier().getId(),
                ps.getSupplier().getName(),
                ps.getSupplierSku(),
                ps.getIsPrimary(),
                ps.getNotes()
        );
    }

    // ProductServiceImpl.java - Implementación

    // ProductServiceImpl.java - Implementación

    @Override
    @Transactional
    public ProductResponseDTO createQuickProduct(QuickProductRequestDTO requestDTO) {
        try {
            log.info("========== INICIO createQuickProduct ==========");
            log.info("SKU proveedor: {}", requestDTO.getSupplierSku());
            log.info("Nombre: {}", requestDTO.getName());
            log.info("subcategoryId: {}", requestDTO.getSubcategoryId());

            // Verificar si ya existe un producto con ese supplierSku
            if (productRepository.existsBySupplierSku(requestDTO.getSupplierSku())) {
                throw new BusinessException("Ya existe un producto con el SKU de proveedor: " + requestDTO.getSupplierSku());
            }

            // Obtener la subcategoría
            Subcategory subcategory = subcategoryRepository.findById(requestDTO.getSubcategoryId())
                    .orElseThrow(() -> {
                        log.error("Subcategoría NO encontrada con ID: {}", requestDTO.getSubcategoryId());
                        return new EntityNotFoundException("Subcategoría no encontrada con ID: " + requestDTO.getSubcategoryId());
                    });

            log.info("Subcategoría encontrada: {} (ID: {})", subcategory.getName(), subcategory.getId());

            // Crear producto temporal
            Product tempProduct = Product.builder()
                    .supplierSku(requestDTO.getSupplierSku())
                    .name(requestDTO.getName())
                    .description("Producto creado rápidamente desde pedido de compra. Pendiente de activación.")
                    .currentStock(0)
                    .subcategory(subcategory)
                    .active(false)
                    .build();

            log.info("Producto temporal creado, guardando...");
            Product savedProduct = productRepository.save(tempProduct);
            log.info("Producto guardado con ID: {}", savedProduct.getId());

            // Generar SKU
            String sku = generateSkuFromSubcategory(subcategory, savedProduct.getId());
            log.info("SKU generado: {}", sku);

            savedProduct.setSku(sku);
            Product finalProduct = productRepository.save(savedProduct);

            log.info("Producto rápido creado exitosamente - ID: {}, SKU: {}", finalProduct.getId(), finalProduct.getSku());
            log.info("========== FIN createQuickProduct ==========");

            return productMapper.toResponseDTO(finalProduct);

        } catch (Exception e) {
            log.error("ERROR en createQuickProduct: {}", e.getMessage(), e);
            throw new RuntimeException("Error creando producto: " + e.getMessage(), e);
        }
    }

    /**
     * Genera SKU en formato CAT-SUB-XXXXX a partir de una subcategoría y un ID
     * Ejemplo: LIV-ALF-00001
     */
    private String generateSkuFromSubcategory(Subcategory subcategory, Long productId) {
        log.info("Generando SKU - subcategory: {}, productId: {}", subcategory.getName(), productId);

        // Verificar que la categoría no sea null
        if (subcategory.getCategory() == null) {
            log.error("La subcategoría {} no tiene categoría asociada", subcategory.getName());
            throw new BusinessException("La subcategoría '" + subcategory.getName() + "' no tiene una categoría asociada");
        }

        String categoryName = subcategory.getCategory().getName();
        String subcategoryName = subcategory.getName();

        log.info("Nombre categoría: '{}', Nombre subcategoría: '{}'", categoryName, subcategoryName);

        if (categoryName == null || categoryName.trim().isEmpty()) {
            log.error("Nombre de categoría inválido: '{}'", categoryName);
            throw new BusinessException("La categoría tiene un nombre inválido");
        }

        if (subcategoryName == null || subcategoryName.trim().isEmpty()) {
            log.error("Nombre de subcategoría inválido: '{}'", subcategoryName);
            throw new BusinessException("La subcategoría '" + subcategoryName + "' tiene un nombre inválido");
        }

        // ✅ Normalizar: eliminar tildes, ñ y caracteres especiales
        String categoryCode = normalizeToCode(categoryName);
        String subcategoryCode = normalizeToCode(subcategoryName);
        String sequentialNumber = String.format("%05d", productId);

        String sku = String.format("%s-%s-%s", categoryCode, subcategoryCode, sequentialNumber);

        // ✅ Verificar si el SKU ya existe y regenerar si es necesario
        int counter = 0;
        String finalSku = sku;
        while (productRepository.existsBySkuAndActiveTrue(finalSku) && counter < 10) {
            log.warn("SKU duplicado encontrado: {}, regenerando...", finalSku);
            long newId = productId + counter + 1;
            sequentialNumber = String.format("%05d", newId);
            finalSku = String.format("%s-%s-%s", categoryCode, subcategoryCode, sequentialNumber);
            counter++;
        }

        log.info("SKU generado: '{}' (Original: '{}')", finalSku, sku);
        return finalSku;
    }

    /**
     * Normaliza un texto para usarlo como código de SKU.
     * Elimina tildes, la ñ, caracteres especiales y espacios.
     * Ejemplos:
     * - "Baño" -> "BAN"
     * - "Accesorios de baño" -> "ACC"
     * - "Cocina & Mesa" -> "COC"
     * - "Ropa de cama" -> "ROP"
     */
    private String normalizeToCode(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "XXX";
        }

        // Convertir a mayúsculas y eliminar acentos
        String normalized = text
                .toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ñ", "N")
                .replace("Ç", "C")
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace("&", "Y")
                .replace("/", "")
                .replace("\\", "")
                .replace(".", "")
                .replace(",", "");

        // Eliminar cualquier carácter que no sea A-Z o 0-9
        normalized = normalized.replaceAll("[^A-Z0-9]", "");

        // Si después de limpiar tiene menos de 3 caracteres, completar con X
        if (normalized.length() < 3) {
            normalized = normalized + "X".repeat(3 - normalized.length());
        }

        // Tomar solo los primeros 3 caracteres
        return normalized.substring(0, 3);
    }

    // ProductServiceImpl.java
    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductBySupplierSku(String supplierSku) {
        log.info("Buscando producto por SKU de proveedor: {}", supplierSku);

        Product product = productRepository.findBySupplierSku(supplierSku)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con SKU de proveedor: " + supplierSku));

        return productMapper.toResponseDTO(product);
    }

}