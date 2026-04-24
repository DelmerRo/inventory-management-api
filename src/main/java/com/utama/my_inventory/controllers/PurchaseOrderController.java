package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.DeliveryReceiptDTO;
import com.utama.my_inventory.dtos.request.PurchaseOrderRequestDTO;
import com.utama.my_inventory.dtos.request.PurchaseOrderItemRequestDTO;
import com.utama.my_inventory.dtos.response.OrderReconciliationDTO;
import com.utama.my_inventory.dtos.response.PurchaseOrderResponseDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "📦 Pedidos de Compra", description = """
    API para la gestión completa de pedidos de compra a proveedores.
    
    **Flujo típico de uso:**
    1. Crear un pedido con los productos solicitados
    2. Recibir la mercadería mediante un remito
    3. El sistema concilia automáticamente lo recibido vs lo pedido
    4. Se actualiza el inventario y el estado del pedido
    
    **Estados del pedido:**
    - `PENDIENTE`: Pedido creado, pendiente de recepción
    - `PARCIAL`: Recepción parcial completada
    - `COMPLETADO`: Todos los productos recibidos
    - `CANCELADO`: Pedido cancelado
    """)
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    // ========== CRUD BÁSICO ==========

    @Operation(
            summary = "📝 Crear nuevo pedido de compra",
            description = """
            Crea un nuevo pedido de compra con sus items.
            
            **Reglas de negocio:**
            - El número de pedido se genera automáticamente con formato `PO-YYYY-XXXXXX`
            - El pedido se crea en estado `PENDIENTE`
            - El proveedor debe existir en el sistema
            - Cada item requiere SKU del proveedor, cantidad y precio unitario
            """,
            operationId = "createOrder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "✅ Pedido creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "❌ Datos inválidos o faltantes"),
            @ApiResponse(responseCode = "404", description = "❌ Proveedor no encontrado")
    })
    @PostMapping
    public ResponseEntity<ExtendedBaseResponse<PurchaseOrderResponseDTO>> createOrder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del pedido a crear",
                    required = true,
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "Ejemplo básico",
                                    summary = "Pedido con un solo producto",
                                    value = """
                        {
                          "supplierId": 1,
                          "orderDate": "2024-12-18T10:00:00",
                          "expectedDeliveryDate": "2024-12-25T10:00:00",
                          "notes": "Pedido urgente - Stock bajo",
                          "items": [
                            {
                              "supplierSku": "PROV-123",
                              "productName": "Alfombra Gris 120x160cm",
                              "quantity": 10,
                              "unitPrice": 1250.50
                            }
                          ]
                        }
                        """
                            ),
                            @ExampleObject(
                                    name = "Ejemplo múltiple",
                                    summary = "Pedido con varios productos",
                                    value = """
                        {
                          "supplierId": 1,
                          "orderDate": "2024-12-18T10:00:00",
                          "expectedDeliveryDate": "2024-12-25T10:00:00",
                          "notes": "Reposición de temporada",
                          "items": [
                            {
                              "supplierSku": "PROV-123",
                              "productName": "Alfombra Gris 120x160cm",
                              "quantity": 10,
                              "unitPrice": 1250.50
                            },
                            {
                              "supplierSku": "PROV-456",
                              "productName": "Almohadón Terciopelo Azul",
                              "quantity": 20,
                              "unitPrice": 850.00
                            }
                          ]
                        }
                        """
                            )
                    })
            )
            @Valid @RequestBody PurchaseOrderRequestDTO dto) {
        PurchaseOrderResponseDTO created = purchaseOrderService.createOrder(dto);
        return ExtendedBaseResponse.created(created, "Pedido creado exitosamente")
                .toResponseEntity();
    }

    @Operation(
            summary = "🔍 Obtener pedido por ID",
            description = "Retorna los detalles completos de un pedido específico, incluyendo todos sus items y el estado de recepción.",
            operationId = "getOrderById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "❌ Pedido no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ExtendedBaseResponse<PurchaseOrderResponseDTO>> getOrderById(
            @Parameter(description = "ID del pedido", required = true, example = "1")
            @PathVariable Long id) {
        PurchaseOrderResponseDTO order = purchaseOrderService.getOrderById(id);
        return ExtendedBaseResponse.ok(order, "Pedido encontrado")
                .toResponseEntity();
    }

    @Operation(
            summary = "📋 Listar todos los pedidos",
            description = "Retorna una lista completa de todos los pedidos ordenados por fecha descendente.",
            operationId = "getAllOrders"
    )
    @ApiResponse(responseCode = "200", description = "✅ Lista de pedidos obtenida exitosamente")
    @GetMapping
    public ResponseEntity<ExtendedBaseResponse<List<PurchaseOrderResponseDTO>>> getAllOrders() {
        List<PurchaseOrderResponseDTO> orders = purchaseOrderService.getAllOrders();
        return ExtendedBaseResponse.ok(orders, "Pedidos obtenidos correctamente")
                .toResponseEntity();
    }

    @Operation(
            summary = "✏️ Actualizar pedido",
            description = """
            Actualiza campos de un pedido existente.
            
            **Campos actualizables:**
            - Fecha estimada de entrega (`expectedDeliveryDate`)
            - Notas del pedido (`notes`)
            
            **Restricciones:**
            - Solo se pueden actualizar pedidos en estado `PENDIENTE`
            - No se pueden modificar los items ni el proveedor
            """,
            operationId = "updateOrder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Pedido actualizado"),
            @ApiResponse(responseCode = "400", description = "❌ Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "❌ Pedido no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ExtendedBaseResponse<PurchaseOrderResponseDTO>> updateOrder(
            @Parameter(description = "ID del pedido a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos a actualizar del pedido",
                    required = true,
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "Actualizar fecha de entrega",
                                    summary = "Cambiar fecha esperada",
                                    value = """
                        {
                          "supplierId": 1,
                          "orderDate": "2024-12-18T10:00:00",
                          "expectedDeliveryDate": "2024-12-28T10:00:00",
                          "notes": "Pedido urgente - Entregar antes de fin de año",
                          "items": []
                        }
                        """
                            )
                    })
            )
            @Valid @RequestBody PurchaseOrderRequestDTO dto) {
        PurchaseOrderResponseDTO updated = purchaseOrderService.updateOrder(id, dto);
        return ExtendedBaseResponse.ok(updated, "Pedido actualizado exitosamente")
                .toResponseEntity();
    }

    @Operation(
            summary = "🗑️ Eliminar pedido",
            description = """
            Elimina un pedido de forma permanente.
            
            **Restricciones:**
            - Solo se pueden eliminar pedidos en estado `PENDIENTE`
            - Los pedidos con recepciones parciales o completadas NO pueden eliminarse
            """,
            operationId = "deleteOrder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Pedido eliminado"),
            @ApiResponse(responseCode = "400", description = "❌ No se puede eliminar pedido no pendiente"),
            @ApiResponse(responseCode = "404", description = "❌ Pedido no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ExtendedBaseResponse<Void>> deleteOrder(
            @Parameter(description = "ID del pedido a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        purchaseOrderService.deleteOrder(id);
        return ExtendedBaseResponse.<Void>ok(null, "Pedido eliminado exitosamente")
                .toResponseEntity();
    }

    @Operation(
            summary = "🚫 Cancelar pedido",
            description = """
            Cancela un pedido cambiando su estado a `CANCELADO`.
            
            **Efectos:**
            - El pedido ya no puede ser recibido
            - No se modifica el inventario
            - Se mantiene el registro histórico
            """,
            operationId = "cancelOrder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Pedido cancelado"),
            @ApiResponse(responseCode = "404", description = "❌ Pedido no encontrado")
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ExtendedBaseResponse<Void>> cancelOrder(
            @Parameter(description = "ID del pedido a cancelar", required = true, example = "1")
            @PathVariable Long id) {
        purchaseOrderService.cancelOrder(id);
        return ExtendedBaseResponse.<Void>ok(null, "Pedido cancelado exitosamente")
                .toResponseEntity();
    }

    // ========== GESTIÓN DE ITEMS ==========

    @Operation(
            summary = "➕ Agregar item al pedido",
            description = """
            Agrega un nuevo producto al pedido.
            
            **Comportamiento especial:**
            - Si el SKU del proveedor ya existe en el pedido, se suma la cantidad
            - Si el precio es diferente al existente, se rechaza la operación
            - Solo permite agregar items a pedidos `PENDIENTES`
            """,
            operationId = "addItemToOrder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Item agregado"),
            @ApiResponse(responseCode = "400", description = "❌ Pedido no está pendiente o precio inconsistente"),
            @ApiResponse(responseCode = "404", description = "❌ Pedido no encontrado")
    })
    @PostMapping("/{orderId}/items")
    public ResponseEntity<ExtendedBaseResponse<PurchaseOrderResponseDTO>> addItemToOrder(
            @Parameter(description = "ID del pedido", required = true, example = "1")
            @PathVariable Long orderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del producto a agregar",
                    required = true,
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "Agregar producto nuevo",
                                    value = """
                        {
                          "supplierSku": "PROV-789",
                          "productName": "Jarrón Cerámica Decorativo",
                          "quantity": 5,
                          "unitPrice": 450.00
                        }
                        """
                            ),
                            @ExampleObject(
                                    name = "Aumentar cantidad de producto existente",
                                    value = """
                        {
                          "supplierSku": "PROV-123",
                          "productName": "Alfombra Gris 120x160cm",
                          "quantity": 5,
                          "unitPrice": 1250.50
                        }
                        """
                            )
                    })
            )
            @Valid @RequestBody PurchaseOrderItemRequestDTO itemDTO) {
        PurchaseOrderResponseDTO updated = purchaseOrderService.addItemToOrder(orderId, itemDTO);
        return ExtendedBaseResponse.ok(updated, "Item agregado exitosamente")
                .toResponseEntity();
    }

    @Operation(
            summary = "➖ Eliminar item del pedido",
            description = "Elimina un producto del pedido. Solo permite eliminar items de pedidos `PENDIENTES`.",
            operationId = "removeItemFromOrder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Item eliminado"),
            @ApiResponse(responseCode = "400", description = "❌ Pedido no está pendiente"),
            @ApiResponse(responseCode = "404", description = "❌ Pedido o item no encontrado")
    })
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<ExtendedBaseResponse<PurchaseOrderResponseDTO>> removeItemFromOrder(
            @Parameter(description = "ID del pedido", required = true, example = "1")
            @PathVariable Long orderId,
            @Parameter(description = "ID del item a eliminar", required = true, example = "1")
            @PathVariable Long itemId) {
        PurchaseOrderResponseDTO updated = purchaseOrderService.removeItemFromOrder(orderId, itemId);
        return ExtendedBaseResponse.ok(updated, "Item eliminado exitosamente")
                .toResponseEntity();
    }

    @Operation(
            summary = "🔢 Actualizar cantidad de un item",
            description = "Modifica la cantidad pedida de un producto en el pedido. Solo permite modificar pedidos `PENDIENTES`.",
            operationId = "updateItemQuantity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Cantidad actualizada"),
            @ApiResponse(responseCode = "400", description = "❌ Pedido no está pendiente"),
            @ApiResponse(responseCode = "404", description = "❌ Pedido o item no encontrado")
    })
    @PutMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<ExtendedBaseResponse<PurchaseOrderResponseDTO>> updateItemQuantity(
            @Parameter(description = "ID del pedido", required = true, example = "1")
            @PathVariable Long orderId,
            @Parameter(description = "ID del item", required = true, example = "1")
            @PathVariable Long itemId,
            @Parameter(description = "Nueva cantidad (mínimo 1)", required = true, example = "15")
            @RequestParam Integer newQuantity) {
        PurchaseOrderResponseDTO updated = purchaseOrderService.updateItemQuantity(orderId, itemId, newQuantity);
        return ExtendedBaseResponse.ok(updated, "Cantidad actualizada exitosamente")
                .toResponseEntity();
    }

    // ========== ⭐ RECEPCIÓN Y CONTRASTE ==========

    @Operation(
            summary = "⭐ PROCESAR REMITO DE ENTREGA",
            description = """
            **ESTA ES LA FUNCIONALIDAD MÁS IMPORTANTE DEL SISTEMA**
            
            Permite cargar un remito de entrega y el sistema automáticamente:
            
            **1. CONTRASTE DE MERCaderÍA:**
            - ✅ **Items coincidentes:** Productos entregados correctamente según lo pedido
            - ⚠️ **Items parciales:** Productos entregados en menor cantidad a la pedida
            - ❌ **Items faltantes:** Productos no entregados
            - ➕ **Items extras:** Productos recibidos que no estaban en el pedido
            
            **2. ACCIONES AUTOMÁTICAS:**
            - Actualiza las cantidades recibidas en el pedido
            - Crea productos nuevos si el SKU del proveedor no existe
            - Actualiza el stock del inventario
            - Cambia el estado del pedido a `PARCIAL` o `COMPLETADO`
            
            **3. RESULTADO:**
            - Devuelve un informe detallado de la reconciliación
            - Recomienda acciones según las discrepancias encontradas
            """,
            operationId = "processDeliveryReceipt"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Remito procesado exitosamente"),
            @ApiResponse(responseCode = "400", description = "❌ Datos inválidos o pedido ya completado"),
            @ApiResponse(responseCode = "404", description = "❌ Pedido no encontrado")
    })
    @PostMapping("/delivery/receive")
    public ResponseEntity<ExtendedBaseResponse<OrderReconciliationDTO>> processDeliveryReceipt(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del remito de entrega",
                    required = true,
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "📦 Entrega perfecta",
                                    summary = "Todos los productos entregados correctamente",
                                    value = """
                        {
                          "purchaseOrderId": 1,
                          "deliveryDate": "2024-12-20T10:00:00",
                          "notes": "REMITO-002//Entrega completa en tiempo y forma",
                          "receivedItems": [
                            {
                              "supplierSku": "PROV-123",
                              "productName": "Alfombra Gris 120x160cm",
                              "receivedQuantity": 10,
                              "unitPrice": 1250.50
                            }
                          ]
                        }
                        """
                            ),
                            @ExampleObject(
                                    name = "⚠️ Entrega parcial",
                                    summary = "Faltaron algunos productos",
                                    value = """
                        {
                          "purchaseOrderId": 1,
                          "deliveryDate": "2024-12-20T10:00:00",
                          "notes": "REMITO-002//Faltaron 2 unidades por problemas de stock",
                          "receivedItems": [
                            {
                              "supplierSku": "PROV-123",
                              "productName": "Alfombra Gris 120x160cm",
                              "receivedQuantity": 8,
                              "unitPrice": 1250.50
                            }
                          ]
                        }
                        """
                            ),
                            @ExampleObject(
                                    name = "➕ Productos extras",
                                    summary = "Recibieron productos no pedidos",
                                    value = """
                        {
                          "purchaseOrderId": 1,
                          "deliveryDate": "2024-12-20T10:00:00",
                          "notes": "REMITO-002//Vinieron productos de cortesía",
                          "receivedItems": [
                            {
                              "supplierSku": "PROV-123",
                              "productName": "Alfombra Gris 120x160cm",
                              "receivedQuantity": 10,
                              "unitPrice": 1250.50
                            },
                            {
                              "supplierSku": "PROV-999",
                              "productName": "Muestra Gratis - Alfombra Pequeña",
                              "receivedQuantity": 2,
                              "unitPrice": 0
                            }
                          ]
                        }
                        """
                            )
                    })
            )
            @Valid @RequestBody DeliveryReceiptDTO receiptDTO) {
        OrderReconciliationDTO reconciliation = purchaseOrderService.processDeliveryReceipt(receiptDTO);
        return ExtendedBaseResponse.ok(reconciliation, "Remito procesado exitosamente")
                .toResponseEntity();
    }

    @Operation(
            summary = "📊 Reconciliar pedido",
            description = """
            Muestra el estado actual de un pedido sin necesidad de cargar un nuevo remito.
            
            **Información que proporciona:**
            - Items recibidos correctamente
            - Items con entrega parcial
            - Items faltantes
            - Resumen de cantidades y valores
            - Recomendaciones de acción
            """,
            operationId = "reconcileOrder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Reconciliación completada"),
            @ApiResponse(responseCode = "404", description = "❌ Pedido no encontrado")
    })
    @GetMapping("/{orderId}/reconcile")
    public ResponseEntity<ExtendedBaseResponse<OrderReconciliationDTO>> reconcileOrder(
            @Parameter(description = "ID del pedido a reconciliar", required = true, example = "1")
            @PathVariable Long orderId) {
        OrderReconciliationDTO reconciliation = purchaseOrderService.reconcileOrder(orderId);
        return ExtendedBaseResponse.ok(reconciliation, "Reconciliación completada")
                .toResponseEntity();
    }

    @Operation(
            summary = "✅ Confirmar pedido completado",
            description = """
            Marca un pedido como `COMPLETADO` manualmente.
            
            **Requisitos:**
            - Todos los items deben tener la cantidad recibida igual a la pedida
            - Si hay items pendientes, la operación será rechazada
            """,
            operationId = "confirmOrderCompletion"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Pedido completado"),
            @ApiResponse(responseCode = "400", description = "❌ Hay items pendientes de recibir"),
            @ApiResponse(responseCode = "404", description = "❌ Pedido no encontrado")
    })
    @PatchMapping("/{orderId}/complete")
    public ResponseEntity<ExtendedBaseResponse<PurchaseOrderResponseDTO>> confirmOrderCompletion(
            @Parameter(description = "ID del pedido", required = true, example = "1")
            @PathVariable Long orderId) {
        PurchaseOrderResponseDTO completed = purchaseOrderService.confirmOrderCompletion(orderId);
        return ExtendedBaseResponse.ok(completed, "Pedido completado exitosamente")
                .toResponseEntity();
    }

    // ========== CONSULTAS ESPECÍFICAS ==========

    @Operation(
            summary = "🏢 Buscar pedidos por proveedor",
            description = "Retorna todos los pedidos asociados a un proveedor específico.",
            operationId = "getOrdersBySupplier"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Pedidos encontrados"),
            @ApiResponse(responseCode = "404", description = "❌ Proveedor no encontrado")
    })
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ExtendedBaseResponse<List<PurchaseOrderResponseDTO>>> getOrdersBySupplier(
            @Parameter(description = "ID del proveedor", required = true, example = "1")
            @PathVariable Long supplierId) {
        List<PurchaseOrderResponseDTO> orders = purchaseOrderService.getOrdersBySupplier(supplierId);
        return ExtendedBaseResponse.ok(orders, "Pedidos obtenidos por proveedor")
                .toResponseEntity();
    }

    @Operation(
            summary = "🎯 Buscar pedidos por estado",
            description = """
            Retorna todos los pedidos con un estado específico.
            
            **Estados disponibles:**
            - `PENDIENTE`: Esperando recepción
            - `PARCIAL`: Recepción parcial completada
            - `COMPLETADO`: Totalmente recibido
            - `CANCELADO`: Pedido cancelado
            """,
            operationId = "getOrdersByStatus"
    )
    @ApiResponse(responseCode = "200", description = "✅ Pedidos encontrados")
    @GetMapping("/status/{status}")
    public ResponseEntity<ExtendedBaseResponse<List<PurchaseOrderResponseDTO>>> getOrdersByStatus(
            @Parameter(description = "Estado del pedido", required = true,
                    example = "PENDIENTE",
                    schema = @Schema(allowableValues = {"PENDIENTE", "PARCIAL", "COMPLETADO", "CANCELADO"}))
            @PathVariable String status) {
        List<PurchaseOrderResponseDTO> orders = purchaseOrderService.getOrdersByStatus(status);
        return ExtendedBaseResponse.ok(orders, "Pedidos obtenidos por estado")
                .toResponseEntity();
    }

    @Operation(
            summary = "⏳ Listar pedidos pendientes",
            description = "Retorna una lista de todos los pedidos que aún no han sido completados (estado `PENDIENTE` o `PARCIAL`).",
            operationId = "getPendingOrders"
    )
    @ApiResponse(responseCode = "200", description = "✅ Lista de pedidos pendientes")
    @GetMapping("/pending")
    public ResponseEntity<ExtendedBaseResponse<List<PurchaseOrderResponseDTO>>> getPendingOrders() {
        List<PurchaseOrderResponseDTO> orders = purchaseOrderService.getPendingOrders();
        return ExtendedBaseResponse.ok(orders, "Pedidos pendientes obtenidos")
                .toResponseEntity();
    }
}