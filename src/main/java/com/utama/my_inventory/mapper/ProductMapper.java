package com.utama.my_inventory.mapper;

import com.utama.my_inventory.dtos.request.ProductRequestDTO;
import com.utama.my_inventory.dtos.response.ProductResponseDTO;
import com.utama.my_inventory.dtos.response.ProductSummaryResponseDTO;
import com.utama.my_inventory.entities.Product;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {SubcategoryMapper.class, SupplierMapper.class}
)
public interface ProductMapper {

    // Entity to ResponseDTO
    @Mapping(target = "margin", expression = "java(product.calculateMargin())")
    @Mapping(target = "marginPercentage", expression = "java(product.calculateMarginPercentage())")
    @Mapping(target = "volume", expression = "java(product.calculateVolume())")
    @Mapping(target = "hasStock", expression = "java(product.hasStock())")
    @Mapping(target = "lowStock", expression = "java(product.isLowStock(10))")
    ProductResponseDTO toResponseDTO(Product product);

    List<ProductResponseDTO> toResponseDTOList(List<Product> products);

    // Entity to SummaryDTO
    @Mapping(target = "subcategoryName", source = "subcategory.name")
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "hasStock", expression = "java(product.getCurrentStock() > 0)")
    ProductSummaryResponseDTO toSummaryDTO(Product product);

    List<ProductSummaryResponseDTO> toSummaryDTOList(List<Product> products);

    // RequestDTO to Entity (sin relaciones)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subcategory", ignore = true)  // Se manejará en el servicio
    @Mapping(target = "supplier", ignore = true)     // Se manejará en el servicio
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "inventoryMovements", ignore = true)
    @Mapping(target = "multimediaFiles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastPurchaseAt", ignore = true)
    @Mapping(target = "currentStock", source = "currentStock", defaultValue = "0")
    @Mapping(target = "measureUnit", source = "measureUnit", defaultValue = "cm")
    Product toEntity(ProductRequestDTO dto);

    // Update Entity from DTO (sin relaciones)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subcategory", ignore = true)  // Se manejará en el servicio
    @Mapping(target = "supplier", ignore = true)     // Se manejará en el servicio
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "inventoryMovements", ignore = true)
    @Mapping(target = "multimediaFiles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastPurchaseAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(ProductRequestDTO dto, @MappingTarget Product product);
}