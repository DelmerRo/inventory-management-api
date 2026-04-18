package com.utama.my_inventory.mapper;

import com.utama.my_inventory.dtos.request.SupplierRequestDTO;
import com.utama.my_inventory.dtos.response.SupplierResponseDTO;
import com.utama.my_inventory.dtos.response.SupplierSummaryResponseDTO;
import com.utama.my_inventory.entities.Supplier;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    // Entity to ResponseDTO
    @Mapping(target = "productCount", expression = "java(supplier.countActiveProducts())")
    SupplierResponseDTO toResponseDTO(Supplier supplier);

    List<SupplierResponseDTO> toResponseDTOList(List<Supplier> suppliers);

    // Entity to SummaryDTO
    @Mapping(target = "productCount", expression = "java(supplier.countActiveProducts())")
    SupplierSummaryResponseDTO toSummaryDTO(Supplier supplier);

    List<SupplierSummaryResponseDTO> toSummaryDTOList(List<Supplier> suppliers);

    // RequestDTO to Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "productSuppliers", ignore = true)  // ← CAMBIADO: products → productSuppliers
    @Mapping(target = "registeredAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Supplier toEntity(SupplierRequestDTO dto);

    // Update Entity from DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "productSuppliers", ignore = true)  // ← CAMBIADO: products → productSuppliers
    @Mapping(target = "registeredAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(SupplierRequestDTO dto, @MappingTarget Supplier supplier);
}