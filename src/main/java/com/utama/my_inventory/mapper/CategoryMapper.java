package com.utama.my_inventory.mapper;

import com.utama.my_inventory.dtos.request.CategoryRequestDTO;
import com.utama.my_inventory.dtos.response.CategoryResponseDTO;
import com.utama.my_inventory.entities.Category;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subcategories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    Category toEntity(CategoryRequestDTO dto);

    @Named("full")
    @Mapping(target = "subcategoryCount", source = "subcategories", qualifiedByName = "countSubcategories")
    CategoryResponseDTO toResponseDTO(Category category);

    @Named("simple")
    @Mapping(target = "subcategoryCount", source = "subcategories", qualifiedByName = "countSubcategories")
    @Mapping(target = "subcategories", ignore = true)
    CategoryResponseDTO toSimpleResponseDTO(Category category);

    @IterableMapping(qualifiedByName = "full")
    List<CategoryResponseDTO> toResponseDTOList(List<Category> categories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subcategories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDTO(CategoryRequestDTO dto, @MappingTarget Category category);

    @Named("countSubcategories")
    default Integer countSubcategories(List<?> subcategories) {
        return subcategories != null ? subcategories.size() : 0;
    }
}

