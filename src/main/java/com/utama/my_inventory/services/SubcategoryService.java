package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.SubcategoryRequestDTO;
import com.utama.my_inventory.dtos.response.SubcategoryResponseDTO;

import java.util.List;

public interface SubcategoryService {
    SubcategoryResponseDTO createSubcategory(SubcategoryRequestDTO requestDTO);

    List<SubcategoryResponseDTO> getAllSubcategories();

    List<SubcategoryResponseDTO> getSubcategoriesByCategoryId(Long categoryId);

    SubcategoryResponseDTO getSubcategoryById(Long id);

    SubcategoryResponseDTO updateSubcategory(Long id, SubcategoryRequestDTO requestDTO);

    void deleteSubcategory(Long id);
}
