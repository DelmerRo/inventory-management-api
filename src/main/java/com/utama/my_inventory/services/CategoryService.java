package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.CategoryRequestDTO;
import com.utama.my_inventory.dtos.response.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {
    CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO);

    List<CategoryResponseDTO> getAllCategories();

    CategoryResponseDTO getCategoryById(Long id);

    CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO requestDTO);

    void deleteCategory(Long id);

    CategoryResponseDTO toggleCategoryStatus(Long id);
}
