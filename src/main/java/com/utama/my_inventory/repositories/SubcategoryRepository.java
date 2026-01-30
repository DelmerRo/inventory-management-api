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


    List<Subcategory> findByCategoryId(Long categoryId);

    boolean existsByCategoryIdAndName(Long categoryId, String name);

    Optional<Subcategory> findByNameAndCategoryName(String name, String categoryName);
}
