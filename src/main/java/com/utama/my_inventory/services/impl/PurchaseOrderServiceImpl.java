package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.*;
import com.utama.my_inventory.dtos.response.OrderReconciliationDTO;
import com.utama.my_inventory.dtos.response.PurchaseOrderItemResponseDTO;
import com.utama.my_inventory.dtos.response.PurchaseOrderResponseDTO;
import com.utama.my_inventory.entities.*;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.repositories.*;
import com.utama.my_inventory.services.PurchaseOrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String ORDER_PREFIX = "PO";
    private static final int SEQUENCE_LENGTH = 6;

    @Override
    public PurchaseOrderResponseDTO createOrder(PurchaseOrderRequestDTO dto) {
        log.info("Creando nuevo pedido");

        String orderNumber = generateOrderNumber();
        log.info("Número de pedido generado: {}", orderNumber);

        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + dto.getSupplierId()));

        PurchaseOrder order = PurchaseOrder.builder()
                .orderNumber(orderNumber)
                .supplier(supplier)
                .orderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDateTime.now())
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .status("PENDIENTE")
                .notes(dto.getNotes())
                .items(new ArrayList<>())
                .build();

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (PurchaseOrderItemRequestDTO itemDTO : dto.getItems()) {
                addItemToOrderEntity(order, itemDTO);
            }
        }

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
        log.info("Pedido creado exitosamente con ID: {} y número: {}", savedOrder.getId(), savedOrder.getOrderNumber());

        return convertToResponseDTO(savedOrder);
    }

    private String generateOrderNumber() {
        int year = LocalDateTime.now().getYear();
        String yearStr = String.valueOf(year);

        log.info("Generando número de pedido para el año: {}", yearStr);

        // Método 1: Usar la consulta nativa
        Long lastSequence = purchaseOrderRepository.getLastSequenceByYear(yearStr);
        log.info("Última secuencia encontrada vía native query: {}", lastSequence);

        long nextNumber = (lastSequence != null ? lastSequence : 0) + 1;

        // Generar el número de pedido
        String sequenceStr = String.format("%0" + SEQUENCE_LENGTH + "d", nextNumber);
        String orderNumber = String.format("%s-%s-%s", ORDER_PREFIX, yearStr, sequenceStr);

        log.info("Número de pedido generado: {}", orderNumber);

        // ✅ Verificar que no exista (por si acaso)
        int maxAttempts = 10;
        int attempt = 0;
        while (purchaseOrderRepository.existsByOrderNumber(orderNumber) && attempt < maxAttempts) {
            log.warn("El número de pedido {} ya existe, incrementando...", orderNumber);
            nextNumber++;
            sequenceStr = String.format("%0" + SEQUENCE_LENGTH + "d", nextNumber);
            orderNumber = String.format("%s-%s-%s", ORDER_PREFIX, yearStr, sequenceStr);
            attempt++;
        }

        if (attempt >= maxAttempts) {
            log.error("No se pudo generar un número de pedido único después de {} intentos", maxAttempts);
            throw new RuntimeException("Error generando número de pedido único");
        }

        log.info("Número de pedido final: {}", orderNumber);
        return orderNumber;
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDTO updateOrder(Long id, PurchaseOrderRequestDTO dto) {
        log.info("Actualizando pedido ID: {}", id);

        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + id));

        if (order.isCompleted()) {
            throw new IllegalStateException("No se puede modificar un pedido completado");
        }

        // Actualizar campos básicos
        if (dto.getExpectedDeliveryDate() != null) {
            order.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        }
        if (dto.getNotes() != null) {
            order.setNotes(dto.getNotes());
        }

        // ✅ ACTUALIZAR ITEMS: Usar un mapa para manejar actualizaciones
        Map<String, PurchaseOrderItem> existingItemsMap = order.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getSku, item -> item));

        // Procesar items del DTO
        List<PurchaseOrderItem> itemsToKeep = new ArrayList<>();

        for (PurchaseOrderItemRequestDTO itemDTO : dto.getItems()) {
            PurchaseOrderItem existingItem = existingItemsMap.get(itemDTO.getSupplierSku());

            if (existingItem != null) {
                // Actualizar item existente
                existingItem.setQuantity(itemDTO.getQuantity());
                existingItem.setUnitPrice(itemDTO.getUnitPrice());
                existingItem.setProductName(itemDTO.getProductName() != null ?
                        itemDTO.getProductName() : existingItem.getProductName());
                itemsToKeep.add(existingItem);
            } else {
                // Agregar nuevo item
                PurchaseOrderItem newItem = PurchaseOrderItem.builder()
                        .purchaseOrder(order)
                        .sku(itemDTO.getSupplierSku())
                        .productName(itemDTO.getProductName() != null ?
                                itemDTO.getProductName() : "Producto pendiente de crear")
                        .quantity(itemDTO.getQuantity())
                        .unitPrice(itemDTO.getUnitPrice())
                        .quantityReceived(0)
                        .build();

                // Buscar producto existente por supplierSku
                productRepository.findBySupplierSku(itemDTO.getSupplierSku())
                        .ifPresent(newItem::setProduct);

                order.addItem(newItem);
                itemsToKeep.add(newItem);
            }
        }

        // Eliminar items que ya no están en el DTO
        order.getItems().removeIf(item -> !itemsToKeep.contains(item));

        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        log.info("Pedido actualizado con ID: {}, Items: {}", id, updatedOrder.getItems().size());

        return convertToResponseDTO(updatedOrder);
    }

    @Override
    public PurchaseOrderResponseDTO getOrderById(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + id));
        return convertToResponseDTO(order);
    }

    @Override
    public List<PurchaseOrderResponseDTO> getAllOrders() {
        log.info("Obteniendo todos los pedidos");
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllByOrderByOrderDateDesc();
        return orders.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
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
    public PurchaseOrderResponseDTO addItemToOrder(Long orderId, PurchaseOrderItemRequestDTO itemDTO) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + orderId));

        if (!order.isPending()) {
            throw new IllegalStateException("Solo se pueden agregar items a pedidos pendientes");
        }

        addItemToOrderEntity(order, itemDTO);
        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        return convertToResponseDTO(updatedOrder);
    }

    @Override
    public PurchaseOrderResponseDTO removeItemFromOrder(Long orderId, Long itemId) {
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
        return convertToResponseDTO(updatedOrder);
    }

    @Override
    public PurchaseOrderResponseDTO updateItemQuantity(Long orderId, Long itemId, Integer newQuantity) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + orderId));

        if (!order.isPending()) {
            throw new IllegalStateException("Solo se pueden modificar items de pedidos pendientes");
        }

        PurchaseOrderItem item = purchaseOrderItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item no encontrado con ID: " + itemId));

        item.setQuantity(newQuantity);
        purchaseOrderItemRepository.save(item);

        return convertToResponseDTO(order);
    }

    @Override
    @Transactional
    public OrderReconciliationDTO processDeliveryReceipt(DeliveryReceiptDTO receiptDTO) {
        log.info("Procesando remito para pedido ID: {}", receiptDTO.getPurchaseOrderId());

        PurchaseOrder order = purchaseOrderRepository.findById(receiptDTO.getPurchaseOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + receiptDTO.getPurchaseOrderId()));

        if (order.isCompleted()) {
            throw new IllegalStateException("El pedido ya está completado");
        }

        // ✅ ACTUALIZAR NOTAS si vienen en el request
        if (receiptDTO.getNotes() != null && !receiptDTO.getNotes().trim().isEmpty()) {
            String currentNotes = order.getNotes();
            String newNote = receiptDTO.getNotes();

            if (currentNotes != null && !currentNotes.trim().isEmpty()) {
                order.setNotes(currentNotes + " | " + newNote);
            } else {
                order.setNotes(newNote);
            }
            log.info("Notas actualizadas del pedido: {}", order.getNotes());
        }

        Map<String, PurchaseOrderItem> orderedItemsMap = order.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getSku, item -> item));

        Map<String, ReceivedItemDTO> receivedItemsMap = receiptDTO.getReceivedItems().stream()
                .collect(Collectors.toMap(ReceivedItemDTO::getSupplierSku, item -> item));

        List<OrderReconciliationDTO.MatchedItemDTO> matchedItems = new ArrayList<>();
        List<OrderReconciliationDTO.PartialItemDTO> partialItems = new ArrayList<>();
        List<OrderReconciliationDTO.MissingItemDTO> missingItems = new ArrayList<>();
        List<OrderReconciliationDTO.ExtraItemDTO> extraItems = new ArrayList<>();

        BigDecimal totalReceivedValue = BigDecimal.ZERO;
        int totalReceivedQuantity = 0;

        // ✅ Procesar productos pedidos
        for (Map.Entry<String, PurchaseOrderItem> entry : orderedItemsMap.entrySet()) {
            String supplierSku = entry.getKey();
            PurchaseOrderItem orderedItem = entry.getValue();
            Integer orderedQty = orderedItem.getQuantity();
            ReceivedItemDTO receivedItem = receivedItemsMap.get(supplierSku);
            int receivedQty = receivedItem != null ? receivedItem.getAdditionalQuantity() : 0;
            BigDecimal receivedUnitPrice = (receivedItem != null && receivedItem.getUnitPrice() != null)
                    ? receivedItem.getUnitPrice() : orderedItem.getUnitPrice();

            Product product = orderedItem.getProduct();
            boolean isNewProduct = false;

            // ✅ Crear producto si no existe
            if (product == null && receivedQty > 0) {
                String productName = orderedItem.getProductName() != null
                        ? orderedItem.getProductName()
                        : "Producto " + supplierSku;
                product = createOrGetProductBySupplierSku(supplierSku, productName);
                orderedItem.setProduct(product);
                isNewProduct = true;
            }

            int currentReceived = orderedItem.getQuantityReceived() != null ? orderedItem.getQuantityReceived() : 0;
            int newTotalReceived = currentReceived + receivedQty;
            int actualReceivedThisTime = Math.min(receivedQty, orderedQty - currentReceived);

            if (actualReceivedThisTime > 0) {
                // ✅ ACTIVAR PRODUCTO SI ESTABA INACTIVO
                if (product != null && !product.getActive() && actualReceivedThisTime > 0) {
                    product.setActive(true);
                    log.info("✅ Producto ACTIVADO al recibir mercadería - SKU: {}, Nombre: {}",
                            product.getSku(), product.getName());
                    productRepository.save(product);
                }

                if (newTotalReceived >= orderedQty) {
                    // Entrega completa de este producto
                    matchedItems.add(OrderReconciliationDTO.MatchedItemDTO.builder()
                            .sku(product != null ? product.getSku() : supplierSku)
                            .productName(orderedItem.getProductName())
                            .orderedQuantity(orderedQty)
                            .receivedQuantity(actualReceivedThisTime)
                            .unitPrice(receivedUnitPrice)
                            .subtotal(receivedUnitPrice.multiply(BigDecimal.valueOf(actualReceivedThisTime)))
                            .build());
                    log.info("✅ Producto completado: {} - Total recibido: {}/{}",
                            orderedItem.getProductName(), newTotalReceived, orderedQty);
                } else {
                    // Entrega parcial de este producto
                    partialItems.add(OrderReconciliationDTO.PartialItemDTO.builder()
                            .sku(product != null ? product.getSku() : supplierSku)
                            .productName(orderedItem.getProductName())
                            .orderedQuantity(orderedQty)
                            .receivedQuantity(actualReceivedThisTime)
                            .pendingQuantity(orderedQty - newTotalReceived)
                            .unitPrice(receivedUnitPrice)
                            .observation("Se recibieron " + actualReceivedThisTime + " unidades en esta entrega. Total recibido: " + newTotalReceived)
                            .build());
                    log.info("⚠️ Producto parcial: {} - Recibido: {}/{}",
                            orderedItem.getProductName(), newTotalReceived, orderedQty);
                }

                orderedItem.setQuantityReceived(newTotalReceived);
                totalReceivedQuantity += actualReceivedThisTime;
                totalReceivedValue = totalReceivedValue.add(
                        receivedUnitPrice.multiply(BigDecimal.valueOf(actualReceivedThisTime))
                );

                if (product != null) {
                    product.setCurrentStock(product.getCurrentStock() + actualReceivedThisTime);
                    productRepository.save(product);
                }

            } else if (receivedQty == 0 && currentReceived == 0) {
                // Producto no recibido y nunca se había recibido nada
                missingItems.add(OrderReconciliationDTO.MissingItemDTO.builder()
                        .sku(product != null ? product.getSku() : supplierSku)
                        .productName(orderedItem.getProductName())
                        .orderedQuantity(orderedQty)
                        .receivedQuantity(0)
                        .missingQuantity(orderedQty)
                        .observation("Producto no recibido")
                        .build());
                log.info("❌ Producto faltante: {}", orderedItem.getProductName());
            }
        }

        // ✅ Procesar productos extras (recibidos pero no pedidos)
        for (Map.Entry<String, ReceivedItemDTO> entry : receivedItemsMap.entrySet()) {
            String supplierSku = entry.getKey();
            ReceivedItemDTO receivedItem = entry.getValue();

            if (!orderedItemsMap.containsKey(supplierSku)) {
                Product product = createOrGetProductBySupplierSku(supplierSku, receivedItem.getProductName());

                // ✅ ACTIVAR PRODUCTO EXTRA
                if (product != null && !product.getActive() && receivedItem.getAdditionalQuantity() > 0) {
                    product.setActive(true);
                    log.info("✅ Producto extra ACTIVADO al recibir mercadería - SKU: {}, Nombre: {}",
                            product.getSku(), product.getName());
                }

                extraItems.add(OrderReconciliationDTO.ExtraItemDTO.builder()
                        .sku(product.getSku())
                        .productName(product.getName())
                        .receivedQuantity(receivedItem.getAdditionalQuantity())
                        .observation("Producto no solicitado en el pedido original. Se ha agregado al inventario.")
                        .build());

                totalReceivedQuantity += receivedItem.getAdditionalQuantity();
                product.setCurrentStock(product.getCurrentStock() + receivedItem.getAdditionalQuantity());
                productRepository.save(product);

                log.info("➕ Producto extra recibido: {} - Cantidad: {}", product.getName(), receivedItem.getAdditionalQuantity());
            }
        }

        // ✅ Actualizar estado del pedido
        boolean hasPendingItems = order.getItems().stream()
                .anyMatch(item -> item.getPendingQuantity() > 0);

        if (hasPendingItems) {
            order.setStatus("PARCIAL");
            log.info("📦 Pedido {} actualizado a estado: PARCIAL", order.getOrderNumber());
        } else {
            order.setStatus("COMPLETADO");
            order.setDeliveryDate(receiptDTO.getDeliveryDate() != null
                    ? receiptDTO.getDeliveryDate()
                    : LocalDateTime.now());
            log.info("✅ Pedido {} completado exitosamente", order.getOrderNumber());
        }
        purchaseOrderRepository.save(order);

        // ✅ Construir resumen
        OrderReconciliationDTO.ReconciliationSummary summary = OrderReconciliationDTO.ReconciliationSummary.builder()
                .totalOrderedItems(order.getItems().stream().mapToInt(PurchaseOrderItem::getQuantity).sum())
                .totalReceivedItems(totalReceivedQuantity)
                .totalMatchedQuantity(matchedItems.stream().mapToInt(OrderReconciliationDTO.MatchedItemDTO::getReceivedQuantity).sum())
                .totalPartialQuantity(partialItems.stream().mapToInt(OrderReconciliationDTO.PartialItemDTO::getPendingQuantity).sum())
                .totalMissingQuantity(missingItems.stream().mapToInt(OrderReconciliationDTO.MissingItemDTO::getMissingQuantity).sum())
                .totalExtraQuantity(extraItems.stream().mapToInt(OrderReconciliationDTO.ExtraItemDTO::getReceivedQuantity).sum())
                .totalOrderValue(order.getTotalAmount())
                .totalReceivedValue(totalReceivedValue)
                .hasDiscrepancies(!partialItems.isEmpty() || !missingItems.isEmpty() || !extraItems.isEmpty())
                .recommendation(buildRecommendation(partialItems, missingItems, extraItems))
                .build();

        log.info("📊 Resumen de recepción - Pedido: {}, Items recibidos: {}, Valor recibido: ${}",
                order.getOrderNumber(), totalReceivedQuantity, totalReceivedValue);

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
                        .sku(item.getSku()).productName(item.getProductName())
                        .orderedQuantity(orderedQty).receivedQuantity(receivedQty)
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(receivedQty))).build());
            } else if (receivedQty > 0 && receivedQty < orderedQty) {
                partialItems.add(OrderReconciliationDTO.PartialItemDTO.builder()
                        .sku(item.getSku()).productName(item.getProductName())
                        .orderedQuantity(orderedQty).receivedQuantity(receivedQty)
                        .pendingQuantity(orderedQty - receivedQty).unitPrice(item.getUnitPrice())
                        .observation("Pendiente de recibir").build());
            } else if (receivedQty == 0) {
                missingItems.add(OrderReconciliationDTO.MissingItemDTO.builder()
                        .sku(item.getSku()).productName(item.getProductName())
                        .orderedQuantity(orderedQty).receivedQuantity(0)
                        .missingQuantity(orderedQty).observation("No recibido").build());
            }
            totalReceivedQuantity += receivedQty;
            totalReceivedValue = totalReceivedValue.add(item.getUnitPrice().multiply(BigDecimal.valueOf(receivedQty)));
        }

        OrderReconciliationDTO.ReconciliationSummary summary = OrderReconciliationDTO.ReconciliationSummary.builder()
                .totalOrderedItems(order.getItems().stream().mapToInt(PurchaseOrderItem::getQuantity).sum())
                .totalReceivedItems(totalReceivedQuantity)
                .totalMatchedQuantity(matchedItems.stream().mapToInt(OrderReconciliationDTO.MatchedItemDTO::getReceivedQuantity).sum())
                .totalPartialQuantity(partialItems.stream().mapToInt(OrderReconciliationDTO.PartialItemDTO::getPendingQuantity).sum())
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
    public List<PurchaseOrderResponseDTO> getOrdersBySupplier(Long supplierId) {
        return purchaseOrderRepository.findBySupplierId(supplierId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrderResponseDTO> getOrdersByStatus(String status) {
        return purchaseOrderRepository.findByStatus(status).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrderResponseDTO> getPendingOrders() {
        return purchaseOrderRepository.findByStatus("PENDIENTE").stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PurchaseOrderResponseDTO confirmOrderCompletion(Long orderId) {
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
        return convertToResponseDTO(savedOrder);
    }

    // ========== MÉTODOS PRIVADOS ==========

    private void addItemToOrderEntity(PurchaseOrder order, PurchaseOrderItemRequestDTO itemDTO) {
        Optional<PurchaseOrderItem> existingItemOpt = order.getItems().stream()
                .filter(item -> item.getSku().equals(itemDTO.getSupplierSku()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            int newQuantity = getNewQuantity(itemDTO, existingItemOpt);
            log.info("Item actualizado - SKU proveedor: {}, Nueva cantidad: {}", itemDTO.getSupplierSku(), newQuantity);

        } else {
            Optional<Product> productOpt = productRepository.findBySupplierSku(itemDTO.getSupplierSku());

            PurchaseOrderItem newItem = PurchaseOrderItem.builder()
                    .purchaseOrder(order)
                    .sku(itemDTO.getSupplierSku())
                    .productName(itemDTO.getProductName() != null ? itemDTO.getProductName() : "Producto pendiente de crear")
                    .quantity(itemDTO.getQuantity())
                    .unitPrice(itemDTO.getUnitPrice())
                    .quantityReceived(0)
                    .build();

            productOpt.ifPresent(newItem::setProduct);
            order.addItem(newItem);
            log.info("Item agregado - SKU proveedor: {}, Cantidad: {}", itemDTO.getSupplierSku(), itemDTO.getQuantity());
        }
    }

    private static int getNewQuantity(PurchaseOrderItemRequestDTO itemDTO, Optional<PurchaseOrderItem> existingItemOpt) {
        PurchaseOrderItem existingItem = existingItemOpt.get();

        if (existingItem.getUnitPrice().compareTo(itemDTO.getUnitPrice()) != 0) {
            throw new BusinessException(String.format(
                    "El producto con SKU de proveedor '%s' ya existe en el pedido con un precio diferente (%.2f vs %.2f)",
                    itemDTO.getSupplierSku(), existingItem.getUnitPrice(), itemDTO.getUnitPrice()
            ));
        }

        int newQuantity = existingItem.getQuantity() + itemDTO.getQuantity();
        existingItem.setQuantity(newQuantity);
        return newQuantity;
    }

    // PurchaseOrderServiceImpl.java
    private Product createOrGetProductBySupplierSku(String supplierSku, String productName) {
        return productRepository.findBySupplierSku(supplierSku)
                .orElseGet(() -> {
                    String finalName = (productName != null && !productName.isBlank()) ? productName : "Producto " + supplierSku;
                    String internalSku = generateProductSku();

                    Product newProduct = Product.builder()
                            .sku(internalSku)
                            .supplierSku(supplierSku)
                            .name(finalName)
                            .description("Producto creado automáticamente desde pedido de compra. Pendiente de activación.")
                            .currentStock(0)
                            .active(false)  // ✅ INACTIVO hasta recibir mercadería
                            .build();

                    log.info("Creando nuevo producto INACTIVO - SKU interno: {}, SKU proveedor: {}, Nombre: {}",
                            internalSku, supplierSku, finalName);
                    return productRepository.save(newProduct);
                });
    }

    private String generateProductSku() {
        Long lastId = productRepository.getMaxId();
        long nextNumber = (lastId != null ? lastId : 0) + 1;
        String sequenceStr = String.format("%06d", nextNumber);
        return "PROD-" + sequenceStr;
    }

    private String buildRecommendation(List<OrderReconciliationDTO.PartialItemDTO> partials,
                                       List<OrderReconciliationDTO.MissingItemDTO> missing,
                                       List<OrderReconciliationDTO.ExtraItemDTO> extras) {
        List<String> recommendations = new ArrayList<>();

        if (!partials.isEmpty()) {
            int totalPending = partials.stream().mapToInt(OrderReconciliationDTO.PartialItemDTO::getPendingQuantity).sum();
            recommendations.add("⚠️ Hay " + partials.size() + " producto(s) con entrega parcial. Faltan " + totalPending + " unidades por recibir.");
        }

        if (!missing.isEmpty()) {
            int totalMissing = missing.stream().mapToInt(OrderReconciliationDTO.MissingItemDTO::getMissingQuantity).sum();
            recommendations.add("❌ Faltan " + missing.size() + " producto(s) no entregados. Total de unidades faltantes: " + totalMissing);
        }

        if (!extras.isEmpty()) {
            int totalExtra = extras.stream().mapToInt(OrderReconciliationDTO.ExtraItemDTO::getReceivedQuantity).sum();
            recommendations.add("➕ Hay " + extras.size() + " producto(s) extras no pedidos. Total de unidades extras: " + totalExtra);
        }

        if (partials.isEmpty() && missing.isEmpty() && extras.isEmpty()) {
            recommendations.add("✅ Pedido completado correctamente sin discrepancias.");
        }

        return String.join(" | ", recommendations);
    }

    private PurchaseOrderResponseDTO convertToResponseDTO(PurchaseOrder order) {
        PurchaseOrderResponseDTO dto = PurchaseOrderResponseDTO.builder()
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
                    .map(this::convertItemToResponseDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private PurchaseOrderItemResponseDTO convertItemToResponseDTO(PurchaseOrderItem item) {
        return PurchaseOrderItemResponseDTO.builder()
                .id(item.getId())
                .sku(item.getSku())
                .productName(item.getDisplayProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .quantityReceived(item.getQuantityReceived())
                .subtotal(item.getSubtotal())
                .pendingQuantity(item.getPendingQuantity())
                .fullyReceived(item.isFullyReceived())
                .build();
    }

}