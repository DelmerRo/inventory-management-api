package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByIdAndActiveTrue(Long id);
    List<Supplier> findByActiveTrue();
    List<Supplier> findByActiveTrueOrderByNameAsc();

    Optional<Supplier> findByNameAndActiveTrue(String name);

    boolean existsByNameAndActiveTrue(String name);

    @Query("SELECT s FROM Supplier s WHERE " +
            "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:contactPerson IS NULL OR LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :contactPerson, '%'))) AND " +
            "(:email IS NULL OR LOWER(s.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "s.active = true")
    List<Supplier> searchSuppliers(
            @Param("name") String name,
            @Param("contactPerson") String contactPerson,
            @Param("email") String email);


    @Query("SELECT COUNT(s) FROM Supplier s WHERE s.active = true")
    Long countActiveSuppliers();
}