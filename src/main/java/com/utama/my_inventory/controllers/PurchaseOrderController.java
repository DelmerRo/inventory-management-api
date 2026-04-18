package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.request.DeliveryReceiptDTO;
import com.utama.my_inventory.dtos.request.PurchaseOrderDTO;
import com.utama.my_inventory.dtos.request.PurchaseOrderItemDTO;
import com.utama.my_inventory.dtos.response.OrderReconciliationDTO;
import com.utama.my_inventory.services.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos de Compra", description = "API para gestionar pedidos a proveedores, recepción de mercadería y conciliación de entregas")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    // ========== CRUD BÁSICO ==========

    @Operation(
            summary = "Crear nuevo pedido",
            description = "Crea un nuevo pedido de compra con sus items. El pedido se crea en estado PENDIENTE."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o número de pedido duplicado"),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    @PostMapping
    public ResponseEntity<PurchaseOrderDTO> createOrder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del pedido a crear",
                    required = true,
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "Ejemplo básico",
                                    value = """
                                            {
                                              "orderNumber": "PED-2024-0001",
                                              "supplierId": 1,
                                              "orderDate": "2024-12-18T10:00:00",
                                              "expectedDeliveryDate": "2024-12-25T10:00:00",
                                              "notes": "Urgente - Stock bajo",
                                              "items": [
                                                {
                                                  "productId": 1,
                                                  "quantity": 10,
                                                  "unitPrice": 1500.50
                                                },
                                                {
                                                  "productId": 2,
                                                  "quantity": 5,
                                                  "unitPrice": 2300.00
                                                }
                                              ]
                                            }
                                            """
                            )
                    })
            )
            @Valid @RequestBody PurchaseOrderDTO dto) {
        PurchaseOrderDTO created = purchaseOrderService.createOrder(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Obtener pedido por ID",
            description = "Retorna los detalles completos de un pedido específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderDTO> getOrderById(
            @Parameter(description = "ID del pedido", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getOrderById(id));
    }

    @Operation(
            summary = "Listar todos los pedidos",
            description = "Retorna una lista paginada de todos los pedidos"
    )
    @GetMapping
    public ResponseEntity<Page<PurchaseOrderDTO>> getAllOrders(
            @Parameter(description = "Configuración de paginación")
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(purchaseOrderService.getAllOrders(pageable));
    }

    @Operation(
            summary = "Actualizar pedido",
            description = "Actualiza campos de un pedido pendiente (fecha esperada, notas, etc.)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrderDTO> updateOrder(
            @Parameter(description = "ID del pedido", required = true) @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderDTO dto) {
        return ResponseEntity.ok(purchaseOrderService.updateOrder(id, dto));
    }

    @Operation(
            summary = "Eliminar pedido",
            description = "Elimina un pedido solo si está en estado PENDIENTE"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pedido eliminado"),
            @ApiResponse(responseCode = "400", description = "No se puede eliminar pedido no pendiente"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        purchaseOrderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Cancelar pedido",
            description = "Cambia el estado del pedido a CANCELADO"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido cancelado"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        purchaseOrderService.cancelOrder(id);
        return ResponseEntity.ok().build();
    }

    // ========== GESTIÓN DE ITEMS ==========

    @Operation(
            summary = "Agregar item al pedido",
            description = "Agrega un nuevo producto al pedido (solo si está pendiente)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item agregado"),
            @ApiResponse(responseCode = "400", description = "Pedido no está pendiente"),
            @ApiResponse(responseCode = "404", description = "Pedido o producto no encontrado")
    })
    @PostMapping("/{orderId}/items")
    public ResponseEntity<PurchaseOrderDTO> addItemToOrder(
            @Parameter(description = "ID del pedido", required = true) @PathVariable Long orderId,
            @Valid @RequestBody PurchaseOrderItemDTO itemDTO) {
        return ResponseEntity.ok(purchaseOrderService.addItemToOrder(orderId, itemDTO));
    }

    @Operation(
            summary = "Eliminar item del pedido",
            description = "Elimina un item del pedido (solo si está pendiente)"
    )
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<PurchaseOrderDTO> removeItemFromOrder(
            @Parameter(description = "ID del pedido", required = true) @PathVariable Long orderId,
            @Parameter(description = "ID del item", required = true) @PathVariable Long itemId) {
        return ResponseEntity.ok(purchaseOrderService.removeItemFromOrder(orderId, itemId));
    }

    @Operation(
            summary = "Actualizar cantidad de un item",
            description = "Modifica la cantidad pedida de un producto en el pedido"
    )
    @PutMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<PurchaseOrderDTO> updateItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Parameter(description = "Nueva cantidad", required = true, example = "15")
            @RequestParam Integer newQuantity) {
        return ResponseEntity.ok(purchaseOrderService.updateItemQuantity(orderId, itemId, newQuantity));
    }

    // ========== ⭐ RECEPCIÓN Y CONTRASTE (FUNCIONALIDAD PRINCIPAL) ==========

    @Operation(
            summary = "⭐ Procesar remito de entrega",
            description = """
                    **Esta es la funcionalidad más importante del sistema.**
                    
                    Permite cargar un remito de entrega y automáticamente:
                    - Contrasta lo recibido vs lo pedido
                    - Identifica entregas completas, parciales, faltantes y extras
                    - Actualiza las cantidades recibidas en el pedido
                    - Cambia el estado del pedido a PARCIAL o COMPLETADO según corresponda
                    
                    **El sistema te mostrará:**
                    - ✅ Productos entregados correctamente
                    - ⚠️ Productos con entrega parcial
                    - ❌ Productos faltantes (no entregados)
                    - ➕ Productos extras (recibidos pero no pedidos)
                    """,
            operationId = "processDeliveryReceipt"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Remito procesado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o pedido ya completado"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @PostMapping("/delivery/receive")
    public ResponseEntity<OrderReconciliationDTO> processDeliveryReceipt(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del remito de entrega",
                    required = true,
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "Ejemplo - Entrega perfecta",
                                    value = """
                                            {
                                              "purchaseOrderId": 1,
                                              "deliveryNumber": "REMITO-12345",
                                              "deliveryDate": "2024-12-20T14:30:00",
                                              "notes": "Todo en orden",
                                              "receivedItems": [
                                                {
                                                  "sku": "SKU-001",
                                                  "receivedQuantity": 10
                                                },
                                                {
                                                  "sku": "SKU-002",
                                                  "receivedQuantity": 5
                                                }
                                              ]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Ejemplo - Entrega parcial con extras",
                                    value = """
                                            {
                                              "purchaseOrderId": 1,
                                              "deliveryNumber": "REMITO-67890",
                                              "deliveryDate": "2024-12-20T14:30:00",
                                              "notes": "Faltaron algunos productos, vinieron otros extras",
                                              "receivedItems": [
                                                {
                                                  "sku": "SKU-001",
                                                  "receivedQuantity": 8
                                                },
                                                {
                                                  "sku": "SKU-002",
                                                  "receivedQuantity": 5
                                                },
                                                {
                                                  "sku": "SKU-999",
                                                  "receivedQuantity": 3
                                                }
                                              ]
                                            }
                                            """
                            )
                    })
            )
            @Valid @RequestBody DeliveryReceiptDTO receiptDTO) {
        OrderReconciliationDTO reconciliation = purchaseOrderService.processDeliveryReceipt(receiptDTO);
        return ResponseEntity.ok(reconciliation);
    }

    @Operation(
            summary = "Reconciliar pedido",
            description = "Muestra el estado actual de un pedido: qué se ha recibido y qué falta sin cargar un nuevo remito"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reconciliación completada"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @GetMapping("/{orderId}/reconcile")
    public ResponseEntity<OrderReconciliationDTO> reconcileOrder(
            @Parameter(description = "ID del pedido a reconciliar", required = true, example = "1")
            @PathVariable Long orderId) {
        return ResponseEntity.ok(purchaseOrderService.reconcileOrder(orderId));
    }

    @Operation(
            summary = "Confirmar pedido completado",
            description = "Marca un pedido como COMPLETADO manualmente (solo si todos los items fueron recibidos)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido completado"),
            @ApiResponse(responseCode = "400", description = "Hay items pendientes"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @PatchMapping("/{orderId}/complete")
    public ResponseEntity<PurchaseOrderDTO> confirmOrderCompletion(
            @Parameter(description = "ID del pedido", required = true) @PathVariable Long orderId) {
        return ResponseEntity.ok(purchaseOrderService.confirmOrderCompletion(orderId));
    }

    // ========== CONSULTAS ESPECÍFICAS ==========

    @Operation(
            summary = "Buscar pedidos por proveedor",
            description = "Retorna todos los pedidos de un proveedor específico"
    )
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<PurchaseOrderDTO>> getOrdersBySupplier(
            @Parameter(description = "ID del proveedor", required = true, example = "1")
            @PathVariable Long supplierId) {
        return ResponseEntity.ok(purchaseOrderService.getOrdersBySupplier(supplierId));
    }

    @Operation(
            summary = "Buscar pedidos por estado",
            description = "Retorna todos los pedidos con un estado específico (PENDIENTE, PARCIAL, COMPLETADO, CANCELADO)"
    )
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseOrderDTO>> getOrdersByStatus(
            @Parameter(description = "Estado del pedido", required = true,
                    example = "PENDIENTE",
                    schema = @Schema(allowableValues = {"PENDIENTE", "PARCIAL", "COMPLETADO", "CANCELADO"}))
            @PathVariable String status) {
        return ResponseEntity.ok(purchaseOrderService.getOrdersByStatus(status));
    }

    @Operation(
            summary = "Listar pedidos pendientes",
            description = "Retorna una lista paginada de pedidos en estado PENDIENTE"
    )
    @GetMapping("/pending")
    public ResponseEntity<Page<PurchaseOrderDTO>> getPendingOrders(
            @Parameter(description = "Configuración de paginación")
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(purchaseOrderService.getPendingOrders(pageable));
    }
}