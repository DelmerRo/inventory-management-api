package com.utama.my_inventory.mapper;

import com.utama.my_inventory.dtos.request.ProductRequestDTO;
import com.utama.my_inventory.dtos.response.product.ProductDetailResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductResponseDTO;
import com.utama.my_inventory.dtos.response.product.ProductSummaryResponseDTO;
import com.utama.my_inventory.dtos.response.SubcategoryEssentialsDTO;
import com.utama.my_inventory.dtos.response.SupplierAssociationResponseDTO;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.ProductSupplier;
import com.utama.my_inventory.entities.Subcategory;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {SubcategoryMapper.class}
)
public interface ProductMapper {

    // ========== PARA DETALLES COMPLETOS (PRODUCTRESPONSEDTO) ==========
    @Mapping(target = "margin", expression = "java(product.calculateMargin())")
    @Mapping(target = "marginPercentage", expression = "java(product.calculateMarginPercentage())")
    @Mapping(target = "volume", expression = "java(product.calculateVolume())")
    @Mapping(target = "hasStock", expression = "java(product.hasStock())")
    @Mapping(target = "lowStock", expression = "java(product.isLowStock(10))")
    @Mapping(target = "suppliers", source = "productSuppliers", qualifiedByName = "mapProductSuppliers")
    @Mapping(target = "imageUrl", expression = "java(getFirstImageUrl(product))")
    ProductResponseDTO toResponseDTO(Product product);

    List<ProductResponseDTO> toResponseDTOList(List<Product> products);

    // ========== PARA DETALLES ESENCIALES ==========
    @Mapping(target = "margin", expression = "java(product.calculateMargin())")
    @Mapping(target = "marginPercentage", expression = "java(product.calculateMarginPercentage())")
    @Mapping(target = "volume", expression = "java(product.calculateVolume())")
    @Mapping(target = "hasStock", expression = "java(product.hasStock())")
    @Mapping(target = "lowStock", expression = "java(product.isLowStock(10))")
    @Mapping(target = "subcategory", source = "subcategory", qualifiedByName = "toSubcategoryEssentialsDTO")
    @Mapping(target = "suppliers", source = "productSuppliers", qualifiedByName = "mapProductSuppliers")
    @Mapping(target = "primarySupplierName", expression = "java(product.getPrimarySupplier() != null ? product.getPrimarySupplier().getName() : null)")
    @Mapping(target = "primarySupplierSku", expression = "java(product.getPrimarySupplierSku())")
    @Mapping(target = "imageUrl", expression = "java(getFirstImageUrl(product))")
    ProductDetailResponseDTO toDetailDTO(Product product);

    List<ProductDetailResponseDTO> toDetailDTOList(List<Product> products);

    // ========== PARA LISTADOS (SUMMARY) - MÉTODO MANUAL ==========
    // No usamos @Mapping para imageUrl para evitar el error

    default ProductSummaryResponseDTO toSummaryDTO(Product product) {
        String imageUrl = getFirstImageUrl(product);
        return new ProductSummaryResponseDTO(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSalePrice(),
                product.getCostPrice(),
                product.getCurrentStock(),
                product.getSubcategory() != null ? product.getSubcategory().getName() : null,
                product.getPrimarySupplier() != null ? product.getPrimarySupplier().getName() : null,
                product.getPrimarySupplierSku(),
                product.getProductSuppliers() != null ? product.getProductSuppliers().size() : 0,
                product.getCurrentStock() > 0,
                product.getActive(),
                product.getCreatedAt(),
                imageUrl
        );
    }

    default List<ProductSummaryResponseDTO> toSummaryDTOList(List<Product> products) {
        if (products == null) return List.of();
        return products.stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    // ========== MÉTODO HELPER PARA OBTENER LA PRIMERA IMAGEN ==========
    default String getFirstImageUrl(Product product) {
        if (product.getMultimediaFiles() == null || product.getMultimediaFiles().isEmpty()) {
            return null;
        }
        return product.getMultimediaFiles().get(0).getFileUrl();
    }

    // ========== MÉTODOS AUXILIARES ==========

    @Named("toSubcategoryEssentialsDTO")
    static SubcategoryEssentialsDTO toSubcategoryEssentialsDTO(Subcategory subcategory) {
        if (subcategory == null) return null;
        return SubcategoryEssentialsDTO.builder()
                .id(subcategory.getId())
                .name(subcategory.getName())
                .active(subcategory.getActive())
                .categoryId(subcategory.getCategory() != null ? subcategory.getCategory().getId() : null)
                .categoryName(subcategory.getCategory() != null ? subcategory.getCategory().getName() : null)
                .build();
    }

    @Named("mapProductSuppliers")
    static List<SupplierAssociationResponseDTO> mapProductSuppliers(List<ProductSupplier> productSuppliers) {
        if (productSuppliers == null) return List.of();
        return productSuppliers.stream()
                .map(ps -> SupplierAssociationResponseDTO.builder()
                        .id(ps.getId())
                        .supplierId(ps.getSupplier().getId())
                        .supplierName(ps.getSupplier().getName())
                        .supplierSku(ps.getSupplierSku())
                        .isPrimary(ps.getIsPrimary())
                        .notes(ps.getNotes())
                        .build())
                .toList();
    }

    // ========== REQUEST → ENTITY ==========
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subcategory", ignore = true)
    @Mapping(target = "productSuppliers", ignore = true)
    @Mapping(target = "supplierSku", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "inventoryMovements", ignore = true)
    @Mapping(target = "multimediaFiles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastPurchaseAt", ignore = true)
    @Mapping(target = "currentStock", source = "currentStock", defaultValue = "0")
    @Mapping(target = "measureUnit", source = "measureUnit", defaultValue = "cm")
    @Mapping(target = "sku", ignore = true)
    Product toEntity(ProductRequestDTO dto);

    // ========== UPDATE ENTITY FROM DTO ==========
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subcategory", ignore = true)
    @Mapping(target = "productSuppliers", ignore = true)
    @Mapping(target = "supplierSku", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "inventoryMovements", ignore = true)
    @Mapping(target = "multimediaFiles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastPurchaseAt", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(ProductRequestDTO dto, @MappingTarget Product product);
}