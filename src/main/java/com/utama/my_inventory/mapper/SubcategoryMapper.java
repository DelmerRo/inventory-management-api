package com.utama.my_inventory.mapper;

import com.utama.my_inventory.dtos.request.SubcategoryRequestDTO;
import com.utama.my_inventory.dtos.response.SubcategoryResponseDTO;
import com.utama.my_inventory.entities.Subcategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface SubcategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Subcategory toEntity(SubcategoryRequestDTO dto);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "productCount", source = "products", qualifiedByName = "countProducts")
    SubcategoryResponseDTO toResponseDTO(Subcategory subcategory);

    List<SubcategoryResponseDTO> toResponseDTOList(List<Subcategory> subcategories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDTO(SubcategoryRequestDTO dto, @MappingTarget Subcategory subcategory);

    @Named("countProducts")
    default Integer countProducts(List<?> products) {
        return products != null ? products.size() : 0;
    }
}