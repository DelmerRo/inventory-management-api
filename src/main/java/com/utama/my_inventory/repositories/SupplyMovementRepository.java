package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.SupplyMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplyMovementRepository extends JpaRepository<SupplyMovement, Long> {
    List<SupplyMovement> findTop50BySupplyIdOrderByMovementDateDesc(Long supplyId);
}
