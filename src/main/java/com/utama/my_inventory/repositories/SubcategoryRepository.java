package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

    Optional<Subcategory> findByCategoryIdAndName(Long categoryId, String name);

    List<Subcategory> findByCategoryId(Long categoryId);

    List<Subcategory> findByCategoryActiveTrue();

    @Query("SELECT s FROM Subcategory s WHERE s.category.id = :categoryId AND s.category.active = true")
    List<Subcategory> findByActiveCategoryId(Long categoryId);

    boolean existsByCategoryIdAndName(Long categoryId, String name);

    @Query("SELECT s FROM Subcategory s WHERE s.id = :id AND s.category.active = true")
    Optional<Subcategory> findByIdWithActiveCategory(@Param("id") Long id);

    Optional<Subcategory> findByNameAndCategoryName(String name, String categoryName);
}
