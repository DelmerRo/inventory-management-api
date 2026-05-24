package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.ProductRequestDTO;
import com.utama.my_inventory.dtos.request.QuickProductRequestDTO;
import com.utama.my_inventory.dtos.request.SupplierAssociationDTO;
import com.utama.my_inventory.dtos.request.inventory.StockEntryRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockExitRequestDTO;
import com.utama.my_inventory.dtos.response.SupplierAssociationResponseDTO;
import com.utama.my_inventory.dtos.response.product.PagedProductResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductDetailResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductSummaryResponseDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryMovementResponseDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaFileResponseDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaUploadResponseDTO;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.repositories.ProductRepository;
import com.utama.my_inventory.services.InventoryService;
import com.utama.my_inventory.services.MultimediaService;
import com.utama.my_inventory.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Productos", description = "API para gestión de productos en el inventario")
public class ProductController {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final MultimediaService multimediaService;
    private final ProductRepository productRepository; // Solo para debug

    // Helper para crear Pageable con validación de campo de ordenamiento
    private Pageable createPageable(int page, int size, String sortField, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String validField = switch (sortField) {
            case "name", "salePrice", "currentStock", "createdAt", "id" -> sortField;
            default -> "createdAt";
        };
        return PageRequest.of(page, size, Sort.by(direction, validField));
    }

    // ========== CRUD BÁSICO ==========

