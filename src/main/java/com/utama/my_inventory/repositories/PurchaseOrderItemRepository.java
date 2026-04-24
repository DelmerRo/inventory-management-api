package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    // Buscar items por ID de pedido
    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);

    // Buscar items pendientes (cantidad recibida < cantidad pedida)
    @Query("SELECT poi FROM PurchaseOrderItem poi " +
            "WHERE poi.purchaseOrder.id = :orderId AND poi.quantityReceived < poi.quantity")
    List<PurchaseOrderItem> findPendingItemsByOrderId(@Param("orderId") Long orderId);

    // Actualizar cantidad recibida
    @Modifying
    @Transactional
    @Query("UPDATE PurchaseOrderItem poi SET poi.quantityReceived = :received WHERE poi.id = :itemId")
    void updateReceivedQuantity(@Param("itemId") Long itemId, @Param("received") Integer received);
}