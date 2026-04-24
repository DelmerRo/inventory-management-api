package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.ProductRequestDTO;
import com.utama.my_inventory.dtos.request.QuickProductRequestDTO;
import com.utama.my_inventory.dtos.request.SupplierAssociationDTO;
import com.utama.my_inventory.dtos.request.inventory.StockEntryRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockExitRequestDTO;
import com.utama.my_inventory.dtos.response.SupplierAssociationResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductDetailResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductSummaryResponseDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryMovementResponseDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaFileResponseDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaUploadResponseDTO;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Productos", description = "API para gestión de productos en el inventario")
public class ProductController {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final MultimediaService multimediaService;

    // ========== CRUD BÁSICO ==========

    @PostMapping
    @Operation(summary = "Crear nuevo producto", description = "Crea un producto con múltiples proveedores asociados")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o falta proveedor principal"),
            @ApiResponse(responseCode = "409", description = "SKU duplicado")
    })
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> createProduct(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del producto",
                    required = true,
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "Ejemplo completo",
                                    value = """
                                    {
                                        "name": "Laptop HP EliteBook",
                                        "description": "Laptop empresarial con 16GB RAM",
                                        "costPrice": 1200.50,
                                        "salePrice": 1500.00,
                                        "currentStock": 10,
                                        "subcategoryId": 1,
                                        "suppliers": [
                                            {
                                                "supplierId": 1,
                                                "supplierSku": "HP-ELITE-001",
                                                "isPrimary": true,
                                                "notes": "Proveedor oficial"
                                            }
                                        ],
                                        "weight": 1.5,
                                        "length": 35.50,
                                        "width": 25.00,
                                        "height": 2.50
                                    }
                                    """
                            )
                    })
            )
            @Valid @RequestBody ProductRequestDTO requestDTO) {

        ProductResponseDTO product = productService.createProduct(requestDTO);
        return ExtendedBaseResponse.created(product, "Producto creado exitosamente")
                .toResponseEntity();
    }

    @GetMapping
    @Operation(summary = "Listar todos los productos (resumen)")
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> getAllProducts() {
        List<ProductSummaryResponseDTO> products = productService.getAllProductsSummary();
        return ExtendedBaseResponse.ok(products, "Productos obtenidos correctamente")
                .toResponseEntity();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID (detalle esencial)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<ExtendedBaseResponse<ProductDetailResponseDTO>> getProductById(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id) {

        ProductDetailResponseDTO product = productService.getProductDetailById(id);
        return ExtendedBaseResponse.ok(product, "Producto encontrado")
                .toResponseEntity();
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Obtener producto por SKU (detalle esencial)")
    public ResponseEntity<ExtendedBaseResponse<ProductDetailResponseDTO>> getProductBySku(
            @Parameter(description = "SKU del producto", example = "LIV-DOR-00001")
            @PathVariable String sku) {

        ProductDetailResponseDTO product = productService.getProductDetailBySku(sku);
        return ExtendedBaseResponse.ok(product, "Producto encontrado")
                .toResponseEntity();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> updateProduct(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO requestDTO) {

        ProductResponseDTO product = productService.updateProduct(id, requestDTO);
        return ExtendedBaseResponse.ok(product, "Producto actualizado exitosamente")
                .toResponseEntity();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto eliminado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<ExtendedBaseResponse<Void>> deleteProduct(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id) {

        productService.deleteProduct(id);
        return ExtendedBaseResponse.<Void>ok(null, "Producto eliminado exitosamente")
                .toResponseEntity();
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Activar/desactivar producto")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> toggleProductStatus(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id) {

        ProductResponseDTO product = productService.toggleProductStatus(id);
        String message = product.active()
                ? "Producto activado exitosamente"
                : "Producto desactivado exitosamente";

        return ExtendedBaseResponse.ok(product, message)
                .toResponseEntity();
    }

    // ========== CONSULTAS POR RELACIONES (RESUMEN) ==========

    @GetMapping("/subcategory/{subcategoryId}")
    @Operation(summary = "Obtener productos por subcategoría (resumen)")
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> getProductsBySubcategory(
            @Parameter(description = "ID de la subcategoría", example = "1")
            @PathVariable Long subcategoryId) {

        List<ProductSummaryResponseDTO> products = productService.getProductsBySubcategorySummary(subcategoryId);
        return ExtendedBaseResponse.ok(products, "Productos obtenidos por subcategoría")
                .toResponseEntity();
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Obtener productos por proveedor (resumen)")
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> getProductsBySupplier(
            @Parameter(description = "ID del proveedor", example = "1")
            @PathVariable Long supplierId) {

        List<ProductSummaryResponseDTO> products = productService.getProductsBySupplierSummary(supplierId);
        return ExtendedBaseResponse.ok(products, "Productos obtenidos por proveedor")
                .toResponseEntity();
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Obtener productos con stock bajo (resumen)")
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> getLowStockProducts(
            @Parameter(description = "Umbral para considerar stock bajo", example = "10")
            @RequestParam(defaultValue = "10") int threshold) {

        List<ProductSummaryResponseDTO> products = productService.getLowStockProductsSummary(threshold);
        return ExtendedBaseResponse.ok(products, "Productos con stock bajo obtenidos")
                .toResponseEntity();
    }

    // ========== BÚSQUEDA (RESUMEN) ==========

    @GetMapping("/search")
    @Operation(summary = "Buscar productos con filtros (resumen)")
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> searchProducts(
            @Parameter(description = "Nombre del producto") @RequestParam(required = false) String name,
            @Parameter(description = "SKU del producto") @RequestParam(required = false) String sku,
            @Parameter(description = "Precio mínimo") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Precio máximo") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "ID de subcategoría") @RequestParam(required = false) Long subcategoryId,
            @Parameter(description = "ID de proveedor") @RequestParam(required = false) Long supplierId) {

        List<ProductSummaryResponseDTO> products = productService.searchProductsSummary(
                name, sku, minPrice, maxPrice, subcategoryId, supplierId);

        return ExtendedBaseResponse.ok(products, "Búsqueda completada")
                .toResponseEntity();
    }

    // ========== GESTIÓN DE PROVEEDORES ==========

    @GetMapping("/{id}/suppliers")
    @Operation(summary = "Obtener proveedores de un producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedores encontrados"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<ExtendedBaseResponse<List<SupplierAssociationResponseDTO>>> getProductSuppliers(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id) {

        List<SupplierAssociationResponseDTO> suppliers = productService.getProductSuppliers(id);
        return ExtendedBaseResponse.ok(suppliers, "Proveedores obtenidos correctamente")
                .toResponseEntity();
    }

    @PostMapping("/{id}/suppliers")
    @Operation(summary = "Agregar un proveedor a un producto existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedor agregado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Proveedor ya asociado o datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Producto o proveedor no encontrado")
    })
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> addSupplierToProduct(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody SupplierAssociationDTO supplierDTO) {

        ProductResponseDTO product = productService.addSupplierToProduct(id, supplierDTO);
        return ExtendedBaseResponse.ok(product, "Proveedor agregado exitosamente")
                .toResponseEntity();
    }

    @DeleteMapping("/{id}/suppliers/{supplierId}")
    @Operation(summary = "Eliminar un proveedor de un producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedor eliminado exitosamente"),
            @ApiResponse(responseCode = "400", description = "No se puede eliminar el único proveedor"),
            @ApiResponse(responseCode = "404", description = "Producto o relación no encontrada")
    })
    public ResponseEntity<ExtendedBaseResponse<Void>> removeSupplierFromProduct(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del proveedor", example = "1")
            @PathVariable Long supplierId) {

        productService.removeSupplierFromProduct(id, supplierId);
        return ExtendedBaseResponse.<Void>ok(null, "Proveedor eliminado exitosamente")
                .toResponseEntity();
    }

    @PatchMapping("/{id}/suppliers/{supplierId}/sku")
    @Operation(summary = "Actualizar el SKU de un proveedor para un producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SKU actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto o relación no encontrada")
    })
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> updateSupplierSku(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del proveedor", example = "1")
            @PathVariable Long supplierId,
            @Parameter(description = "Nuevo SKU del proveedor", example = "HP-ELITE-002")
            @RequestParam String supplierSku) {

        ProductResponseDTO product = productService.updateSupplierSku(id, supplierId, supplierSku);
        return ExtendedBaseResponse.ok(product, "SKU de proveedor actualizado exitosamente")
                .toResponseEntity();
    }

    // ========== GESTIÓN DE STOCK ==========

    @PostMapping("/add-stock")
    @Operation(summary = "Agregar stock a producto")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> addStock(
            @Valid @RequestBody StockEntryRequestDTO request) {

        ProductResponseDTO product = productService.addStock(
                request.productId(), request.quantity(), request.reason(), request.user());
        return ExtendedBaseResponse.ok(product, "Stock agregado exitosamente")
                .toResponseEntity();
    }

    @PostMapping("/remove-stock")
    @Operation(summary = "Remover stock de producto")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> removeStock(
            @Valid @RequestBody StockExitRequestDTO request) {

        ProductResponseDTO product = productService.removeStock(
                request.productId(),
                request.quantity(),
                request.reason(),
                request.user()
        );

        return ExtendedBaseResponse.ok(product, "Stock removido exitosamente")
                .toResponseEntity();
    }

    // ========== ESTADÍSTICAS ==========

    @GetMapping("/statistics")
    @Operation(summary = "Obtener estadísticas de productos")
    public ResponseEntity<ExtendedBaseResponse<Map<String, Object>>> getStatistics() {

        Long totalProducts = productService.getTotalProductCount();
        Long totalStock = productService.getTotalStock();
        BigDecimal totalValue = productService.getTotalInventoryValue();

        Map<String, Object> stats = Map.of(
                "totalProducts", totalProducts,
                "totalStock", totalStock,
                "totalInventoryValue", totalValue
        );

        return ExtendedBaseResponse.ok(stats, "Estadísticas obtenidas")
                .toResponseEntity();
    }

    // ========== HISTORIAL DE INVENTARIO ==========

    @GetMapping("/{id}/inventory")
    @Operation(summary = "Obtener historial de inventario de un producto")
    public ResponseEntity<ExtendedBaseResponse<List<InventoryMovementResponseDTO>>> getProductInventoryHistory(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id) {

        List<InventoryMovementResponseDTO> history = inventoryService.getProductHistory(id);
        return ExtendedBaseResponse.ok(history, "Historial de inventario obtenido")
                .toResponseEntity();
    }

    // ========== MULTIMEDIA (IMÁGENES) ==========

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir imagen al producto")
    public ResponseEntity<ExtendedBaseResponse<MultimediaUploadResponseDTO>> uploadProductImage(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        MultimediaUploadResponseDTO result = multimediaService.uploadFile(id, file, "IMAGE");
        return ExtendedBaseResponse.ok(result, "Imagen subida exitosamente")
                .toResponseEntity();
    }

    @GetMapping("/{id}/images")
    @Operation(summary = "Listar imágenes del producto")
    public ResponseEntity<ExtendedBaseResponse<List<MultimediaFileResponseDTO>>> getProductImages(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id) {

        List<MultimediaFileResponseDTO> images = multimediaService.getProductFilesByType(id, "IMAGE");
        return ExtendedBaseResponse.ok(images, "Imágenes obtenidas correctamente")
                .toResponseEntity();
    }

    @Operation(
            summary = "Crear producto rápido desde pedido de compra",
            description = "Crea un producto con datos mínimos (nombre, SKU proveedor, subcategoría)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "✅ Producto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "❌ Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "❌ SKU de proveedor ya existe")
    })
    @PostMapping("/quick")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> createQuickProduct(
            @Valid @RequestBody QuickProductRequestDTO requestDTO) {

        ProductResponseDTO product = productService.createQuickProduct(requestDTO);
        return ExtendedBaseResponse.created(product, "Producto creado exitosamente")
                .toResponseEntity();
    }

    @Operation(summary = "Obtener producto por SKU de proveedor")
    @GetMapping("/by-supplier-sku/{supplierSku}")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> getProductBySupplierSku(
            @PathVariable String supplierSku) {
        ProductResponseDTO product = productService.getProductBySupplierSku(supplierSku);
        return ExtendedBaseResponse.ok(product, "Producto encontrado")
                .toResponseEntity();
    }


}