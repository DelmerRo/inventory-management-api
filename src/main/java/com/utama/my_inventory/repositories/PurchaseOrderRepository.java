// PurchaseOrderRepository.java
package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findByStatus(String status);

    List<PurchaseOrder> findAllByOrderByOrderDateDesc();

    boolean existsByOrderNumber(String orderNumber);

    // ✅ Método para PostgreSQL (usando SPLIT_PART)
    @Query(value = "SELECT COALESCE(MAX(CAST(SPLIT_PART(order_number, '-', 3) AS INTEGER)), 0) " +
            "FROM purchase_orders " +
            "WHERE order_number LIKE CONCAT(:year, '-%')",
            nativeQuery = true)
    Long getLastSequenceByYear(@Param("year") String year);

    // ✅ Método alternativo para encontrar el último número de pedido
    @Query("SELECT p.orderNumber FROM PurchaseOrder p WHERE p.orderNumber LIKE CONCAT(:prefix, '%') ORDER BY p.id DESC")
    List<String> findLastOrderNumbersByPrefix(@Param("prefix") String prefix, org.springframework.data.domain.Pageable pageable);

    // ✅ Método para contar pedidos por año
    @Query("SELECT COUNT(p) FROM PurchaseOrder p WHERE p.orderNumber LIKE CONCAT(:year, '-%')")
    Long countOrdersByYear(@Param("year") String year);
}