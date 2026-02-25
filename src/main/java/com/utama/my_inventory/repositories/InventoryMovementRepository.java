package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.InventoryMovement;
import com.utama.my_inventory.entities.enums.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> , JpaSpecificationExecutor<InventoryMovement> {

    // Métodos simples que Spring Data genera automáticamente
    List<InventoryMovement> findByProductIdOrderByMovementDateDesc(Long productId);
    List<InventoryMovement> findByMovementTypeOrderByMovementDateDesc(MovementType movementType);
    List<InventoryMovement> findByMovementDateBetweenOrderByMovementDateDesc(LocalDateTime startDate, LocalDateTime endDate);
    List<InventoryMovement> findAllByOrderByMovementDateDesc();

    // Estadísticas
    @Query("SELECT COALESCE(SUM(im.quantity), 0) FROM InventoryMovement im WHERE " +
            "im.product.id = :productId AND im.movementType = 'ENTRADA'")
    Integer getTotalEntriesByProduct(@Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(im.quantity), 0) FROM InventoryMovement im WHERE " +
            "im.product.id = :productId AND im.movementType = 'SALIDA'")
    Integer getTotalExitsByProduct(@Param("productId") Long productId);

    @Query("SELECT COUNT(im) FROM InventoryMovement im WHERE " +
            "im.product.id = :productId")
    Long countMovementsByProduct(@Param("productId") Long productId);

    List<InventoryMovement> findByRegisteredByOrderByMovementDateDesc(String username);
}