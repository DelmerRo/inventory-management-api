package com.utama.my_inventory.repositories
        ;

import com.utama.my_inventory.entities.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findByStatus(String status);

    Page<PurchaseOrder> findByStatus(String status, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.status = :status AND po.orderDate BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findByStatusAndDateRange(@Param("status") String status,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    Page<PurchaseOrder> findBySupplierId(Long supplierId, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.status IN ('PENDIENTE', 'PARCIAL')")
    List<PurchaseOrder> findPendingAndPartialOrders();

    boolean existsByOrderNumber(String orderNumber);
}