package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.*;
import com.utama.my_inventory.dtos.request.PurchaseOrderDTO;
import com.utama.my_inventory.dtos.response.OrderReconciliationDTO;
import com.utama.my_inventory.entities.*;
import com.utama.my_inventory.repositories.*;
import com.utama.my_inventory.services.PurchaseOrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    @Override
    public PurchaseOrderDTO createOrder(PurchaseOrderDTO dto) {
        log.info("Creando nuevo pedido: {}", dto.getOrderNumber());

        // Validar número único
        if (purchaseOrderRepository.existsByOrderNumber(dto.getOrderNumber())) {
            throw new IllegalArgumentException("Ya existe un pedido con el número: " + dto.getOrderNumber());
        }

        // Validar proveedor
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + dto.getSupplierId()));

        // Crear pedido
        PurchaseOrder order = PurchaseOrder.builder()
                .orderNumber(dto.getOrderNumber())
                .supplier(supplier)
                .orderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDateTime.now())
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .status("PENDIENTE")
                .notes(dto.getNotes())
                .items(new ArrayList<>())
                .build();

        // Agregar items
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (PurchaseOrderItemDTO itemDTO : dto.getItems()) {
                addItemToOrderEntity(order, itemDTO);
            }
        }

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
        log.info("Pedido creado exitosamente con ID: {}", savedOrder.getId());

        return convertToDTO(savedOrder);
    }

    @Override
    public PurchaseOrderDTO updateOrder(Long id, PurchaseOrderDTO dto) {
        log.info("Actualizando pedido ID: {}", id);

        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + id));

        if (order.isCompleted()) {
            throw new IllegalStateException("No se puede modificar un pedido completado");
        }

        if (dto.getExpectedDeliveryDate() != null) {
            order.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        }
        if (dto.getNotes() != null) {
            order.setNotes(dto.getNotes());
        }

        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    @Override
    public PurchaseOrderDTO getOrderById(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + id));
        return convertToDTO(order);
    }

    @Override
    public Page<PurchaseOrderDTO> getAllOrders(Pageable pageable) {
        return purchaseOrderRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Override
    public void deleteOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + id));

        if (!order.isPending()) {
            throw new IllegalStateException("Solo se pueden eliminar pedidos pendientes");
        }

        purchaseOrderRepository.delete(order);
        log.info("Pedido eliminado ID: {}", id);
    }

    @Override
    public void cancelOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + id));

        order.setStatus("CANCELADO");
        purchaseOrderRepository.save(order);
        log.info("Pedido cancelado ID: {}", id);
    }

    @Override
    public PurchaseOrderDTO addItemToOrder(Long orderId, PurchaseOrderItemDTO itemDTO) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + orderId));

        if (!order.isPending()) {
            throw new IllegalStateException("Solo se pueden agregar items a pedidos pendientes");
        }

        addItemToOrderEntity(order, itemDTO);
        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    @Override
    public PurchaseOrderDTO removeItemFromOrder(Long orderId, Long itemId) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + orderId));

        if (!order.isPending()) {
            throw new IllegalStateException("Solo se pueden eliminar items de pedidos pendientes");
        }

        PurchaseOrderItem item = purchaseOrderItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item no encontrado con ID: " + itemId));

        order.removeItem(item);
        purchaseOrderItemRepository.delete(item);

        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    @Override
    public PurchaseOrderDTO updateItemQuantity(Long orderId, Long itemId, Integer newQuantity) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + orderId));

        if (!order.isPending()) {
            throw new IllegalStateException("Solo se pueden modificar items de pedidos pendientes");
        }

        PurchaseOrderItem item = purchaseOrderItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item no encontrado con ID: " + itemId));

        item.setQuantity(newQuantity);
        purchaseOrderItemRepository.save(item);

        return convertToDTO(order);
    }

    /**
     * ⭐ MÉTODO PRINCIPAL: Procesa el remito de entrega y hace el contraste
     */
    @Override
    @Transactional
    public OrderReconciliationDTO processDeliveryReceipt(DeliveryReceiptDTO receiptDTO) {
        log.info("Procesando remito para pedido ID: {}", receiptDTO.getPurchaseOrderId());

        PurchaseOrder order = purchaseOrderRepository.findById(receiptDTO.getPurchaseOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + receiptDTO.getPurchaseOrderId()));

        if (order.isCompleted()) {
            throw new IllegalStateException("El pedido ya está completado");
        }

        // Mapa de lo pedido por SKU
        Map<String, PurchaseOrderItem> orderedItemsMap = order.getItems().stream()
                .collect(Collectors.toMap(
                        item -> item.getProduct().getSku(),
                        item -> item
                ));

        // Mapa de lo recibido (remito)
        Map<String, Integer> receivedItemsMap = receiptDTO.getReceivedItems().stream()
                .collect(Collectors.toMap(
                        ReceivedItemDTO::getSku,
                        ReceivedItemDTO::getReceivedQuantity
                ));

        // Variables para el contraste
        List<OrderReconciliationDTO.MatchedItemDTO> matchedItems = new ArrayList<>();
        List<OrderReconciliationDTO.PartialItemDTO> partialItems = new ArrayList<>();
        List<OrderReconciliationDTO.MissingItemDTO> missingItems = new ArrayList<>();
        List<OrderReconciliationDTO.ExtraItemDTO> extraItems = new ArrayList<>();

        BigDecimal totalReceivedValue = BigDecimal.ZERO;
        int totalReceivedQuantity = 0;

        // 1. Procesar productos pedidos (ver qué se entregó, qué faltó, qué vino parcial)
        for (Map.Entry<String, PurchaseOrderItem> entry : orderedItemsMap.entrySet()) {
            String sku = entry.getKey();
            PurchaseOrderItem orderedItem = entry.getValue();
            Integer orderedQty = orderedItem.getQuantity();
            Integer receivedQty = receivedItemsMap.getOrDefault(sku, 0);

            if (receivedQty.equals(orderedQty)) {
                // Caso: Entregado completo
                matchedItems.add(OrderReconciliationDTO.MatchedItemDTO.builder()
                        .sku(sku)
                        .productName(orderedItem.getProduct().getName())
                        .orderedQuantity(orderedQty)
                        .receivedQuantity(receivedQty)
                        .unitPrice(orderedItem.getUnitPrice())
                        .subtotal(orderedItem.getUnitPrice().multiply(BigDecimal.valueOf(receivedQty)))
                        .build());

                // Actualizar cantidad recibida
                orderedItem.setQuantityReceived(receivedQty);
                totalReceivedQuantity += receivedQty;
                totalReceivedValue = totalReceivedValue.add(
                        orderedItem.getUnitPrice().multiply(BigDecimal.valueOf(receivedQty))
                );

            } else if (receivedQty > 0 && receivedQty < orderedQty) {
                // Caso: Entrega parcial
                partialItems.add(OrderReconciliationDTO.PartialItemDTO.builder()
                        .sku(sku)
                        .productName(orderedItem.getProduct().getName())
                        .orderedQuantity(orderedQty)
                        .receivedQuantity(receivedQty)
                        .pendingQuantity(orderedQty - receivedQty)
                        .unitPrice(orderedItem.getUnitPrice())
                        .observation("Se recibieron " + receivedQty + " de " + orderedQty + " unidades")
                        .build());

                orderedItem.setQuantityReceived(receivedQty);
                totalReceivedQuantity += receivedQty;
                totalReceivedValue = totalReceivedValue.add(
                        orderedItem.getUnitPrice().multiply(BigDecimal.valueOf(receivedQty))
                );

            } else if (receivedQty == 0) {
                // Caso: No se entregó nada
                missingItems.add(OrderReconciliationDTO.MissingItemDTO.builder()
                        .sku(sku)
                        .productName(orderedItem.getProduct().getName())
                        .orderedQuantity(orderedQty)
                        .receivedQuantity(0)
                        .missingQuantity(orderedQty)
                        .observation("Producto no recibido")
                        .build());
            }
        }

        // 2. Procesar productos extras (recibidos pero no pedidos)
        for (Map.Entry<String, Integer> entry : receivedItemsMap.entrySet()) {
            String sku = entry.getKey();
            Integer receivedQty = entry.getValue();

            if (!orderedItemsMap.containsKey(sku)) {
                // Buscar producto por SKU
                Optional<Product> productOpt = productRepository.findBySku(sku);
                if (productOpt.isPresent()) {
                    Product product = productOpt.get();
                    extraItems.add(OrderReconciliationDTO.ExtraItemDTO.builder()
                            .sku(sku)
                            .productName(product.getName())
                            .receivedQuantity(receivedQty)
                            .observation("Producto no solicitado en el pedido original")
                            .build());

                    totalReceivedQuantity += receivedQty;
                } else {
                    log.warn("Producto con SKU {} no encontrado en el sistema", sku);
                    extraItems.add(OrderReconciliationDTO.ExtraItemDTO.builder()
                            .sku(sku)
                            .productName("DESCONOCIDO")
                            .receivedQuantity(receivedQty)
                            .observation("SKU no existe en el inventario")
                            .build());
                }
            }
        }

        // 3. Actualizar estado del pedido
        boolean hasPendingItems = order.getItems().stream()
                .anyMatch(item -> item.getPendingQuantity() > 0);

        if (hasPendingItems) {
            order.setStatus("PARCIAL");
        } else {
            order.setStatus("COMPLETADO");
            order.setDeliveryDate(receiptDTO.getDeliveryDate() != null ?
                    receiptDTO.getDeliveryDate() : LocalDateTime.now());
        }

        purchaseOrderRepository.save(order);

        // 4. Construir resumen
        OrderReconciliationDTO.ReconciliationSummary summary = OrderReconciliationDTO.ReconciliationSummary.builder()
                .totalOrderedItems(order.getItems().stream().mapToInt(PurchaseOrderItem::getQuantity).sum())
                .totalReceivedItems(totalReceivedQuantity)
                .totalMatchedQuantity(matchedItems.stream().mapToInt(OrderReconciliationDTO.MatchedItemDTO::getReceivedQuantity).sum())
                .totalPartialQuantity(partialItems.stream().mapToInt(OrderReconciliationDTO.PartialItemDTO::getReceivedQuantity).sum())
                .totalMissingQuantity(missingItems.stream().mapToInt(OrderReconciliationDTO.MissingItemDTO::getMissingQuantity).sum())
                .totalExtraQuantity(extraItems.stream().mapToInt(OrderReconciliationDTO.ExtraItemDTO::getReceivedQuantity).sum())
                .totalOrderValue(order.getTotalAmount())
                .totalReceivedValue(totalReceivedValue)
                .hasDiscrepancies(!partialItems.isEmpty() || !missingItems.isEmpty() || !extraItems.isEmpty())
                .recommendation(buildRecommendation(partialItems, missingItems, extraItems))
                .build();

        // 5. Construir respuesta completa
        return OrderReconciliationDTO.builder()
                .purchaseOrderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .supplierName(order.getSupplier().getName())
                .matchedItems(matchedItems)
                .partialItems(partialItems)
                .missingItems(missingItems)
                .extraItems(extraItems)
                .summary(summary)
                .build();
    }

    /**
     * Método para reconciliar un pedido existente (sin cargar nuevo remito)
     */
    @Override
    public OrderReconciliationDTO reconcileOrder(Long orderId) {
        log.info("Reconciliando pedido ID: {}", orderId);

        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + orderId));

        List<OrderReconciliationDTO.MatchedItemDTO> matchedItems = new ArrayList<>();
        List<OrderReconciliationDTO.PartialItemDTO> partialItems = new ArrayList<>();
        List<OrderReconciliationDTO.MissingItemDTO> missingItems = new ArrayList<>();

        BigDecimal totalReceivedValue = BigDecimal.ZERO;
        int totalReceivedQuantity = 0;

        for (PurchaseOrderItem item : order.getItems()) {
            Integer orderedQty = item.getQuantity();
            Integer receivedQty = item.getQuantityReceived();

            if (receivedQty.equals(orderedQty)) {
                matchedItems.add(OrderReconciliationDTO.MatchedItemDTO.builder()
                        .sku(item.getProduct().getSku())
                        .productName(item.getProduct().getName())
                        .orderedQuantity(orderedQty)
                        .receivedQuantity(receivedQty)
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(receivedQty)))
                        .build());
            } else if (receivedQty > 0 && receivedQty < orderedQty) {
                partialItems.add(OrderReconciliationDTO.PartialItemDTO.builder()
                        .sku(item.getProduct().getSku())
                        .productName(item.getProduct().getName())
                        .orderedQuantity(orderedQty)
                        .receivedQuantity(receivedQty)
                        .pendingQuantity(orderedQty - receivedQty)
                        .unitPrice(item.getUnitPrice())
                        .observation("Pendiente de recibir")
                        .build());
            } else if (receivedQty == 0) {
                missingItems.add(OrderReconciliationDTO.MissingItemDTO.builder()
                        .sku(item.getProduct().getSku())
                        .productName(item.getProduct().getName())
                        .orderedQuantity(orderedQty)
                        .receivedQuantity(0)
                        .missingQuantity(orderedQty)
                        .observation("No recibido")
                        .build());
            }

            totalReceivedQuantity += receivedQty;
            totalReceivedValue = totalReceivedValue.add(
                    item.getUnitPrice().multiply(BigDecimal.valueOf(receivedQty))
            );
        }

        OrderReconciliationDTO.ReconciliationSummary summary = OrderReconciliationDTO.ReconciliationSummary.builder()
                .totalOrderedItems(order.getItems().stream().mapToInt(PurchaseOrderItem::getQuantity).sum())
                .totalReceivedItems(totalReceivedQuantity)
                .totalMatchedQuantity(matchedItems.stream().mapToInt(OrderReconciliationDTO.MatchedItemDTO::getReceivedQuantity).sum())
                .totalPartialQuantity(partialItems.stream().mapToInt(OrderReconciliationDTO.PartialItemDTO::getReceivedQuantity).sum())
                .totalMissingQuantity(missingItems.stream().mapToInt(OrderReconciliationDTO.MissingItemDTO::getMissingQuantity).sum())
                .totalExtraQuantity(0)
                .totalOrderValue(order.getTotalAmount())
                .totalReceivedValue(totalReceivedValue)
                .hasDiscrepancies(!partialItems.isEmpty() || !missingItems.isEmpty())
                .recommendation(buildRecommendation(partialItems, missingItems, new ArrayList<>()))
                .build();

        return OrderReconciliationDTO.builder()
                .purchaseOrderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .supplierName(order.getSupplier().getName())
                .matchedItems(matchedItems)
                .partialItems(partialItems)
                .missingItems(missingItems)
                .extraItems(new ArrayList<>())
                .summary(summary)
                .build();
    }

    @Override
    public List<PurchaseOrderDTO> getOrdersBySupplier(Long supplierId) {
        return purchaseOrderRepository.findBySupplierId(supplierId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrderDTO> getOrdersByStatus(String status) {
        return purchaseOrderRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PurchaseOrderDTO> getPendingOrders(Pageable pageable) {
        return purchaseOrderRepository.findByStatus("PENDIENTE", pageable)
                .map(this::convertToDTO);
    }

    @Override
    public PurchaseOrderDTO confirmOrderCompletion(Long orderId) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + orderId));

        boolean allReceived = order.getItems().stream()
                .allMatch(item -> item.getQuantityReceived().equals(item.getQuantity()));

        if (!allReceived) {
            throw new IllegalStateException("No se puede completar el pedido porque hay items pendientes de recibir");
        }

        order.setStatus("COMPLETADO");
        order.setDeliveryDate(LocalDateTime.now());

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
        return convertToDTO(savedOrder);
    }

    // ========== MÉTODOS PRIVADOS AUXILIARES ==========

    private void addItemToOrderEntity(PurchaseOrder order, PurchaseOrderItemDTO itemDTO) {
        Product product = productRepository.findById(itemDTO.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + itemDTO.getProductId()));

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .product(product)
                .quantity(itemDTO.getQuantity())
                .unitPrice(itemDTO.getUnitPrice())
                .quantityReceived(0)
                .build();

        order.addItem(item);
    }

    private String buildRecommendation(List<OrderReconciliationDTO.PartialItemDTO> partials,
                                       List<OrderReconciliationDTO.MissingItemDTO> missing,
                                       List<OrderReconciliationDTO.ExtraItemDTO> extras) {
        List<String> recommendations = new ArrayList<>();

        if (!partials.isEmpty()) {
            recommendations.add("⚠️ Hay " + partials.size() + " producto(s) con entrega parcial. Contactar al proveedor para el envío pendiente.");
        }

        if (!missing.isEmpty()) {
            recommendations.add("❌ Faltan " + missing.size() + " producto(s) no entregados. Gestionar reclamación al proveedor.");
        }

        if (!extras.isEmpty()) {
            recommendations.add("➕ Hay " + extras.size() + " producto(s) extras no pedidos. Evaluar si se aceptan o se devuelven.");
        }

        if (partials.isEmpty() && missing.isEmpty() && extras.isEmpty()) {
            recommendations.add("✅ Pedido completado correctamente sin discrepancias.");
        }

        return String.join(" | ", recommendations);
    }

    private PurchaseOrderDTO convertToDTO(PurchaseOrder order) {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .supplierId(order.getSupplier().getId())
                .supplierName(order.getSupplier().getName())
                .orderDate(order.getOrderDate())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .status(order.getStatus())
                .notes(order.getNotes())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(new ArrayList<>())
                .build();

        if (order.getItems() != null) {
            dto.setItems(order.getItems().stream()
                    .map(this::convertItemToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private PurchaseOrderItemDTO convertItemToDTO(PurchaseOrderItem item) {
        return PurchaseOrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productSku(item.getProduct().getSku())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .quantityReceived(item.getQuantityReceived())
                .subtotal(item.getSubtotal())
                .pendingQuantity(item.getPendingQuantity())
                .fullyReceived(item.isFullyReceived())
                .build();
    }
}