    @PostMapping
    @Operation(summary = "Crear nuevo producto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o falta proveedor principal"),
            @ApiResponse(responseCode = "409", description = "SKU duplicado")
    })
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> createProduct(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del producto",
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "Ejemplo completo", value = """
                            {
                                "name": "Laptop HP EliteBook",
                                "description": "Laptop empresarial con 16GB RAM",
                                "costPrice": 1200.50,
                                "salePrice": 1500.00,
                                "currentStock": 10,
                                "subcategoryId": 1,
                                "suppliers": [{"supplierId": 1, "supplierSku": "HP-ELITE-001", "isPrimary": true, "notes": "Proveedor oficial"}],
                                "weight": 1.5, "length": 35.50, "width": 25.00, "height": 2.50
                            }
                            """)))
            @Valid @RequestBody ProductRequestDTO requestDTO) {
        return ExtendedBaseResponse.created(productService.createProduct(requestDTO), "Producto creado exitosamente")
                .toResponseEntity();
    }

    @GetMapping
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> getAllProducts() {
        return ExtendedBaseResponse.ok(productService.getAllProductsSummary(), "Productos obtenidos correctamente")
                .toResponseEntity();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExtendedBaseResponse<ProductDetailResponseDTO>> getProductById(@PathVariable Long id) {
        return ExtendedBaseResponse.ok(productService.getProductDetailById(id), "Producto encontrado")
                .toResponseEntity();
    }

    @GetMapping("/paged")
    public ResponseEntity<ExtendedBaseResponse<PagedProductResponseDTO>> getProductsPaged(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String supplierSku,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Integer maxStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Pageable pageable = createPageable(page, size, sortField, sortDirection);
        PagedProductResponseDTO pagedResponse = productService.getProductsPaged(
                name, sku, supplierSku, minPrice, maxPrice, subcategoryId, categoryId,
                supplierId, active, minStock, maxStock, pageable);
        return ExtendedBaseResponse.ok(pagedResponse, "Búsqueda paginada completada con éxito")
                .toResponseEntity();
    }

    @GetMapping("/search-general")
    public ResponseEntity<ExtendedBaseResponse<Page<ProductSummaryResponseDTO>>> searchProductsGeneral(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Pageable pageable = createPageable(page, size, sortField, sortDirection);
        Page<ProductSummaryResponseDTO> productPage = productService.searchProductsGeneral(
                query, minPrice, maxPrice, subcategoryId, supplierId, active, pageable);
        return ExtendedBaseResponse.ok(productPage, "Búsqueda completada").toResponseEntity();
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ExtendedBaseResponse<ProductDetailResponseDTO>> getProductBySku(@PathVariable String sku) {
        return ExtendedBaseResponse.ok(productService.getProductDetailBySku(sku), "Producto encontrado")
                .toResponseEntity();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> updateProduct(
            @PathVariable Long id, @Valid @RequestBody ProductRequestDTO requestDTO) {
        return ExtendedBaseResponse.ok(productService.updateProduct(id, requestDTO), "Producto actualizado exitosamente")
                .toResponseEntity();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ExtendedBaseResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ExtendedBaseResponse.<Void>ok(null, "Producto eliminado exitosamente").toResponseEntity();
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> toggleProductStatus(@PathVariable Long id) {
        ProductResponseDTO product = productService.toggleProductStatus(id);
        String message = product.active() ? "Producto activado exitosamente" : "Producto desactivado exitosamente";
        return ExtendedBaseResponse.ok(product, message).toResponseEntity();
    }

    // ========== CONSULTAS POR RELACIONES ==========

    @GetMapping("/subcategory/{subcategoryId}")
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> getProductsBySubcategory(@PathVariable Long subcategoryId) {
        return ExtendedBaseResponse.ok(productService.getProductsBySubcategorySummary(subcategoryId), "Productos obtenidos por subcategoría")
                .toResponseEntity();
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> getProductsBySupplier(@PathVariable Long supplierId) {
        return ExtendedBaseResponse.ok(productService.getProductsBySupplierSummary(supplierId), "Productos obtenidos por proveedor")
                .toResponseEntity();
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold) {
        return ExtendedBaseResponse.ok(productService.getLowStockProductsSummary(threshold), "Productos con stock bajo obtenidos")
                .toResponseEntity();
    }

    // ========== BÚSQUEDA (resumen) ==========

    @GetMapping("/search")
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String supplierSku,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        List<ProductSummaryResponseDTO> products = productService.searchProductsSummary(
                name, sku, supplierSku, minPrice, maxPrice, subcategoryId, supplierId, dateFrom, dateTo);
        return ExtendedBaseResponse.ok(products, "Búsqueda completada").toResponseEntity();
    }

    @GetMapping("/by-supplier-sku/{supplierSku}")
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> getProductsBySupplierSku(@PathVariable String supplierSku) {
        return ExtendedBaseResponse.ok(productService.findByProductSupplierSku(supplierSku), "Productos encontrados por SKU de proveedor")
                .toResponseEntity();
    }

    // ========== GESTIÓN DE PROVEEDORES ==========

    @GetMapping("/{id}/suppliers")
    public ResponseEntity<ExtendedBaseResponse<List<SupplierAssociationResponseDTO>>> getProductSuppliers(@PathVariable Long id) {
        return ExtendedBaseResponse.ok(productService.getProductSuppliers(id), "Proveedores obtenidos correctamente")
                .toResponseEntity();
    }

    @PostMapping("/{id}/suppliers")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> addSupplierToProduct(
            @PathVariable Long id, @Valid @RequestBody SupplierAssociationDTO supplierDTO) {
        return ExtendedBaseResponse.ok(productService.addSupplierToProduct(id, supplierDTO), "Proveedor agregado exitosamente")
                .toResponseEntity();
    }

    @DeleteMapping("/{id}/suppliers/{supplierId}")
    public ResponseEntity<ExtendedBaseResponse<Void>> removeSupplierFromProduct(@PathVariable Long id, @PathVariable Long supplierId) {
        productService.removeSupplierFromProduct(id, supplierId);
        return ExtendedBaseResponse.<Void>ok(null, "Proveedor eliminado exitosamente").toResponseEntity();
    }

    @PatchMapping("/{id}/suppliers/{supplierId}/sku")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> updateSupplierSku(
            @PathVariable Long id, @PathVariable Long supplierId, @RequestParam String supplierSku) {
        return ExtendedBaseResponse.ok(productService.updateSupplierSku(id, supplierId, supplierSku), "SKU de proveedor actualizado exitosamente")
                .toResponseEntity();
    }

    // ========== GESTIÓN DE STOCK ==========

    @PostMapping("/add-stock")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> addStock(@Valid @RequestBody StockEntryRequestDTO request) {
        return ExtendedBaseResponse.ok(
                productService.addStock(request.productId(), request.quantity(), request.reason(), request.user()),
                "Stock agregado exitosamente").toResponseEntity();
    }

    @PostMapping("/remove-stock")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> removeStock(@Valid @RequestBody StockExitRequestDTO request) {
        return ExtendedBaseResponse.ok(
                productService.removeStock(request.productId(), request.quantity(), request.reason(), request.user()),
                "Stock removido exitosamente").toResponseEntity();
    }

    // ========== ESTADÍSTICAS ==========

    @GetMapping("/statistics")
    public ResponseEntity<ExtendedBaseResponse<Map<String, Object>>> getStatistics() {
        Map<String, Object> stats = Map.of(
                "totalProducts", productService.getTotalProductCount(),
                "totalStock", productService.getTotalStock(),
                "totalInventoryValue", productService.getTotalInventoryValue()
        );
        return ExtendedBaseResponse.ok(stats, "Estadísticas obtenidas").toResponseEntity();
    }

    // ========== HISTORIAL DE INVENTARIO ==========

    @GetMapping("/{id}/inventory")
    public ResponseEntity<ExtendedBaseResponse<List<InventoryMovementResponseDTO>>> getProductInventoryHistory(@PathVariable Long id) {
        return ExtendedBaseResponse.ok(inventoryService.getProductHistory(id), "Historial de inventario obtenido")
                .toResponseEntity();
    }

    // ========== MULTIMEDIA ==========

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExtendedBaseResponse<MultimediaUploadResponseDTO>> uploadProductImage(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ExtendedBaseResponse.ok(multimediaService.uploadFile(id, file, "IMAGE"), "Imagen subida exitosamente")
                .toResponseEntity();
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<ExtendedBaseResponse<List<MultimediaFileResponseDTO>>> getProductImages(@PathVariable Long id) {
        return ExtendedBaseResponse.ok(multimediaService.getProductFilesByType(id, "IMAGE"), "Imágenes obtenidas correctamente")
                .toResponseEntity();
    }

    @PostMapping("/quick")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> createQuickProduct(@Valid @RequestBody QuickProductRequestDTO requestDTO) {
        return ExtendedBaseResponse.created(productService.createQuickProduct(requestDTO), "Producto creado exitosamente")
                .toResponseEntity();
    }

    // ========== DEBUG (solo para desarrollo) ==========
    @GetMapping("/debug/{id}")
    public ResponseEntity<?> debugProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();

        Map<String, Object> debug = new HashMap<>();
        debug.put("productId", product.getId());
        debug.put("productName", product.getName());
        debug.put("productSuppliers", product.getProductSuppliers().stream()
                .map(ps -> Map.of(
                        "id", ps.getId(),
                        "supplierId", ps.getSupplier().getId(),
                        "supplierName", ps.getSupplier().getName(),
                        "supplierSku", ps.getSupplierSku(),
                        "isPrimary", ps.getIsPrimary()
                ))
                .collect(Collectors.toList()));
        return ResponseEntity.ok(debug);
    }
}