package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.InventoryMovement;
import com.utama.my_inventory.entities.enums.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    // Búsquedas por producto
    List<InventoryMovement> findByProductIdOrderByMovementDateDesc(Long productId);
    List<InventoryMovement> findByProductIdAndMovementTypeOrderByMovementDateDesc(Long productId, MovementType movementType);

    // Búsquedas por tipo de movimiento
    List<InventoryMovement> findByMovementTypeOrderByMovementDateDesc(MovementType movementType);

    // Búsquedas por usuario
    List<InventoryMovement> findByRegisteredByOrderByMovementDateDesc(String registeredBy);

    // Búsquedas por fecha
    List<InventoryMovement> findByMovementDateBetweenOrderByMovementDateDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    List<InventoryMovement> findByProductIdAndMovementDateBetweenOrderByMovementDateDesc(
            Long productId, LocalDateTime startDate, LocalDateTime endDate);

    List<InventoryMovement> findAllByOrderByMovementDateDesc();

    // Consultas personalizadas
    @Query("SELECT im FROM InventoryMovement im WHERE " +
            "(:productId IS NULL OR im.product.id = :productId) AND " +
            "(:movementType IS NULL OR im.movementType = :movementType) AND " +
            "(:registeredBy IS NULL OR LOWER(im.registeredBy) LIKE LOWER(CONCAT('%', :registeredBy, '%'))) AND " +
            "(:startDate IS NULL OR im.movementDate >= :startDate) AND " +
            "(:endDate IS NULL OR im.movementDate <= :endDate) " +
            "ORDER BY im.movementDate DESC")
    List<InventoryMovement> searchMovements(
            @Param("productId") Long productId,
            @Param("movementType") MovementType movementType,
            @Param("registeredBy") String registeredBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Estadísticas
    @Query("SELECT SUM(im.quantity) FROM InventoryMovement im WHERE " +
            "im.product.id = :productId AND im.movementType = 'ENTRADA'")
    Integer getTotalEntriesByProduct(@Param("productId") Long productId);

    @Query("SELECT SUM(im.quantity) FROM InventoryMovement im WHERE " +
            "im.product.id = :productId AND im.movementType = 'SALIDA'")
    Integer getTotalExitsByProduct(@Param("productId") Long productId);

    @Query("SELECT COUNT(im) FROM InventoryMovement im WHERE " +
            "im.product.id = :productId")
    Long countMovementsByProduct(@Param("productId") Long productId);

    // Último movimiento
    @Query("SELECT im FROM InventoryMovement im WHERE im.product.id = :productId " +
            "ORDER BY im.movementDate DESC LIMIT 1")
    InventoryMovement findLastMovementByProduct(@Param("productId") Long productId);
}