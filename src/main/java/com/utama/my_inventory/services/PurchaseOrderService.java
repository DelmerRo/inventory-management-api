package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.DeliveryReceiptDTO;
import com.utama.my_inventory.dtos.request.PurchaseOrderItemRequestDTO;
import com.utama.my_inventory.dtos.request.PurchaseOrderRequestDTO;
import com.utama.my_inventory.dtos.response.OrderReconciliationDTO;
import com.utama.my_inventory.dtos.response.PurchaseOrderResponseDTO;

import java.util.List;

public interface PurchaseOrderService {

    // CRUD básico
    PurchaseOrderResponseDTO createOrder(PurchaseOrderRequestDTO dto);
    PurchaseOrderResponseDTO updateOrder(Long id, PurchaseOrderRequestDTO dto);
    PurchaseOrderResponseDTO getOrderById(Long id);
    List<PurchaseOrderResponseDTO> getAllOrders();
    void deleteOrder(Long id);
    void cancelOrder(Long id);

    // Gestión de items
    PurchaseOrderResponseDTO addItemToOrder(Long orderId, PurchaseOrderItemRequestDTO itemDTO);
    PurchaseOrderResponseDTO removeItemFromOrder(Long orderId, Long itemId);
    PurchaseOrderResponseDTO updateItemQuantity(Long orderId, Long itemId, Integer newQuantity);

    // Recepción y contraste
    OrderReconciliationDTO processDeliveryReceipt(DeliveryReceiptDTO receiptDTO);
    OrderReconciliationDTO reconcileOrder(Long orderId);
    PurchaseOrderResponseDTO confirmOrderCompletion(Long orderId);

    // Consultas específicas
    List<PurchaseOrderResponseDTO> getOrdersBySupplier(Long supplierId);
    List<PurchaseOrderResponseDTO> getOrdersByStatus(String status);
    List<PurchaseOrderResponseDTO> getPendingOrders();
}