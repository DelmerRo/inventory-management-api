package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.CategoryRequestDTO;
import com.utama.my_inventory.dtos.response.CategoryResponseDTO;
import com.utama.my_inventory.entities.Category;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.mapper.CategoryMapper;
import com.utama.my_inventory.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO) {
        log.info("Creando nueva categoría: {}", requestDTO.name());

        validateUniqueCategoryName(requestDTO.name());

        Category category = categoryMapper.toEntity(requestDTO);
        Category savedCategory = categoryRepository.save(category);

        log.info("Categoría creada con ID: {}", savedCategory.getId());
        return categoryMapper.toResponseDTO(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategories() {
        log.info("Obteniendo todas las categorías activas");
        List<Category> categories = categoryRepository.findByActiveTrue();
        return categoryMapper.toResponseDTOList(categories);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryById(Long id) {
        log.info("Obteniendo categoría con ID: {}", id);
        Category category = findActiveCategoryById(id);
        return categoryMapper.toResponseDTO(category);
    }

    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO requestDTO) {
        log.info("Actualizando categoría con ID: {}", id);

        Category category = findActiveCategoryById(id);

        if (!category.getName().equals(requestDTO.name())) {
            validateUniqueCategoryName(requestDTO.name());
        }

        categoryMapper.updateEntityFromDTO(requestDTO, category);
        Category updatedCategory = categoryRepository.save(category);

        log.info("Categoría actualizada con ID: {}", id);
        return categoryMapper.toResponseDTO(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("Intentando eliminar categoría con ID: {}", id);

        Category category = findActiveCategoryById(id);

        if (!category.getSubcategories().isEmpty()) {
            throw new BusinessException(
                    String.format("No se puede eliminar la categoría '%s' porque tiene %d subcategorías asociadas",
                            category.getName(), category.getSubcategories().size())
            );
        }

        categoryRepository.delete(category);
        log.info("Categoría eliminada con ID: {}", id);
    }

    @Transactional
    public CategoryResponseDTO toggleCategoryStatus(Long id) {
        log.info("Cambiando estado de categoría con ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        category.setActive(!category.getActive());
        Category updatedCategory = categoryRepository.save(category);

        log.info("Estado de categoría cambiado a: {}", updatedCategory.getActive());
        return categoryMapper.toResponseDTO(updatedCategory);
    }

    private Category findActiveCategoryById(Long id) {
        return categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada o inactiva"));
    }

    private void validateUniqueCategoryName(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new BusinessException("Ya existe una categoría con el nombre: " + name);
        }
    }
}