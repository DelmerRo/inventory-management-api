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

    /**
     * Obtiene el valor numérico máximo de la secuencia para un año determinado.
     * Ejemplo: Para "PO-2026-000009", extrae "000009" como INTEGER -> 9.
     */
    @SuppressWarnings("SqlNoDataSourceInspection")
    @Query(value = "SELECT COALESCE(MAX(CAST(SPLIT_PART(po.order_number, '-', 3) AS INTEGER)), 0) " +
            "FROM purchase_orders po " +
            "WHERE po.order_number LIKE CONCAT('PO-', :year, '-%')",
            nativeQuery = true)
    Long getLastSequenceByYear(@Param("year") String year);
}