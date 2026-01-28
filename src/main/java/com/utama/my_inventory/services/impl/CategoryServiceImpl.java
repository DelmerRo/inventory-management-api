package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.CategoryRequestDTO;
import com.utama.my_inventory.dtos.response.CategoryResponseDTO;
import com.utama.my_inventory.entities.Category;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.mapper.CategoryMapper;
import com.utama.my_inventory.repositories.CategoryRepository;
import com.utama.my_inventory.services.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO) {
        log.info("Creating new category: {}", requestDTO.name());

        validateUniqueCategoryName(requestDTO.name());

        Category category = categoryMapper.toEntity(requestDTO);
        Category savedCategory = categoryRepository.save(category);

        log.info("Category created with ID: {}", savedCategory.getId());
        return categoryMapper.toResponseDTO(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponseDTO> getAllCategories() {
        log.info("Retrieving all active categories");
        List<Category> categories = categoryRepository.findByActiveTrue();
        return categoryMapper.toResponseDTOList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#id")
    public CategoryResponseDTO getCategoryById(Long id) {
        log.info("Retrieving category with ID: {}", id);
        Category category = findActiveCategoryById(id);
        return categoryMapper.toResponseDTO(category);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", key = "#id")
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO requestDTO) {
        log.info("Updating category with ID: {}", id);

        Category category = findActiveCategoryById(id);

        if (!category.getName().equals(requestDTO.name())) {
            validateUniqueCategoryName(requestDTO.name());
        }

        categoryMapper.updateEntityFromDTO(requestDTO, category);
        Category updatedCategory = categoryRepository.save(category);

        log.info("Category updated with ID: {}", id);
        return categoryMapper.toResponseDTO(updatedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", key = "#id")
    public void deleteCategory(Long id) {
        log.info("Attempting to delete category with ID: {}", id);

        Category category = findActiveCategoryById(id);

        if (!category.getSubcategories().isEmpty()) {
            throw new BusinessException(
                    "Cannot delete category '%s' because it has %d associated subcategories"
                            .formatted(category.getName(), category.getSubcategories().size())
            );
        }

        categoryRepository.delete(category);
        log.info("Category deleted with ID: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", key = "#id")
    public CategoryResponseDTO toggleCategoryStatus(Long id) {
        log.info("Toggling status for category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setActive(!category.getActive());
        Category updatedCategory = categoryRepository.save(category);

        String status = updatedCategory.getActive() ? "activated" : "deactivated";
        log.info("Category {} with ID: {}", status, id);
        return categoryMapper.toResponseDTO(updatedCategory);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Category findActiveCategoryById(Long id) {
        return categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found or inactive with ID: " + id
                ));
    }

    private void validateUniqueCategoryName(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new BusinessException("Category already exists with name: " + name);
        }
    }
}