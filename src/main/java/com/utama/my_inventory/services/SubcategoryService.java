package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.SubcategoryRequestDTO;
import com.utama.my_inventory.dtos.response.SubcategoryResponseDTO;
import com.utama.my_inventory.entities.Category;
import com.utama.my_inventory.entities.Subcategory;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.mapper.SubcategoryMapper;
import com.utama.my_inventory.repositories.CategoryRepository;
import com.utama.my_inventory.repositories.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryMapper subcategoryMapper;

    @Transactional
    public SubcategoryResponseDTO createSubcategory(SubcategoryRequestDTO requestDTO) {
        log.info("Creando nueva subcategoría: {}", requestDTO.name());

        Category category = findActiveCategoryById(requestDTO.categoryId());
        validateUniqueSubcategoryName(requestDTO.name(), category.getId());

        Subcategory subcategory = subcategoryMapper.toEntity(requestDTO);
        subcategory.setCategory(category);

        Subcategory savedSubcategory = subcategoryRepository.save(subcategory);

        log.info("Subcategoría creada con ID: {}", savedSubcategory.getId());
        return subcategoryMapper.toResponseDTO(savedSubcategory);
    }

    @Transactional(readOnly = true)
    public List<SubcategoryResponseDTO> getAllSubcategories() {
        log.info("Obteniendo todas las subcategorías");
        List<Subcategory> subcategories = subcategoryRepository.findAll();
        return subcategoryMapper.toResponseDTOList(subcategories);
    }

    @Transactional(readOnly = true)
    public List<SubcategoryResponseDTO> getSubcategoriesByCategoryId(Long categoryId) {
        log.info("Obteniendo subcategorías para categoría ID: {}", categoryId);

        findActiveCategoryById(categoryId); // Validar que la categoría existe

        List<Subcategory> subcategories = subcategoryRepository.findByCategoryId(categoryId);
        return subcategoryMapper.toResponseDTOList(subcategories);
    }

    @Transactional(readOnly = true)
    public SubcategoryResponseDTO getSubcategoryById(Long id) {
        log.info("Obteniendo subcategoría con ID: {}", id);
        Subcategory subcategory = findSubcategoryById(id);
        return subcategoryMapper.toResponseDTO(subcategory);
    }

    @Transactional
    public SubcategoryResponseDTO updateSubcategory(Long id, SubcategoryRequestDTO requestDTO) {
        log.info("Actualizando subcategoría con ID: {}", id);

        Subcategory subcategory = findSubcategoryById(id);
        Category category = findActiveCategoryById(requestDTO.categoryId());

        if (!subcategory.getName().equals(requestDTO.name()) ||
                !subcategory.getCategory().getId().equals(requestDTO.categoryId())) {
            validateUniqueSubcategoryName(requestDTO.name(), requestDTO.categoryId());
        }

        subcategoryMapper.updateEntityFromDTO(requestDTO, subcategory);
        subcategory.setCategory(category);

        Subcategory updatedSubcategory = subcategoryRepository.save(subcategory);

        log.info("Subcategoría actualizada con ID: {}", id);
        return subcategoryMapper.toResponseDTO(updatedSubcategory);
    }

    @Transactional
    public void deleteSubcategory(Long id) {
        log.info("Eliminando subcategoría con ID: {}", id);

        Subcategory subcategory = findSubcategoryById(id);

        if (!subcategory.getProducts().isEmpty()) {
            throw new BusinessException(
                    String.format("No se puede eliminar la subcategoría '%s' porque tiene productos asociados",
                            subcategory.getName())
            );
        }

        subcategoryRepository.delete(subcategory);
        log.info("Subcategoría eliminada con ID: {}", id);
    }

    private Subcategory findSubcategoryById(Long id) {
        return subcategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategoría no encontrada"));
    }

    private Category findActiveCategoryById(Long id) {
        return categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada o inactiva"));
    }

    private void validateUniqueSubcategoryName(String name, Long categoryId) {
        if (subcategoryRepository.existsByCategoryIdAndName(categoryId, name)) {
            throw new BusinessException(
                    String.format("Ya existe una subcategoría con el nombre '%s' en esta categoría", name)
            );
        }
    }
}