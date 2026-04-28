package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.*;
import com.utama.my_inventory.dtos.request.inventory.StockEntryRequestDTO;
import com.utama.my_inventory.dtos.response.OrderReconciliationDTO;
import com.utama.my_inventory.dtos.response.PurchaseOrderItemResponseDTO;
import com.utama.my_inventory.dtos.response.PurchaseOrderResponseDTO;
import com.utama.my_inventory.entities.*;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.repositories.*;
import com.utama.my_inventory.services.InventoryService;
import com.utama.my_inventory.services.PurchaseOrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
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
    private final ProductSupplierRepository productSupplierRepository;
    private final InventoryService inventoryService;

    private static final String ORDER_PREFIX = "PO";
    private static final int SEQUENCE_LENGTH = 6;

    // ==================== CRUD PRINCIPAL ====================

    @Override
    public PurchaseOrderResponseDTO createOrder(PurchaseOrderRequestDTO dto) {
        log.info("Creando nuevo pedido");

        String orderNumber = generateOrderNumber();
        Supplier supplier = findSupplierById(dto.getSupplierId());

        PurchaseOrder order = buildPurchaseOrder(orderNumber, supplier, dto);
        addItemsToOrder(order, dto.getItems());

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
        log.info("Pedido creado exitosamente - ID: {}, Número: {}", savedOrder.getId(), savedOrder.getOrderNumber());

        return convertToResponseDTO(savedOrder);
    }

    @Override
    public PurchaseOrderResponseDTO updateOrder(Long id, PurchaseOrderRequestDTO dto) {
        log.info("Actualizando pedido ID: {}", id);

        PurchaseOrder order = findOrderById(id);
        validateModifiableOrder(order);

        updateOrderBasicInfo(order, dto);
        syncOrderItems(order, dto.getItems());

        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        log.info("Pedido actualizado ID: {}, Items: {}", id, updatedOrder.getItems().size());

        return convertToResponseDTO(updatedOrder);
    }

    @Override
    public PurchaseOrderResponseDTO getOrderById(Long id) {
        return convertToResponseDTO(findOrderById(id));
    }

    @Override
    public List<PurchaseOrderResponseDTO> getAllOrders() {
        return purchaseOrderRepository.findAllByOrderByOrderDateDesc().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteOrder(Long id) {
        PurchaseOrder order = findOrderById(id);
        validatePendingOrder(order);
        purchaseOrderRepository.delete(order);
        log.info("Pedido eliminado ID: {}", id);
    }

    @Override
    public void cancelOrder(Long id) {
        PurchaseOrder order = findOrderById(id);
        order.setStatus("CANCELADO");
        purchaseOrderRepository.save(order);
        log.info("Pedido cancelado ID: {}", id);
    }

    // ==================== GESTIÓN DE ITEMS ====================

    @Override
    public PurchaseOrderResponseDTO addItemToOrder(Long orderId, PurchaseOrderItemRequestDTO itemDTO) {
        PurchaseOrder order = findOrderById(orderId);
        validateModifiableOrder(order);

        addItemToOrderEntity(order, itemDTO);
        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);

        return convertToResponseDTO(updatedOrder);
    }

    @Override
    public PurchaseOrderResponseDTO removeItemFromOrder(Long orderId, Long itemId) {
        PurchaseOrder order = findOrderById(orderId);
        validateModifiableOrder(order);

        PurchaseOrderItem item = findOrderItemById(itemId);
        order.removeItem(item);
        purchaseOrderItemRepository.delete(item);

        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        return convertToResponseDTO(updatedOrder);
    }

    @Override
    public PurchaseOrderResponseDTO updateItemQuantity(Long orderId, Long itemId, Integer newQuantity) {
        PurchaseOrder order = findOrderById(orderId);
        validateModifiableOrder(order);

        PurchaseOrderItem item = findOrderItemById(itemId);
        item.setQuantity(newQuantity);
        purchaseOrderItemRepository.save(item);

        return convertToResponseDTO(order);
    }

    // ==================== RECEPCIÓN Y RECONCILIACIÓN ====================

    @Override
    @Transactional
    public OrderReconciliationDTO processDeliveryReceipt(DeliveryReceiptDTO receiptDTO) {
        log.info("========== INICIO PROCESAMIENTO DE REMITO ==========");

        PurchaseOrder order = findOrderById(receiptDTO.getPurchaseOrderId());
        validateOrderNotCompleted(order);

        updateOrderNotes(order, receiptDTO.getNotes());

        DeliveryReceiptContext context = buildDeliveryContext(order, receiptDTO);
        processOrderedItems(context);
        processExtraItems(context);
        updateOrderStatus(context.order);

        logOrderSummary(context);

        return buildReconciliationResponse(context);
    }

    @Override
    public OrderReconciliationDTO reconcileOrder(Long orderId) {
        log.info("Reconciliando pedido ID: {}", orderId);
        PurchaseOrder order = findOrderById(orderId);

        ReconciliationResult result = calculateReconciliation(order);

        return buildReconciliationResponse(order, result);
    }

    // ==================== CONSULTAS ESPECÍFICAS ====================

    @Override
    public List<PurchaseOrderResponseDTO> getOrdersBySupplier(Long supplierId) {
        findSupplierById(supplierId);
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
        PurchaseOrder order = findOrderById(orderId);

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

    // ==================== MÉTODOS PRIVADOS - HELPERS ====================

    // --- Validaciones y búsquedas ---

    private PurchaseOrder findOrderById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + id));
    }

    private PurchaseOrderItem findOrderItemById(Long id) {
        return purchaseOrderItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item no encontrado con ID: " + id));
    }

    private Supplier findSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));
    }

    private void validateModifiableOrder(PurchaseOrder order) {
        if (order.isCompleted()) {
            throw new IllegalStateException("No se puede modificar un pedido completado");
        }
    }

    private void validatePendingOrder(PurchaseOrder order) {
        if (!order.isPending()) {
            throw new IllegalStateException("Solo se pueden " + "eliminar" + " pedidos pendientes");
        }
    }

    private void validateOrderNotCompleted(PurchaseOrder order) {
        if (order.isCompleted()) {
            throw new IllegalStateException("El pedido ya está completado");
        }
    }

    // --- Creación y construcción ---

    private String generateOrderNumber() {
        int year = LocalDateTime.now().getYear();
        String yearStr = String.valueOf(year);

        Long lastSequence = purchaseOrderRepository.getLastSequenceByYearJPQL(yearStr);
        long nextNumber = (lastSequence != null ? lastSequence : 0) + 1;

        String orderNumber = String.format("%s-%s-%s", ORDER_PREFIX, yearStr,
                String.format("%0" + SEQUENCE_LENGTH + "d", nextNumber));

        return ensureUniqueOrderNumber(orderNumber, yearStr, nextNumber);
    }

    private String ensureUniqueOrderNumber(String orderNumber, String yearStr, long baseNumber) {
        int attempts = 0;
        long currentNumber = baseNumber;
        String currentOrderNumber = orderNumber;

        while (purchaseOrderRepository.existsByOrderNumber(currentOrderNumber) && attempts < 10) {
            log.warn("Número de pedido {} ya existe, incrementando...", currentOrderNumber);
            currentNumber++;
            currentOrderNumber = String.format("%s-%s-%s", ORDER_PREFIX, yearStr,
                    String.format("%0" + SEQUENCE_LENGTH + "d", currentNumber));
            attempts++;
        }

        if (attempts >= 10) {
            throw new RuntimeException("No se pudo generar un número de pedido único");
        }

        return currentOrderNumber;
    }

    private PurchaseOrder buildPurchaseOrder(String orderNumber, Supplier supplier, PurchaseOrderRequestDTO dto) {
        return PurchaseOrder.builder()
                .orderNumber(orderNumber)
                .supplier(supplier)
                .orderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDateTime.now())
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .status("PENDIENTE")
                .notes(dto.getNotes())
                .items(new ArrayList<>())
                .build();
    }

    private void addItemsToOrder(PurchaseOrder order, List<PurchaseOrderItemRequestDTO> items) {
        if (items != null && !items.isEmpty()) {
            items.forEach(item -> addItemToOrderEntity(order, item));
        }
    }

    private void updateOrderBasicInfo(PurchaseOrder order, PurchaseOrderRequestDTO dto) {
        if (dto.getExpectedDeliveryDate() != null) {
            order.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        }
        if (dto.getNotes() != null) {
            order.setNotes(dto.getNotes());
        }
    }

    private void syncOrderItems(PurchaseOrder order, List<PurchaseOrderItemRequestDTO> newItems) {
        Map<String, PurchaseOrderItem> existingItemsMap = order.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getSku, item -> item));

        List<PurchaseOrderItem> itemsToKeep = new ArrayList<>();

        for (PurchaseOrderItemRequestDTO itemDTO : newItems) {
            PurchaseOrderItem existingItem = existingItemsMap.get(itemDTO.getSupplierSku());

            if (existingItem != null) {
                updateExistingItem(existingItem, itemDTO);
                itemsToKeep.add(existingItem);
            } else {
                PurchaseOrderItem newItem = createNewOrderItem(order, itemDTO);
                order.addItem(newItem);
                itemsToKeep.add(newItem);
            }
        }

        order.getItems().removeIf(item -> !itemsToKeep.contains(item));
    }

    private void updateExistingItem(PurchaseOrderItem item, PurchaseOrderItemRequestDTO dto) {
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(dto.getUnitPrice());
        if (dto.getProductName() != null) {
            item.setProductName(dto.getProductName());
        }
    }

    private PurchaseOrderItem createNewOrderItem(PurchaseOrder order, PurchaseOrderItemRequestDTO dto) {
        PurchaseOrderItem newItem = PurchaseOrderItem.builder()
                .purchaseOrder(order)
                .sku(dto.getSupplierSku())
                .productName(dto.getProductName() != null ? dto.getProductName() : "Producto pendiente de crear")
                .quantity(dto.getQuantity())
                .unitPrice(dto.getUnitPrice())
                .quantityReceived(0)
                .build();

        productSupplierRepository.findBySupplierSku(dto.getSupplierSku())
                .map(ProductSupplier::getProduct)
                .ifPresent(newItem::setProduct);

        return newItem;
    }

    private void addItemToOrderEntity(PurchaseOrder order, PurchaseOrderItemRequestDTO itemDTO) {
        Optional<PurchaseOrderItem> existingItemOpt = order.getItems().stream()
                .filter(item -> item.getSku().equals(itemDTO.getSupplierSku()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            PurchaseOrderItem existingItem = existingItemOpt.get();
            validateSameUnitPrice(existingItem, itemDTO);
            existingItem.setQuantity(existingItem.getQuantity() + itemDTO.getQuantity());
            log.info("Item actualizado - SKU: {}, Nueva cantidad: {}", itemDTO.getSupplierSku(), existingItem.getQuantity());
        } else {
            PurchaseOrderItem newItem = createNewOrderItem(order, itemDTO);
            order.addItem(newItem);
            log.info("Item agregado - SKU: {}, Cantidad: {}", itemDTO.getSupplierSku(), itemDTO.getQuantity());
        }
    }

    private void validateSameUnitPrice(PurchaseOrderItem existingItem, PurchaseOrderItemRequestDTO newItem) {
        if (existingItem.getUnitPrice().compareTo(newItem.getUnitPrice()) != 0) {
            throw new BusinessException(String.format(
                    "El producto con SKU '%s' ya existe con precio diferente (%.2f vs %.2f)",
                    newItem.getSupplierSku(), existingItem.getUnitPrice(), newItem.getUnitPrice()
            ));
        }
    }

    private void updateOrderNotes(PurchaseOrder order, String newNotes) {
        if (newNotes != null && !newNotes.trim().isEmpty()) {
            String currentNotes = order.getNotes();
            order.setNotes(currentNotes != null && !currentNotes.trim().isEmpty()
                    ? currentNotes + " | " + newNotes
                    : newNotes);
            log.info("Notas actualizadas del pedido: {}", order.getNotes());
        }
    }

    // --- Contexto para procesamiento de recepción ---

    // Reemplazar el record por una clase mutable
    @Getter
    private static class DeliveryReceiptContext {
        // Getters
        private final PurchaseOrder order;
        private final Map<String, PurchaseOrderItem> orderedItemsMap;
        private final Map<String, ReceivedItemDTO> receivedItemsMap;
        private final List<OrderReconciliationDTO.MatchedItemDTO> matchedItems;
        private final List<OrderReconciliationDTO.PartialItemDTO> partialItems;
        private final List<OrderReconciliationDTO.MissingItemDTO> missingItems;
        private final List<OrderReconciliationDTO.ExtraItemDTO> extraItems;
        private final List<InventoryMovementRecord> movementRecords;
        private BigDecimal totalReceivedValue;
        private int totalReceivedQuantity;

        private DeliveryReceiptContext(PurchaseOrder order, DeliveryReceiptDTO receiptDTO) {
            this.order = order;
            this.orderedItemsMap = order.getItems().stream()
                    .collect(Collectors.toMap(PurchaseOrderItem::getSku, item -> item));
            this.receivedItemsMap = receiptDTO.getReceivedItems().stream()
                    .collect(Collectors.toMap(ReceivedItemDTO::getSupplierSku, item -> item));
            this.matchedItems = new ArrayList<>();
            this.partialItems = new ArrayList<>();
            this.missingItems = new ArrayList<>();
            this.extraItems = new ArrayList<>();
            this.movementRecords = new ArrayList<>();
            this.totalReceivedValue = BigDecimal.ZERO;
            this.totalReceivedQuantity = 0;
        }

        public static DeliveryReceiptContext create(PurchaseOrder order, DeliveryReceiptDTO receiptDTO) {
            return new DeliveryReceiptContext(order, receiptDTO);
        }

        // Setters para modificar
        public void addToTotalReceivedValue(BigDecimal value) {
            this.totalReceivedValue = this.totalReceivedValue.add(value);
        }
        public void addToTotalReceivedQuantity(int quantity) {
            this.totalReceivedQuantity += quantity;
        }
    }

    private DeliveryReceiptContext buildDeliveryContext(PurchaseOrder order, DeliveryReceiptDTO receiptDTO) {
        return DeliveryReceiptContext.create(order, receiptDTO);
    }

    private void processOrderedItems(DeliveryReceiptContext ctx) {
        for (Map.Entry<String, PurchaseOrderItem> entry : ctx.getOrderedItemsMap().entrySet()) {
            String supplierSku = entry.getKey();
            PurchaseOrderItem orderedItem = entry.getValue();
            ReceivedItemDTO receivedItem = ctx.getReceivedItemsMap().get(supplierSku);

            processSingleOrderedItem(ctx, orderedItem, receivedItem);
        }
    }

    private void processSingleOrderedItem(DeliveryReceiptContext ctx, PurchaseOrderItem orderedItem, ReceivedItemDTO receivedItem) {
        Integer orderedQty = orderedItem.getQuantity();
        int receivedQty = receivedItem != null ? receivedItem.getAdditionalQuantity() : 0;
        BigDecimal receivedUnitPrice = getReceivedUnitPrice(receivedItem, orderedItem);

        Product product = getOrCreateProductForItem(orderedItem, receivedQty, ctx.getOrder().getSupplier().getId());

        int currentReceived = orderedItem.getQuantityReceived() != null ? orderedItem.getQuantityReceived() : 0;
        int newTotalReceived = currentReceived + receivedQty;
        int actualReceived = Math.min(receivedQty, orderedQty - currentReceived);

        if (actualReceived > 0 && product != null) {
            processReceivedProduct(ctx, orderedItem, product, actualReceived, newTotalReceived,
                    orderedQty, receivedUnitPrice);
            orderedItem.setQuantityReceived(newTotalReceived);
            ctx.addToTotalReceivedQuantity(actualReceived);
        } else if (receivedQty == 0 && currentReceived == 0) {
            ctx.getMissingItems().add(buildMissingItemDTO(orderedItem, product, orderedItem.getSku()));
        }
    }

    private void processReceivedProduct(DeliveryReceiptContext ctx, PurchaseOrderItem orderedItem,
                                        Product product, int actualReceived, int newTotalReceived,
                                        int orderedQty, BigDecimal unitPrice) {
        activateProductIfNeeded(product);
        registerInventoryMovement(product.getId(), actualReceived, unitPrice,
                buildMovementReason(ctx.getOrder(), orderedItem));

        updateProductStock(product, actualReceived);

        if (newTotalReceived >= orderedQty) {
            ctx.getMatchedItems().add(buildMatchedItemDTO(orderedItem, product, actualReceived, unitPrice));
        } else {
            ctx.getPartialItems().add(buildPartialItemDTO(orderedItem, product, actualReceived,
                    newTotalReceived, orderedQty, unitPrice));
        }

        ctx.addToTotalReceivedValue(unitPrice.multiply(BigDecimal.valueOf(actualReceived)));
        ctx.getMovementRecords().add(new InventoryMovementRecord(
                product.getId(), product.getName(), actualReceived, unitPrice, "ENTRADA"));
    }

    private Product getOrCreateProductForItem(PurchaseOrderItem item, int receivedQty, Long supplierId) {
        Product product = item.getProduct();
        if (product == null && receivedQty > 0) {
            String productName = item.getProductName() != null ? item.getProductName() : "Producto " + item.getSku();
            product = createOrGetProductBySupplierSku(item.getSku(), productName, supplierId);
            item.setProduct(product);
            log.info("🆕 Nuevo producto creado: {} (ID: {})", product.getName(), product.getId());
        }
        return product;
    }

    private void processExtraItems(DeliveryReceiptContext ctx) {
        for (Map.Entry<String, ReceivedItemDTO> entry : ctx.getReceivedItemsMap().entrySet()) {
            String supplierSku = entry.getKey();
            ReceivedItemDTO receivedItem = entry.getValue();

            if (!ctx.getOrderedItemsMap().containsKey(supplierSku)) {
                processSingleExtraItem(ctx, supplierSku, receivedItem);
            }
        }
    }

    private void processSingleExtraItem(DeliveryReceiptContext ctx, String supplierSku, ReceivedItemDTO receivedItem) {
        Product product = createOrGetProductBySupplierSku(
                supplierSku, receivedItem.getProductName(), ctx.getOrder().getSupplier().getId());

        activateProductIfNeeded(product);

        BigDecimal unitPrice = receivedItem.getUnitPrice() != null ? receivedItem.getUnitPrice() : BigDecimal.ZERO;

        registerInventoryMovement(product.getId(), receivedItem.getAdditionalQuantity(), unitPrice,
                "PRODUCTO EXTRA - Pedido: " + ctx.getOrder().getOrderNumber());

        updateProductStock(product, receivedItem.getAdditionalQuantity());

        ctx.getExtraItems().add(buildExtraItemDTO(product, receivedItem.getAdditionalQuantity()));
        ctx.addToTotalReceivedQuantity(receivedItem.getAdditionalQuantity());

        if (unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            ctx.addToTotalReceivedValue(unitPrice.multiply(BigDecimal.valueOf(receivedItem.getAdditionalQuantity())));
        }

        ctx.getMovementRecords().add(new InventoryMovementRecord(
                product.getId(), product.getName(), receivedItem.getAdditionalQuantity(), unitPrice, "ENTRADA_EXTRA"));

        log.info("➕ Producto extra recibido: {} - Cantidad: {}", product.getName(), receivedItem.getAdditionalQuantity());
    }

    private void updateOrderStatus(PurchaseOrder order) {
        boolean hasPendingItems = order.getItems().stream()
                .anyMatch(item -> item.getPendingQuantity() > 0);

        if (hasPendingItems) {
            order.setStatus("PARCIAL");
            log.info("📦 Pedido {} actualizado a estado: PARCIAL", order.getOrderNumber());
        } else {
            order.setStatus("COMPLETADO");
            order.setDeliveryDate(LocalDateTime.now());
            log.info("✅ Pedido {} completado exitosamente", order.getOrderNumber());
        }
        purchaseOrderRepository.save(order);
    }

    private void logOrderSummary(DeliveryReceiptContext ctx) {
        log.info("========== RESUMEN DE RECEPCIÓN ==========");
        log.info("Pedido: {}", ctx.order.getOrderNumber());
        log.info("Items recibidos: {}", ctx.totalReceivedQuantity);
        log.info("Valor recibido: ${}", ctx.totalReceivedValue);
        log.info("Movimientos registrados: {}", ctx.movementRecords.size());
        ctx.movementRecords.forEach(record ->
                log.info("  - Producto: {}, Cantidad: +{}, Tipo: {}", record.productName, record.quantity, record.type));
        log.info("========== FIN PROCESAMIENTO DE REMITO ==========");
    }

    private OrderReconciliationDTO buildReconciliationResponse(DeliveryReceiptContext ctx) {
        OrderReconciliationDTO.ReconciliationSummary summary = buildSummary(ctx);

        return OrderReconciliationDTO.builder()
                .purchaseOrderId(ctx.order.getId())
                .orderNumber(ctx.order.getOrderNumber())
                .supplierName(ctx.order.getSupplier().getName())
                .matchedItems(ctx.matchedItems)
                .partialItems(ctx.partialItems)
                .missingItems(ctx.missingItems)
                .extraItems(ctx.extraItems)
                .summary(summary)
                .build();
    }

    private OrderReconciliationDTO buildReconciliationResponse(PurchaseOrder order, ReconciliationResult result) {
        OrderReconciliationDTO.ReconciliationSummary summary = OrderReconciliationDTO.ReconciliationSummary.builder()
                .totalOrderedItems(result.totalOrdered)
                .totalReceivedItems(result.totalReceived)
                .totalMatchedQuantity(result.matched)
                .totalPartialQuantity(result.partial)
                .totalMissingQuantity(result.missing)
                .totalExtraQuantity(0)
                .totalOrderValue(order.getTotalAmount())
                .totalReceivedValue(result.totalReceivedValue)
                .hasDiscrepancies(result.partial > 0 || result.missing > 0)
                .recommendation(buildRecommendation(result.partialItems, result.missingItems, new ArrayList<>()))
                .build();

        return OrderReconciliationDTO.builder()
                .purchaseOrderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .supplierName(order.getSupplier().getName())
                .matchedItems(result.matchedItems)
                .partialItems(result.partialItems)
                .missingItems(result.missingItems)
                .extraItems(new ArrayList<>())
                .summary(summary)
                .build();
    }

    private OrderReconciliationDTO.ReconciliationSummary buildSummary(DeliveryReceiptContext ctx) {
        return OrderReconciliationDTO.ReconciliationSummary.builder()
                .totalOrderedItems(ctx.order.getItems().stream().mapToInt(PurchaseOrderItem::getQuantity).sum())
                .totalReceivedItems(ctx.totalReceivedQuantity)
                .totalMatchedQuantity(ctx.matchedItems.stream().mapToInt(OrderReconciliationDTO.MatchedItemDTO::getReceivedQuantity).sum())
                .totalPartialQuantity(ctx.partialItems.stream().mapToInt(OrderReconciliationDTO.PartialItemDTO::getPendingQuantity).sum())
                .totalMissingQuantity(ctx.missingItems.stream().mapToInt(OrderReconciliationDTO.MissingItemDTO::getMissingQuantity).sum())
                .totalExtraQuantity(ctx.extraItems.stream().mapToInt(OrderReconciliationDTO.ExtraItemDTO::getReceivedQuantity).sum())
                .totalOrderValue(ctx.order.getTotalAmount())
                .totalReceivedValue(ctx.totalReceivedValue)
                .hasDiscrepancies(!ctx.partialItems.isEmpty() || !ctx.missingItems.isEmpty() || !ctx.extraItems.isEmpty())
                .recommendation(buildRecommendation(ctx.partialItems, ctx.missingItems, ctx.extraItems))
                .build();
    }

    private record ReconciliationResult(
            int totalOrdered, int totalReceived, int matched, int partial, int missing,
            BigDecimal totalReceivedValue,
            List<OrderReconciliationDTO.MatchedItemDTO> matchedItems,
            List<OrderReconciliationDTO.PartialItemDTO> partialItems,
            List<OrderReconciliationDTO.MissingItemDTO> missingItems
    ) {}

    private ReconciliationResult calculateReconciliation(PurchaseOrder order) {
        List<OrderReconciliationDTO.MatchedItemDTO> matchedItems = new ArrayList<>();
        List<OrderReconciliationDTO.PartialItemDTO> partialItems = new ArrayList<>();
        List<OrderReconciliationDTO.MissingItemDTO> missingItems = new ArrayList<>();

        BigDecimal totalReceivedValue = BigDecimal.ZERO;
        int totalOrdered = 0, totalReceived = 0, matched = 0, partial = 0, missing = 0;

        for (PurchaseOrderItem item : order.getItems()) {
            Integer orderedQty = item.getQuantity();
            Integer receivedQty = item.getQuantityReceived();
            totalOrdered += orderedQty;
            totalReceived += receivedQty;

            if (receivedQty.equals(orderedQty)) {
                matched += receivedQty;
                matchedItems.add(buildMatchedItemDTO(item));
            } else if (receivedQty > 0 && receivedQty < orderedQty) {
                partial += orderedQty - receivedQty;
                partialItems.add(buildPartialItemDTO(item));
            } else if (receivedQty == 0) {
                missing += orderedQty;
                missingItems.add(buildMissingItemDTO(item));
            }

            totalReceivedValue = totalReceivedValue.add(
                    item.getUnitPrice().multiply(BigDecimal.valueOf(receivedQty)));
        }

        return new ReconciliationResult(totalOrdered, totalReceived, matched, partial, missing,
                totalReceivedValue, matchedItems, partialItems, missingItems);
    }

    // --- DTO Builders ---

    private OrderReconciliationDTO.MatchedItemDTO buildMatchedItemDTO(PurchaseOrderItem item) {
        return OrderReconciliationDTO.MatchedItemDTO.builder()
                .sku(item.getSku())
                .productName(item.getProductName())
                .orderedQuantity(item.getQuantity())
                .receivedQuantity(item.getQuantityReceived())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }

    private OrderReconciliationDTO.MatchedItemDTO buildMatchedItemDTO(PurchaseOrderItem item, Product product,
                                                                      int receivedQty, BigDecimal unitPrice) {
        return OrderReconciliationDTO.MatchedItemDTO.builder()
                .sku(product.getSku())
                .productName(item.getProductName())
                .orderedQuantity(item.getQuantity())
                .receivedQuantity(receivedQty)
                .unitPrice(unitPrice)
                .subtotal(unitPrice.multiply(BigDecimal.valueOf(receivedQty)))
                .build();
    }

    private OrderReconciliationDTO.PartialItemDTO buildPartialItemDTO(PurchaseOrderItem item) {
        return OrderReconciliationDTO.PartialItemDTO.builder()
                .sku(item.getSku())
                .productName(item.getProductName())
                .orderedQuantity(item.getQuantity())
                .receivedQuantity(item.getQuantityReceived())
                .pendingQuantity(item.getPendingQuantity())
                .unitPrice(item.getUnitPrice())
                .observation("Pendiente de recibir")
                .build();
    }

    private OrderReconciliationDTO.PartialItemDTO buildPartialItemDTO(PurchaseOrderItem item, Product product,
                                                                      int receivedQty, int newTotalReceived,
                                                                      int orderedQty, BigDecimal unitPrice) {
        return OrderReconciliationDTO.PartialItemDTO.builder()
                .sku(product.getSku())
                .productName(item.getProductName())
                .orderedQuantity(orderedQty)
                .receivedQuantity(receivedQty)
                .pendingQuantity(orderedQty - newTotalReceived)
                .unitPrice(unitPrice)
                .observation("Se recibieron " + receivedQty + " unidades. Total recibido: " + newTotalReceived)
                .build();
    }

    private OrderReconciliationDTO.MissingItemDTO buildMissingItemDTO(PurchaseOrderItem item) {
        return OrderReconciliationDTO.MissingItemDTO.builder()
                .sku(item.getSku())
                .productName(item.getProductName())
                .orderedQuantity(item.getQuantity())
                .receivedQuantity(item.getQuantityReceived() != null ? item.getQuantityReceived() : 0)
                .missingQuantity(item.getQuantity())
                .observation("No recibido")
                .build();
    }

    private OrderReconciliationDTO.MissingItemDTO buildMissingItemDTO(PurchaseOrderItem item, Product product, String sku) {
        return OrderReconciliationDTO.MissingItemDTO.builder()
                .sku(product != null ? product.getSku() : sku)
                .productName(item.getProductName())
                .orderedQuantity(item.getQuantity())
                .receivedQuantity(0)
                .missingQuantity(item.getQuantity())
                .observation("Producto no recibido")
                .build();
    }

    private OrderReconciliationDTO.ExtraItemDTO buildExtraItemDTO(Product product, int quantity) {
        return OrderReconciliationDTO.ExtraItemDTO.builder()
                .sku(product.getSku())
                .productName(product.getName())
                .receivedQuantity(quantity)
                .observation("Producto no solicitado en el pedido original. Se ha agregado al inventario.")
                .build();
    }

    private BigDecimal getReceivedUnitPrice(ReceivedItemDTO receivedItem, PurchaseOrderItem orderedItem) {
        return (receivedItem != null && receivedItem.getUnitPrice() != null)
                ? receivedItem.getUnitPrice()
                : orderedItem.getUnitPrice();
    }

    private String buildMovementReason(PurchaseOrder order, PurchaseOrderItem item) {
        return String.format("RECEPCIÓN DE PEDIDO - Pedido: %s, Item: %s",
                order.getOrderNumber(), item.getProductName());
    }

    // --- Product Management ---

    private Product createOrGetProductBySupplierSku(String supplierSku, String productName, Long supplierId) {
        log.info("🔍 Buscando producto por supplierSku: {}", supplierSku);

        Optional<ProductSupplier> existingRelation = productSupplierRepository.findBySupplierSku(supplierSku);

        if (existingRelation.isPresent()) {
            Product existingProduct = existingRelation.get().getProduct();
            log.info("✅ Producto encontrado: {} (ID: {})", existingProduct.getSku(), existingProduct.getId());
            ensureSupplierAssociation(existingProduct, supplierId, supplierSku);
            return existingProduct;
        }

        return createNewProduct(supplierSku, productName, supplierId);
    }

    private Product createNewProduct(String supplierSku, String productName, Long supplierId) {
        log.info("📦 Creando nuevo producto con supplierSku: {}", supplierSku);

        String finalName = (productName != null && !productName.isBlank()) ? productName : "Producto " + supplierSku;
        String internalSku = generateProductSku();

        Product newProduct = Product.builder()
                .sku(internalSku)
                .name(finalName)
                .description("Producto creado automáticamente desde pedido de compra. Pendiente de activación.")
                .currentStock(0)
                .active(false)
                .subcategory(null)
                .build();

        Product savedProduct = productRepository.save(newProduct);
        createProductSupplierRelation(savedProduct, supplierId, supplierSku);

        return savedProduct;
    }

    private void ensureSupplierAssociation(Product product, Long supplierId, String supplierSku) {
        if (supplierId == null) return;

        boolean alreadyHasSupplier = product.getProductSuppliers().stream()
                .anyMatch(ps -> ps.getSupplier().getId().equals(supplierId));

        if (!alreadyHasSupplier) {
            log.info("Agregando proveedor faltante a producto existente");
            addSupplierToExistingProduct(product, supplierId, supplierSku);
        }
    }

    private void createProductSupplierRelation(Product product, Long supplierId, String supplierSku) {
        if (supplierId == null) {
            log.error("❌ supplierId es NULL - No se puede crear relación ProductSupplier");
            return;
        }

        try {
            Supplier supplier = findSupplierById(supplierId);

            ProductSupplier productSupplier = ProductSupplier.builder()
                    .product(product)
                    .supplier(supplier)
                    .supplierSku(supplierSku)
                    .isPrimary(true)
                    .notes("Proveedor creado automáticamente desde pedido de compra")
                    .build();

            ProductSupplier savedRelation = productSupplierRepository.save(productSupplier);
            product.getProductSuppliers().add(savedRelation);

            log.info("✅ Relación ProductSupplier creada - ID: {}, Producto: {}, Proveedor: {}",
                    savedRelation.getId(), product.getSku(), supplier.getName());
        } catch (Exception e) {
            log.error("❌ Error al crear relación ProductSupplier: {}", e.getMessage(), e);
        }
    }

    private void addSupplierToExistingProduct(Product product, Long supplierId, String supplierSku) {
        try {
            Supplier supplier = findSupplierById(supplierId);

            boolean alreadyExists = productSupplierRepository
                    .findByProductIdAndSupplierId(product.getId(), supplierId)
                    .isPresent();

            if (alreadyExists) {
                log.info("Relación ya existe para producto {} y proveedor {}", product.getId(), supplierId);
                return;
            }

            ProductSupplier productSupplier = ProductSupplier.builder()
                    .product(product)
                    .supplier(supplier)
                    .supplierSku(supplierSku)
                    .isPrimary(false)
                    .notes("Proveedor agregado automáticamente desde recepción de pedido")
                    .build();

            productSupplierRepository.save(productSupplier);
            product.getProductSuppliers().add(productSupplier);
            log.info("✅ Proveedor agregado a producto existente - Producto: {}, Proveedor: {}",
                    product.getSku(), supplier.getName());
        } catch (Exception e) {
            log.error("❌ Error al agregar proveedor: {}", e.getMessage(), e);
        }
    }

    private void activateProductIfNeeded(Product product) {
        if (product != null && !product.getActive()) {
            product.setActive(true);
            productRepository.save(product);
            log.info("🔄 Producto ACTIVADO - SKU: {}, Nombre: {}", product.getSku(), product.getName());
        }
    }

    private void updateProductStock(Product product, int quantity) {
        int stockBefore = product.getCurrentStock();
        product.setCurrentStock(stockBefore + quantity);
        productRepository.save(product);
        log.info("📦 Stock actualizado - Producto: {}, Stock anterior: {}, Stock nuevo: {}",
                product.getName(), stockBefore, product.getCurrentStock());
    }

    private String generateProductSku() {
        Long lastId = productRepository.getMaxId();
        long nextNumber = (lastId != null ? lastId : 0) + 1;
        return "PROD-" + String.format("%06d", nextNumber);
    }

    private void registerInventoryMovement(Long productId, int quantity, BigDecimal unitCost, String reason) {
        try {
            StockEntryRequestDTO stockEntry = new StockEntryRequestDTO(
                    productId, quantity, reason, unitCost, "system");
            inventoryService.registerEntry(stockEntry, "system");
            log.info("✅ Movimiento de inventario registrado - Producto ID: {}, Cantidad: +{}", productId, quantity);
        } catch (Exception e) {
            log.error("❌ Error al registrar movimiento de inventario: {}", e.getMessage(), e);
            throw new BusinessException("Error al registrar movimiento de inventario: " + e.getMessage());
        }
    }

    // --- Utilidades ---

    private String buildRecommendation(List<OrderReconciliationDTO.PartialItemDTO> partials,
                                       List<OrderReconciliationDTO.MissingItemDTO> missing,
                                       List<OrderReconciliationDTO.ExtraItemDTO> extras) {
        List<String> recommendations = new ArrayList<>();

        if (!partials.isEmpty()) {
            int totalPending = partials.stream().mapToInt(OrderReconciliationDTO.PartialItemDTO::getPendingQuantity).sum();
            recommendations.add("⚠️ Hay " + partials.size() + " producto(s) con entrega parcial. Faltan " + totalPending + " unidades.");
        }

        if (!missing.isEmpty()) {
            int totalMissing = missing.stream().mapToInt(OrderReconciliationDTO.MissingItemDTO::getMissingQuantity).sum();
            recommendations.add("❌ Faltan " + missing.size() + " producto(s). Total unidades faltantes: " + totalMissing);
        }

        if (!extras.isEmpty()) {
            int totalExtra = extras.stream().mapToInt(OrderReconciliationDTO.ExtraItemDTO::getReceivedQuantity).sum();
            recommendations.add("➕ Hay " + extras.size() + " producto(s) extras. Total unidades extras: " + totalExtra);
        }

        if (partials.isEmpty() && missing.isEmpty() && extras.isEmpty()) {
            recommendations.add("✅ Pedido completado correctamente sin discrepancias.");
        }

        return String.join(" | ", recommendations);
    }

    private PurchaseOrderResponseDTO convertToResponseDTO(PurchaseOrder order) {
        return PurchaseOrderResponseDTO.builder()
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
                .items(order.getItems().stream()
                        .map(this::convertItemToResponseDTO)
                        .collect(Collectors.toList()))
                .build();
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

    // ==================== CLASES INTERNAS ====================

    private record InventoryMovementRecord(Long productId, String productName, int quantity,
                                           BigDecimal unitPrice, String type) {}
}