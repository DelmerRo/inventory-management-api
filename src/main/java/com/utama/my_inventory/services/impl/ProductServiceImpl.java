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

        if (subcategory.getCategory() == null) {
            throw new BusinessException("La subcategoría '" + subcategory.getName() + "' no tiene una categoría asociada");
        }

        product.setSubcategory(subcategory);
        product.setSku(null);
        product.setActive(true);
        // ✅ NO asignar supplierSku aquí

        Product savedProduct = productRepository.save(product);

        // ✅ Crear relaciones ProductSupplier
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

            productSupplierRepository.save(productSupplier);
            savedProduct.getProductSuppliers().add(productSupplier);
        }

        // Generar SKU después de tener ID
        String sku = generateSkuFromSubcategory(subcategory, savedProduct.getId());
        savedProduct.setSku(sku);

        Product finalProduct = productRepository.save(savedProduct);

        log.info("Product created with ID: {} and SKU: {}", finalProduct.getId(), finalProduct.getSku());
        return productMapper.toResponseDTO(finalProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, allEntries = true)
    public ProductResponseDTO createQuickProduct(QuickProductRequestDTO requestDTO) {
        log.info("========== INICIO createQuickProduct ==========");
        log.info("SKU proveedor: {}, Nombre: {}, subcategoryId: {}",
                requestDTO.getSupplierSku(), requestDTO.getName(), requestDTO.getSubcategoryId());

        // ✅ Verificar usando ProductSupplier
        if (productSupplierRepository.existsBySupplierSku(requestDTO.getSupplierSku())) {
            throw new BusinessException("Ya existe un producto con el SKU de proveedor: " + requestDTO.getSupplierSku());
        }

        // Obtener la subcategoría
        Subcategory subcategory = subcategoryRepository.findById(requestDTO.getSubcategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Subcategoría no encontrada con ID: " + requestDTO.getSubcategoryId()));

        log.info("Subcategoría encontrada: {} (ID: {})", subcategory.getName(), subcategory.getId());

        // Crear producto (SIN supplierSku)
        Product tempProduct = Product.builder()
                .name(requestDTO.getName())
                .description("Producto creado rápidamente desde pedido de compra. Pendiente de activación.")
                .currentStock(0)
                .subcategory(subcategory)
                .active(false)
                .build();

        Product savedProduct = productRepository.save(tempProduct);
        log.info("Producto guardado con ID: {}", savedProduct.getId());

        // Generar SKU
        String sku = generateSkuFromSubcategory(subcategory, savedProduct.getId());
        savedProduct.setSku(sku);

        // ⚠️ NOTA: createQuickProduct no asigna proveedor porque el DTO no tiene supplierId
        // El proveedor se asignará cuando se reciba el pedido de compra

        Product finalProduct = productRepository.save(savedProduct);

        log.info("Producto rápido creado exitosamente - ID: {}, SKU: {}", finalProduct.getId(), finalProduct.getSku());
        log.info("========== FIN createQuickProduct ==========");

        return productMapper.toResponseDTO(finalProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductBySupplierSku(String supplierSku) {
        log.info("Buscando producto por SKU de proveedor: {}", supplierSku);

        // ✅ Buscar en ProductSupplier
        ProductSupplier productSupplier = productSupplierRepository
                .findBySupplierSku(supplierSku)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con SKU de proveedor: " + supplierSku));

        return productMapper.toResponseDTO(productSupplier.getProduct());
    }

    // Resto de métodos sin cambios...
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

        productMapper.updateEntityFromDTO(requestDTO, product);

        if (requestDTO.subcategoryId() != null && !requestDTO.subcategoryId().equals(product.getSubcategory().getId())) {
            Subcategory subcategory = subcategoryRepository.findById(requestDTO.subcategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with ID: " + requestDTO.subcategoryId()));
            product.setSubcategory(subcategory);
            String newSku = generateSkuFromSubcategory(subcategory, product.getId());
            product.setSku(newSku);
        }

        // ✅ Actualizar proveedores
        productSupplierRepository.deleteByProductId(product.getId());
        product.getProductSuppliers().clear();

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

            productSupplierRepository.save(productSupplier);
            product.getProductSuppliers().add(productSupplier);
        }

        // ✅ NO asignar supplierSku en Product

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated with ID: {}", id);
        return productMapper.toResponseDTO(updatedProduct);
    }

    // Resto de métodos (deleteProduct, toggleProductStatus, getters, stock methods) se mantienen igual...

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
    @Cacheable(value = "productSummary", key = "'all-including-inactive'")
    public List<ProductSummaryResponseDTO> getAllProductsSummary() {
        log.info("Retrieving ALL products summary (including inactive) - ordered by createdAt DESC");
        // ✅ AHORA: Trae TODOS los productos (activos e inactivos)
        List<Product> products = productRepository.findAllOrderByCreatedAtDesc();
        return productMapper.toSummaryDTOList(products);
    }

    // ✅ NUEVO: Método para obtener solo productos activos (si se necesita)
    @Transactional(readOnly = true)
    @Cacheable(value = "productSummary", key = "'active-only'")
    public List<ProductSummaryResponseDTO> getActiveProductsSummary() {
        log.info("Retrieving ACTIVE products summary only");
        List<Product> products = productRepository.findAllByActiveOrderByCreatedAtDesc(true);
        return productMapper.toSummaryDTOList(products);
    }

    // ✅ NUEVO: Método para obtener solo productos inactivos
    @Transactional(readOnly = true)
    @Cacheable(value = "productSummary", key = "'inactive-only'")
    public List<ProductSummaryResponseDTO> getInactiveProductsSummary() {
        log.info("Retrieving INACTIVE products summary only");
        List<Product> products = productRepository.findAllByActiveOrderByCreatedAtDesc(false);
        return productMapper.toSummaryDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponseDTO> getProductsBySubcategorySummary(Long subcategoryId) {
        log.info("Retrieving ALL products summary by subcategory ID: {} (including inactive)", subcategoryId);
        // ✅ AHORA: Trae TODOS los productos de la subcategoría
        List<Product> products = productRepository.findBySubcategoryId(subcategoryId);
        // Ordenar por fecha descendente
        products.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return productMapper.toSummaryDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponseDTO> getProductsBySupplierSummary(Long supplierId) {
        log.info("Retrieving ALL products summary by supplier ID: {} (including inactive)", supplierId);
        // ✅ AHORA: Trae TODOS los productos del proveedor
        List<Product> products = productRepository.findAllProductsBySupplierId(supplierId);
        // Ordenar por fecha descendente
        products.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return productMapper.toSummaryDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponseDTO> getLowStockProductsSummary(int threshold) {
        log.info("Retrieving ALL low stock products summary (threshold: {}) - including inactive", threshold);
        // ✅ AHORA: Trae TODOS los productos con stock bajo (activos e inactivos)
        List<Product> products = productRepository.findAllLowStockProducts(threshold);
        return productMapper.toSummaryDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponseDTO> searchProductsSummary(String name, String sku,
                                                                 BigDecimal minPrice, BigDecimal maxPrice,
                                                                 Long subcategoryId, Long supplierId,
                                                                 String dateFrom, String dateTo) {
        log.info("Searching ALL products summary with filters (including inactive)");

        java.time.LocalDateTime startDate = null;
        java.time.LocalDateTime endDate = null;

        if (dateFrom != null && !dateFrom.isEmpty()) {
            startDate = java.time.LocalDateTime.parse(dateFrom + "T00:00:00");
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            endDate = java.time.LocalDateTime.parse(dateTo + "T23:59:59");
        }

        // ✅ AHORA: Busca en TODOS los productos (sin filtro active)
        List<Product> products = productRepository.searchAllProductsWithDates(
                name, sku, minPrice, maxPrice, subcategoryId, supplierId, startDate, endDate);

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
        // ✅ AHORA: Cuenta TODOS los productos
        return productRepository.countAllProducts();
    }

    // ✅ NUEVO: Método para contar solo inactivos
    @Transactional(readOnly = true)
    public Long getInactiveProductCount() {
        return productRepository.countAllProducts() - productRepository.countActiveProducts();
    }

    @Transactional(readOnly = true)
    public Long getActiveProductCount() {
        return productRepository.countActiveProducts();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalStock() {
        Long totalStock = productRepository.getTotalStockAll();
        return totalStock != null ? totalStock : 0L;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalInventoryValue() {
        BigDecimal totalValue = productRepository.getTotalInventoryValueAll();
        return totalValue != null ? totalValue : BigDecimal.ZERO;
    }

    // ========== CONSULTAS (VERSIÓN COMPLETA) ==========
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsBySubcategory(Long subcategoryId) {
        List<Product> products = productRepository.findBySubcategoryIdAndActiveTrue(subcategoryId);
        return productMapper.toResponseDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsBySupplier(Long supplierId) {
        List<Product> products = productRepository.findProductsBySupplierId(supplierId);
        return productMapper.toResponseDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getLowStockProducts(int threshold) {
        List<Product> products = productRepository.findLowStockProducts(threshold);
        return productMapper.toResponseDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> searchProducts(String name, String sku, BigDecimal minPrice,
                                                   BigDecimal maxPrice, Long subcategoryId, Long supplierId) {
        List<Product> products = productRepository.searchProducts(name, sku, minPrice, maxPrice, subcategoryId, supplierId);
        return productMapper.toResponseDTOList(products);
    }

    // ========== DETALLES ==========
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "productDetails", key = "#id")
    public ProductDetailResponseDTO getProductDetailById(Long id) {
        Product product = findActiveProductById(id);
        return productMapper.toDetailDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponseDTO getProductDetailBySku(String sku) {
        Product product = productRepository.findBySkuAndActiveTrue(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return productMapper.toDetailDTO(product);
    }

    // ========== MÉTODOS PARA MÚLTIPLES PROVEEDORES ==========
    @Override
    @Transactional(readOnly = true)
    public List<SupplierAssociationResponseDTO> getProductSuppliers(Long productId) {
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

        productSupplierRepository.save(productSupplier);
        product.getProductSuppliers().add(productSupplier);
        Product updatedProduct = productRepository.save(product);

        log.info("Supplier added successfully to product {}", productId);
        return productMapper.toResponseDTO(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productSummary"}, key = "#productId")
    public void removeSupplierFromProduct(Long productId, Long supplierId) {
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

    private void validatePricing(BigDecimal costPrice, BigDecimal salePrice) {
        if (costPrice != null && salePrice != null && salePrice.compareTo(costPrice) < 0) {
            throw new BusinessException("Sale price cannot be less than cost price");
        }
    }

    private SupplierAssociationResponseDTO mapToSupplierAssociationResponseDTO(ProductSupplier ps) {
        return SupplierAssociationResponseDTO.builder()
                .id(ps.getId())
                .supplierId(ps.getSupplier().getId())
                .supplierName(ps.getSupplier().getName())
                .supplierSku(ps.getSupplierSku())
                .isPrimary(ps.getIsPrimary())
                .notes(ps.getNotes())
                .build();
    }

    private String generateSkuFromSubcategory(Subcategory subcategory, Long productId) {
        if (subcategory.getCategory() == null) {
            throw new BusinessException("La subcategoría '" + subcategory.getName() + "' no tiene una categoría asociada");
        }

        String categoryCode = normalizeToCode(subcategory.getCategory().getName());
        String subcategoryCode = normalizeToCode(subcategory.getName());
        String sequentialNumber = String.format("%05d", productId);
        String sku = String.format("%s-%s-%s", categoryCode, subcategoryCode, sequentialNumber);

        int counter = 0;
        String finalSku = sku;
        while (productRepository.existsBySkuAndActiveTrue(finalSku) && counter < 10) {
            long newId = productId + counter + 1;
            sequentialNumber = String.format("%05d", newId);
            finalSku = String.format("%s-%s-%s", categoryCode, subcategoryCode, sequentialNumber);
            counter++;
        }
        return finalSku;
    }

    private String normalizeToCode(String text) {
        if (text == null || text.trim().isEmpty()) return "XXX";
        String normalized = text.toUpperCase()
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace("Ñ", "N")
                .replace("Ç", "C").replace(" ", "").replace("-", "")
                .replace("_", "").replace("&", "Y").replace("/", "")
                .replace("\\", "").replace(".", "").replace(",", "");
        normalized = normalized.replaceAll("[^A-Z0-9]", "");
        if (normalized.length() < 3) {
            normalized = normalized + "X".repeat(3 - normalized.length());
        }
        return normalized.substring(0, 3);
    }
}