package com.utama.my_inventory.services.impl;


import com.utama.my_inventory.dtos.request.supply.SupplyMovementRequestDTO;
import com.utama.my_inventory.dtos.request.supply.SupplyRequestDTO;
import com.utama.my_inventory.dtos.request.supply.SupplyResponseDTO;
import com.utama.my_inventory.dtos.response.supply.SupplyMovementResponseDTO;
import com.utama.my_inventory.entities.Supply;
import com.utama.my_inventory.entities.SupplyMovement;
import com.utama.my_inventory.entities.enums.MovementType;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.mapper.SupplyMapper;
import com.utama.my_inventory.repositories.SupplyMovementRepository;
import com.utama.my_inventory.repositories.SupplyRepository;
import com.utama.my_inventory.services.SupplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplyServiceImpl implements SupplyService {

    private final SupplyRepository supplyRepository;
    private final SupplyMovementRepository supplyMovementRepository;
    private final SupplyMapper supplyMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SupplyResponseDTO> getAllActiveSupplies() {
        return supplyMapper.toResponseDTOList(supplyRepository.findByActiveTrueOrderByNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public SupplyResponseDTO getSupplyById(Long id) {
        Supply supply = findSupplyEntityById(id);
        return supplyMapper.toResponseDTO(supply);
    }

    @Override
    @Transactional
    public SupplyResponseDTO createSupply(SupplyRequestDTO dto) {
        if (supplyRepository.existsByNameIgnoreCase(dto.name())) {
            throw new BusinessException("Ya existe un insumo con el nombre: " + dto.name());
        }
        Supply supply = supplyMapper.toEntity(dto);
        if (supply.getCurrentStock() == null) supply.setCurrentStock(0);

        Supply savedSupply = supplyRepository.save(supply);
        log.info("Insumo creado: {}", savedSupply.getName());
        return supplyMapper.toResponseDTO(savedSupply);
    }

    @Override
    @Transactional
    public SupplyResponseDTO updateSupply(Long id, SupplyRequestDTO dto) {
        Supply supply = findSupplyEntityById(id);

        if (!supply.getName().equalsIgnoreCase(dto.name()) && supplyRepository.existsByNameIgnoreCase(dto.name())) {
            throw new BusinessException("Ya existe otro insumo con el nombre: " + dto.name());
        }

        supplyMapper.updateEntityFromDTO(dto, supply);
        Supply updatedSupply = supplyRepository.save(supply);
        log.info("Insumo actualizado: {}", updatedSupply.getName());
        return supplyMapper.toResponseDTO(updatedSupply);
    }

    @Override
    @Transactional
    public void deactivateSupply(Long id) {
        Supply supply = findSupplyEntityById(id);
        supply.setActive(false);
        supplyRepository.save(supply);
        log.info("Insumo desactivado: {}", supply.getName());
    }

    @Override
    @Transactional
    public SupplyMovementResponseDTO registerMovement(Long supplyId, SupplyMovementRequestDTO dto) {
        Supply supply = findSupplyEntityById(supplyId);
        MovementType type;

        try {
            type = MovementType.valueOf(dto.movementType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tipo de movimiento inválido. Use ENTRADA, SALIDA o AJUSTE.");
        }

        int oldStock = supply.getCurrentStock();
        int newStock = oldStock;

        // Lógica de cálculo de stock
        switch (type) {
            case ENTRADA -> newStock = oldStock + dto.quantity();
            case SALIDA -> {
                if (oldStock < dto.quantity()) {
                    throw new BusinessException("Stock insuficiente. Actual: " + oldStock + ", Requerido: " + dto.quantity());
                }
                newStock = oldStock - dto.quantity();
            }
            case AJUSTE -> {
                if (dto.reason() == null || dto.reason().isBlank()) {
                    throw new BusinessException("Los movimientos de AJUSTE requieren un motivo (reason).");
                }
                newStock = dto.quantity(); // En Ajuste, la cantidad enviada es el stock final real
            }
        }

        supply.setCurrentStock(newStock);

        // Si mandan un nuevo costo en una ENTRADA, lo actualizamos
        BigDecimal unitCost = dto.unitCost() != null ? dto.unitCost() : supply.getUnitCost();
        if (type == MovementType.ENTRADA && dto.unitCost() != null) {
            supply.setUnitCost(dto.unitCost());
        }

        supplyRepository.save(supply);

        // La cantidad a registrar en el historial es la diferencia real que ocurrió
        int quantityToRecord = type == MovementType.AJUSTE ? Math.abs(newStock - oldStock) : dto.quantity();

        SupplyMovement movement = SupplyMovement.builder()
                .supply(supply)
                .movementType(type)
                .quantity(quantityToRecord)
                .unitCost(unitCost)
                .reason(dto.reason())
                .registeredBy("ADMIN") // Hardcodeado según indicación
                .build();

        SupplyMovement savedMovement = supplyMovementRepository.save(movement);
        log.info("Movimiento de {} registrado en insumo {}. Nuevo stock: {}", type, supply.getName(), newStock);

        return mapToMovementResponse(savedMovement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplyMovementResponseDTO> getMovementsBySupply(Long supplyId) {
        findSupplyEntityById(supplyId); // Validar que existe
        return supplyMovementRepository.findTop50BySupplyIdOrderByMovementDateDesc(supplyId)
                .stream()
                .map(this::mapToMovementResponse)
                .collect(Collectors.toList());
    }

    // --- Helpers Privados ---
    private Supply findSupplyEntityById(Long id) {
        return supplyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado con ID: " + id));
    }

    private SupplyMovementResponseDTO mapToMovementResponse(SupplyMovement movement) {
        return new SupplyMovementResponseDTO(
                movement.getId(),
                movement.getSupply().getId(),
                movement.getSupply().getName(),
                movement.getQuantity(),
                movement.getMovementType().name(),
                movement.getReason(),
                movement.getUnitCost(),
                movement.getTotalValue(),
                movement.getMovementDate(),
                movement.getRegisteredBy()
        );
    }
}