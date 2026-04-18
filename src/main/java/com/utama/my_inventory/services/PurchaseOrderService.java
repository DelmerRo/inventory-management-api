package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.PurchaseOrderDTO;
import com.utama.my_inventory.dtos.request.PurchaseOrderItemDTO;
import com.utama.my_inventory.dtos.request.DeliveryReceiptDTO;
import com.utama.my_inventory.dtos.response.OrderReconciliationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PurchaseOrderService {

    // CRUD básico
    PurchaseOrderDTO createOrder(PurchaseOrderDTO purchaseOrderDTO);
    PurchaseOrderDTO updateOrder(Long id, PurchaseOrderDTO purchaseOrderDTO);
    PurchaseOrderDTO getOrderById(Long id);
    Page<PurchaseOrderDTO> getAllOrders(Pageable pageable);
    void deleteOrder(Long id);
    void cancelOrder(Long id);

    // Gestión de items
    PurchaseOrderDTO addItemToOrder(Long orderId, PurchaseOrderItemDTO itemDTO);  // ← Este método faltaba
    PurchaseOrderDTO removeItemFromOrder(Long orderId, Long itemId);
    PurchaseOrderDTO updateItemQuantity(Long orderId, Long itemId, Integer newQuantity);

    // Recepción y contraste
    OrderReconciliationDTO processDeliveryReceipt(DeliveryReceiptDTO receiptDTO);
    OrderReconciliationDTO reconcileOrder(Long orderId);

    // Consultas específicas
    List<PurchaseOrderDTO> getOrdersBySupplier(Long supplierId);
    List<PurchaseOrderDTO> getOrdersByStatus(String status);
    Page<PurchaseOrderDTO> getPendingOrders(Pageable pageable);

    // Confirmación final
    PurchaseOrderDTO confirmOrderCompletion(Long orderId);
}