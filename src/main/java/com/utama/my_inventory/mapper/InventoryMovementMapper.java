package com.utama.my_inventory.mapper;

import com.utama.my_inventory.dtos.request.inventory.InventoryMovementRequestDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryMovementResponseDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryOperationResponseDTO;
import com.utama.my_inventory.entities.InventoryMovement;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface InventoryMovementMapper {

    // Entity to ResponseDTO
    @Mapping(target = "product", source = "product")
    @Mapping(target = "totalValue", expression = "java(movement.getTotalValue())")
    InventoryMovementResponseDTO toResponseDTO(InventoryMovement movement);

    // RequestDTO to Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "movementDate", ignore = true)
    InventoryMovement toEntity(InventoryMovementRequestDTO dto);

    // To OperationResponseDTO (sin stockBefore/stockAfter porque no existen en la entidad)
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "sku", source = "product.sku")
    @Mapping(target = "operationType", source = "movementType")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "operationDate", source = "movementDate")
    @Mapping(target = "totalValue", expression = "java(movement.getTotalValue())")
    // stockBefore y stockAfter se ignoran aquí, se pasan como parámetros en el método default
    @Mapping(target = "stockBefore", ignore = true)
    @Mapping(target = "stockAfter", ignore = true)
    @Mapping(target = "message", ignore = true)
    InventoryOperationResponseDTO toOperationResponseDTO(InventoryMovement movement);

    // Método con parámetros para stockBefore y stockAfter
    default InventoryOperationResponseDTO toOperationResponseDTOWithStock(
            InventoryMovement movement,
            Integer stockBefore,
            Integer stockAfter,
            String message) {

        return InventoryOperationResponseDTO.builder()
                .movementId(movement.getId())
                .productId(movement.getProduct().getId())
                .productName(movement.getProduct().getName())
                .sku(movement.getProduct().getSku())
                .operationType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .stockBefore(stockBefore)
                .stockAfter(stockAfter)
                .unitCost(movement.getUnitCost())
                .totalValue(movement.getTotalValue())
                .reason(movement.getReason())
                .operationDate(movement.getMovementDate())
                .message(message)
                .build();
    }
}