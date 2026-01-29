package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.ProductRequestDTO;
import com.utama.my_inventory.dtos.response.ProductResponseDTO;
import com.utama.my_inventory.dtos.response.ProductSummaryResponseDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryMovementResponseDTO;
import com.utama.my_inventory.services.InventoryService;
import com.utama.my_inventory.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    @Operation(summary = "Crear nuevo producto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente",
                    content = @Content(schema = @Schema(implementation = ExtendedBaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "SKU duplicado")
    })
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> createProduct(
            @Valid @RequestBody ProductRequestDTO requestDTO) {

        ProductResponseDTO product = productService.createProduct(requestDTO);
        return ExtendedBaseResponse.created(product, "Producto creado exitosamente")
                .toResponseEntity();
    }

    @GetMapping
    @Operation(summary = "Listar todos los productos")
    public ResponseEntity<ExtendedBaseResponse<List<ProductResponseDTO>>> getAllProducts() {

        List<ProductResponseDTO> products = productService.getAllProducts();
        return ExtendedBaseResponse.ok(products, "Productos obtenidos correctamente")
                .toResponseEntity();
    }

    @GetMapping("/summary")
    @Operation(summary = "Listar productos (resumen)")
    public ResponseEntity<ExtendedBaseResponse<List<ProductSummaryResponseDTO>>> getAllProductsSummary() {

        List<ProductSummaryResponseDTO> products = productService.getAllProductsSummary();
        return ExtendedBaseResponse.ok(products, "Resumen de productos obtenido")
                .toResponseEntity();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> getProductById(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id) {

        ProductResponseDTO product = productService.getProductById(id);
        return ExtendedBaseResponse.ok(product, "Producto encontrado")
                .toResponseEntity();
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Obtener producto por SKU")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> getProductBySku(
            @Parameter(description = "SKU del producto", example = "PROD-001")
            @PathVariable String sku) {

        ProductResponseDTO product = productService.getProductBySku(sku);
        return ExtendedBaseResponse.ok(product, "Producto encontrado")
                .toResponseEntity();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "SKU duplicado o precios inválidos")
    })
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> updateProduct(
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
            @PathVariable Long id) {

        productService.deleteProduct(id);
        return ExtendedBaseResponse.<Void>ok(null, "Producto eliminado exitosamente")
                .toResponseEntity();
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Activar/desactivar producto")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> toggleProductStatus(
            @PathVariable Long id) {

        ProductResponseDTO product = productService.toggleProductStatus(id);
        String message = product.active()
                ? "Producto activado exitosamente"
                : "Producto desactivado exitosamente";

        return ExtendedBaseResponse.ok(product, message)
                .toResponseEntity();
    }

    @GetMapping("/subcategory/{subcategoryId}")
    @Operation(summary = "Obtener productos por subcategoría")
    public ResponseEntity<ExtendedBaseResponse<List<ProductResponseDTO>>> getProductsBySubcategory(
            @PathVariable Long subcategoryId) {

        List<ProductResponseDTO> products = productService.getProductsBySubcategory(subcategoryId);
        return ExtendedBaseResponse.ok(products, "Productos obtenidos por subcategoría")
                .toResponseEntity();
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Obtener productos por proveedor")
    public ResponseEntity<ExtendedBaseResponse<List<ProductResponseDTO>>> getProductsBySupplier(
            @PathVariable Long supplierId) {

        List<ProductResponseDTO> products = productService.getProductsBySupplier(supplierId);
        return ExtendedBaseResponse.ok(products, "Productos obtenidos por proveedor")
                .toResponseEntity();
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Obtener productos con stock bajo")
    public ResponseEntity<ExtendedBaseResponse<List<ProductResponseDTO>>> getLowStockProducts(
            @Parameter(description = "Umbral para considerar stock bajo", example = "10")
            @RequestParam(defaultValue = "10") int threshold) {

        List<ProductResponseDTO> products = productService.getLowStockProducts(threshold);
        return ExtendedBaseResponse.ok(products, "Productos con stock bajo obtenidos")
                .toResponseEntity();
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar productos con filtros")
    public ResponseEntity<ExtendedBaseResponse<List<ProductResponseDTO>>> searchProducts(
            @Parameter(description = "Nombre del producto") @RequestParam(required = false) String name,
            @Parameter(description = "SKU del producto") @RequestParam(required = false) String sku,
            @Parameter(description = "Precio mínimo") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Precio máximo") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "ID de subcategoría") @RequestParam(required = false) Long subcategoryId,
            @Parameter(description = "ID de proveedor") @RequestParam(required = false) Long supplierId) {

        List<ProductResponseDTO> products = productService.searchProducts(
                name, sku, minPrice, maxPrice, subcategoryId, supplierId);

        return ExtendedBaseResponse.ok(products, "Búsqueda completada")
                .toResponseEntity();
    }

    @PostMapping("/{id}/add-stock")
    @Operation(summary = "Agregar stock a producto")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> addStock(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        int quantity = (Integer) request.get("quantity");
        String reason = (String) request.get("reason");
        String user = (String) request.get("user");

        ProductResponseDTO product = productService.addStock(id, quantity, reason, user);
        return ExtendedBaseResponse.ok(product, "Stock agregado exitosamente")
                .toResponseEntity();
    }

    @PostMapping("/{id}/remove-stock")
    @Operation(summary = "Remover stock de producto")
    public ResponseEntity<ExtendedBaseResponse<ProductResponseDTO>> removeStock(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        int quantity = (Integer) request.get("quantity");
        String reason = (String) request.get("reason");
        String user = (String) request.get("user");

        ProductResponseDTO product = productService.removeStock(id, quantity, reason, user);
        return ExtendedBaseResponse.ok(product, "Stock removido exitosamente")
                .toResponseEntity();
    }

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

    @GetMapping("/{id}/inventory")
    @Operation(summary = "Obtener historial de inventario de un producto")
    public ResponseEntity<ExtendedBaseResponse<List<InventoryMovementResponseDTO>>> getProductInventoryHistory(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id) {

        List<InventoryMovementResponseDTO> history = inventoryService.getProductHistory(id);
        return ExtendedBaseResponse.ok(history, "Historial de inventario obtenido")
                .toResponseEntity();
    }
}