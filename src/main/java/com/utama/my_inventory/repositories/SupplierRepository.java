package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByName(String name);

    List<Supplier> findByActiveTrue();

    Page<Supplier> findByActiveTrue(Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE s.active = true AND LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Supplier> searchActiveSuppliers(String search);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    long countByActiveTrue();
}
