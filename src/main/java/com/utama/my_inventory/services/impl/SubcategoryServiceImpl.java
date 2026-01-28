package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.SubcategoryRequestDTO;
import com.utama.my_inventory.dtos.response.SubcategoryResponseDTO;
import com.utama.my_inventory.entities.Category;
import com.utama.my_inventory.entities.Subcategory;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.mapper.SubcategoryMapper;
import com.utama.my_inventory.repositories.CategoryRepository;
import com.utama.my_inventory.repositories.SubcategoryRepository;
import com.utama.my_inventory.services.CategoryService;
import com.utama.my_inventory.services.SubcategoryService;
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
public class SubcategoryServiceImpl implements SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryMapper subcategoryMapper;
    private final CategoryService categoryService; // Inyección de otro servicio

    @Override
    @Transactional
    @CacheEvict(value = "subcategories", allEntries = true)
    public SubcategoryResponseDTO createSubcategory(SubcategoryRequestDTO requestDTO) {
        log.info("Creating new subcategory: {}", requestDTO.name());

        Category category = findActiveCategoryById(requestDTO.categoryId());
        validateUniqueSubcategoryName(requestDTO.name(), category.getId());

        Subcategory subcategory = subcategoryMapper.toEntity(requestDTO);
        subcategory.setCategory(category);

        Subcategory savedSubcategory = subcategoryRepository.save(subcategory);

        log.info("Subcategory created with ID: {}", savedSubcategory.getId());
        return subcategoryMapper.toResponseDTO(savedSubcategory);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "subcategories", key = "'all'")
    public List<SubcategoryResponseDTO> getAllSubcategories() {
        log.info("Retrieving all subcategories");
        List<Subcategory> subcategories = subcategoryRepository.findAll();
        return subcategoryMapper.toResponseDTOList(subcategories);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "subcategories", key = "#categoryId")
    public List<SubcategoryResponseDTO> getSubcategoriesByCategoryId(Long categoryId) {
        log.info("Retrieving subcategories for category ID: {}", categoryId);

        // Verificar que la categoría existe y está activa
        categoryService.getCategoryById(categoryId); // Reutiliza la lógica existente

        List<Subcategory> subcategories = subcategoryRepository.findByCategoryId(categoryId);
        return subcategoryMapper.toResponseDTOList(subcategories);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "subcategories", key = "#id")
    public SubcategoryResponseDTO getSubcategoryById(Long id) {
        log.info("Retrieving subcategory with ID: {}", id);
        Subcategory subcategory = findSubcategoryById(id);
        return subcategoryMapper.toResponseDTO(subcategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subcategories", key = "#id")
    public SubcategoryResponseDTO updateSubcategory(Long id, SubcategoryRequestDTO requestDTO) {
        log.info("Updating subcategory with ID: {}", id);

        Subcategory subcategory = findSubcategoryById(id);
        Category category = findActiveCategoryById(requestDTO.categoryId());

        if (!subcategory.getName().equals(requestDTO.name()) ||
                !subcategory.getCategory().getId().equals(requestDTO.categoryId())) {
            validateUniqueSubcategoryName(requestDTO.name(), requestDTO.categoryId());
        }

        subcategoryMapper.updateEntityFromDTO(requestDTO, subcategory);
        subcategory.setCategory(category);

        Subcategory updatedSubcategory = subcategoryRepository.save(subcategory);

        log.info("Subcategory updated with ID: {}", id);
        return subcategoryMapper.toResponseDTO(updatedSubcategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subcategories", key = "#id")
    public void deleteSubcategory(Long id) {
        log.info("Deleting subcategory with ID: {}", id);

        Subcategory subcategory = findSubcategoryById(id);

        if (!subcategory.getProducts().isEmpty()) {
            throw new BusinessException(
                    "Cannot delete subcategory '%s' because it has associated products"
                            .formatted(subcategory.getName())
            );
        }

        subcategoryRepository.delete(subcategory);
        log.info("Subcategory deleted with ID: {}", id);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Subcategory findSubcategoryById(Long id) {
        return subcategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subcategory not found with ID: " + id
                ));
    }

    private Category findActiveCategoryById(Long id) {
        return categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found or inactive with ID: " + id
                ));
    }

    private void validateUniqueSubcategoryName(String name, Long categoryId) {
        if (subcategoryRepository.existsByCategoryIdAndName(categoryId, name)) {
            throw new BusinessException(
                    "Subcategory already exists with name '%s' in this category".formatted(name)
            );
        }
    }
}