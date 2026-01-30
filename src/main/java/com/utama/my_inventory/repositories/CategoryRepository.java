package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    List<Category> findByActiveTrue();

    boolean existsByName(String name);

    Optional<Category> findByIdAndActiveTrue(Long id);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.active = true")
    Optional<Category> findActiveById(@Param("id") Long id);
}
