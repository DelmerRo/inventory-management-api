package com.utama.my_inventory.mapper;

import com.utama.my_inventory.dtos.request.supply.SupplyRequestDTO;
import com.utama.my_inventory.dtos.request.supply.SupplyResponseDTO;
import com.utama.my_inventory.entities.Supply;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SupplyMapper {
    SupplyResponseDTO toResponseDTO(Supply supply);
    List<SupplyResponseDTO> toResponseDTOList(List<Supply> supplies);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "currentStock", source = "initialStock", defaultValue = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Supply toEntity(SupplyRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "currentStock", ignore = true) // El stock solo se modifica por movimientos
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(SupplyRequestDTO dto, @MappingTarget Supply supply);
}