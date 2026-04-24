package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.Subcategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {


    List<Subcategory> findByCategoryId(Long categoryId);

    boolean existsByCategoryIdAndName(Long categoryId, String name);

    Optional<Subcategory> findByNameAndCategoryName(String name, String categoryName);

    Optional<Subcategory> findByName(@NotBlank(message = "El nombre de la subcategoría es obligatorio") @Size(max = 100, message = "La subcategoría no puede exceder 100 caracteres") String subcategoryName);

    boolean existsByNameAndCategoryName(String name, @NotBlank(message = "El nombre es obligatorio") @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres") String name1);
}
