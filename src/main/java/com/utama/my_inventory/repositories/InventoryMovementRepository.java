package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.InventoryMovement;
import com.utama.my_inventory.entities.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByProductId(Long productId);

    Page<InventoryMovement> findByProductId(Long productId, Pageable pageable);

    List<InventoryMovement> findByProductIdAndMovementType(Long productId, MovementType movementType);

    List<InventoryMovement> findByMovementDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT im FROM InventoryMovement im WHERE im.product.id = :productId " +
            "ORDER BY im.movementDate DESC")
    List<InventoryMovement> findRecentByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT im FROM InventoryMovement im WHERE im.product.supplier.id = :supplierId " +
            "AND im.movementDate BETWEEN :startDate AND :endDate")
    List<InventoryMovement> findBySupplierAndDateRange(@Param("supplierId") Long supplierId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(im.quantity) FROM InventoryMovement im " +
            "WHERE im.product.id = :productId AND im.movementType = :movementType")
    Integer sumQuantityByProductAndType(@Param("productId") Long productId,
                                        @Param("movementType") MovementType movementType);

    long countByProductId(Long productId);
}