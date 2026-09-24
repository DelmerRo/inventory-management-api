package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.Supply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplyRepository extends JpaRepository<Supply, Long> {
    List<Supply> findByActiveTrueOrderByNameAsc();
    Optional<Supply> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}