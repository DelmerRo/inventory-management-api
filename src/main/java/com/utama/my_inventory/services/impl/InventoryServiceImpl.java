package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.inventory.InventoryMovementRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockAdjustmentRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockEntryRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockExitRequestDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryMovementResponseDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryOperationResponseDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryStatisticsResponseDTO;
import com.utama.my_inventory.entities.InventoryMovement;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.enums.MovementType;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.mapper.InventoryMovementMapper;
import com.utama.my_inventory.repositories.InventoryMovementRepository;
import com.utama.my_inventory.repositories.ProductRepository;
import com.utama.my_inventory.services.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementMapper movementMapper;

    @Override
    @Transactional
    public InventoryOperationResponseDTO registerMovement(InventoryMovementRequestDTO requestDTO, String currentUser) {
        log.info("Registering inventory movement for product ID: {}, type: {}",
                requestDTO.productId(), requestDTO.movementType());

        Product product = findActiveProductById(requestDTO.productId());
        String user = requestDTO.registeredBy() != null ? requestDTO.registeredBy() : currentUser;

        if (user == null || user.trim().isEmpty()) {
            throw new BusinessException("Usuario registrador es obligatorio");
        }

        // Validar stock para salidas
        if (requestDTO.movementType() == MovementType.SALIDA &&
                product.getCurrentStock() < requestDTO.quantity()) {
            throw new BusinessException(
                    "Stock insuficiente. Disponible: " + product.getCurrentStock() +
                            ", Solicitado: " + requestDTO.quantity()
            );
        }

        // Validar ajustes
        if (requestDTO.movementType() == MovementType.AJUSTE &&
                (requestDTO.reason() == null || requestDTO.reason().trim().isEmpty())) {
            throw new BusinessException("El motivo es obligatorio para ajustes de inventario");
        }

        int stockBefore = product.getCurrentStock();

        // Crear movimiento
        InventoryMovement movement = InventoryMovement.builder()
                .product(product)
                .quantity(requestDTO.quantity())
                .movementType(requestDTO.movementType())
                .reason(requestDTO.reason())
                .registeredBy(user)
                .unitCost(requestDTO.unitCost())
                .movementDate(LocalDateTime.now())
                .build();

        // Actualizar stock según tipo de movimiento
        updateProductStock(product, movement);

        // Guardar movimiento
        InventoryMovement savedMovement = movementRepository.save(movement);
        Product updatedProduct = productRepository.save(product);

        log.info("Movement registered with ID: {}. Product stock updated from {} to {}",
                savedMovement.getId(), stockBefore, updatedProduct.getCurrentStock());

        return movementMapper.toOperationResponseDTOWithStock(
                savedMovement,
                stockBefore,
                updatedProduct.getCurrentStock(),
                getMovementMessage(savedMovement.getMovementType(), stockBefore, updatedProduct.getCurrentStock())
        );
    }

    @Override
    @Transactional
    public InventoryOperationResponseDTO registerEntry(StockEntryRequestDTO requestDTO, String currentUser) {
        log.info("Registering stock entry for product ID: {}, quantity: {}",
                requestDTO.productId(), requestDTO.quantity());

        InventoryMovementRequestDTO movementDTO = new InventoryMovementRequestDTO(
                requestDTO.productId(),
                requestDTO.quantity(),
                MovementType.ENTRADA,
                requestDTO.reason(),
                currentUser,
                requestDTO.unitCost()
        );

        return registerMovement(movementDTO, currentUser);
    }

    @Override
    @Transactional
    public InventoryOperationResponseDTO registerExit(StockExitRequestDTO requestDTO, String currentUser) {
        log.info("Registering stock exit for product ID: {}, quantity: {}",
                requestDTO.productId(), requestDTO.quantity());

        InventoryMovementRequestDTO movementDTO = new InventoryMovementRequestDTO(
                requestDTO.productId(),
                requestDTO.quantity(),
                MovementType.SALIDA,
                requestDTO.reason(),
                currentUser,
                null
        );

        return registerMovement(movementDTO, currentUser);
    }

    @Override
    @Transactional
    public InventoryOperationResponseDTO registerAdjustment(StockAdjustmentRequestDTO requestDTO, String currentUser) {
        log.info("Registering stock adjustment for product ID: {}, type: {}",
                requestDTO.productId(), requestDTO.adjustmentType());

        Product product = findActiveProductById(requestDTO.productId());
        MovementType movementType;
        int quantity;

        if (requestDTO.newStock() != null) {
            // Ajuste por nuevo stock objetivo
            int difference = requestDTO.newStock() - product.getCurrentStock();

            if (difference == 0) {
                throw new BusinessException("El stock actual ya es igual al stock objetivo");
            }

            movementType = difference > 0 ? MovementType.ENTRADA : MovementType.SALIDA;
            quantity = Math.abs(difference);
        } else if (requestDTO.adjustmentQuantity() != null && requestDTO.adjustmentType() != null) {
            // Ajuste por cantidad específica
            movementType = MovementType.valueOf(requestDTO.adjustmentType().toUpperCase());
            quantity = requestDTO.adjustmentQuantity();

            // Validar stock para salidas
            if (movementType == MovementType.SALIDA && product.getCurrentStock() < quantity) {
                throw new BusinessException(
                        "Stock insuficiente para ajuste. Disponible: " + product.getCurrentStock() +
                                ", Ajuste solicitado: " + quantity
                );
            }
        } else {
            throw new BusinessException("Debe especificar nuevo stock o cantidad de ajuste");
        }

        InventoryMovementRequestDTO movementDTO = new InventoryMovementRequestDTO(
                requestDTO.productId(),
                quantity,
                MovementType.AJUSTE,
                requestDTO.reason() + " (Ajuste: " + quantity + " unidades)",
                currentUser,
                null
        );

        return registerMovement(movementDTO, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> getProductHistory(Long productId) {
        log.info("Retrieving inventory history for product ID: {}", productId);

        // Verificar que el producto existe
        findActiveProductById(productId);

        List<InventoryMovement> movements = movementRepository.findByProductIdOrderByMovementDateDesc(productId);
        return movements.stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> getAllMovements() {
        log.info("Retrieving all inventory movements");

        List<InventoryMovement> movements = movementRepository.findAllByOrderByMovementDateDesc();
        return movements.stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> searchMovements(Long productId, String movementType,
                                                              String registeredBy, LocalDateTime startDate,
                                                              LocalDateTime endDate) {
        log.info("Searching movements with filters - productId: {}, movementType: {}, registeredBy: {}",
                productId, movementType, registeredBy);

        MovementType type = null;
        if (movementType != null && !movementType.trim().isEmpty()) {
            try {
                type = MovementType.valueOf(movementType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Tipo de movimiento inválido: " + movementType);
            }
        }

        List<InventoryMovement> movements = movementRepository.searchMovements(
                productId, type, registeredBy, startDate, endDate);

        return movements.stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> getMovementsByType(String movementType) {
        log.info("Retrieving movements by type: {}", movementType);

        MovementType type = MovementType.valueOf(movementType.toUpperCase());
        List<InventoryMovement> movements = movementRepository.findByMovementTypeOrderByMovementDateDesc(type);

        return movements.stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> getMovementsByUser(String username) {
        log.info("Retrieving movements by user: {}", username);

        List<InventoryMovement> movements = movementRepository.findByRegisteredByOrderByMovementDateDesc(username);
        return movements.stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Retrieving movements between {} and {}", startDate, endDate);

        List<InventoryMovement> movements = movementRepository.findByMovementDateBetweenOrderByMovementDateDesc(
                startDate, endDate);

        return movements.stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalEntriesByProduct(Long productId) {
        Integer total = movementRepository.getTotalEntriesByProduct(productId);
        return total != null ? total : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalExitsByProduct(Long productId) {
        Integer total = movementRepository.getTotalExitsByProduct(productId);
        return total != null ? total : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getMovementCountByProduct(Long productId) {
        return movementRepository.countMovementsByProduct(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyMovementSummary(int year, int month) {
        log.info("Generating monthly movement summary for {}/{}", month, year);

        // Implementar consulta personalizada si es necesario
        // Por ahora retornamos una lista vacía
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getProductMovementSummary(Long productId) {
        log.info("Generating product movement summary for ID: {}", productId);

        // Implementar consulta personalizada si es necesario
        // Por ahora retornamos una lista vacía
        return List.of();
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Product findActiveProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found or inactive with ID: " + id
                ));
    }

    private void updateProductStock(Product product, InventoryMovement movement) {
        int currentStock = product.getCurrentStock();

        switch (movement.getMovementType()) {
            case ENTRADA:
                product.setCurrentStock(currentStock + movement.getQuantity());
                break;
            case SALIDA:
                product.setCurrentStock(currentStock - movement.getQuantity());
                break;
            case AJUSTE:
                // Para ajustes, la cantidad es absoluta y el tipo determina si suma o resta
                product.setCurrentStock(currentStock + movement.getQuantity());
                break;
        }
    }

    private String getMovementMessage(MovementType type, int stockBefore, int stockAfter) {
        int difference = stockAfter - stockBefore;

        switch (type) {
            case ENTRADA:
                return String.format("Entrada registrada. Stock aumentó de %d a %d (+%d)",
                        stockBefore, stockAfter, difference);
            case SALIDA:
                return String.format("Salida registrada. Stock disminuyó de %d a %d (-%d)",
                        stockBefore, stockAfter, Math.abs(difference));
            case AJUSTE:
                if (difference > 0) {
                    return String.format("Ajuste positivo registrado. Stock aumentó de %d a %d (+%d)",
                            stockBefore, stockAfter, difference);
                } else {
                    return String.format("Ajuste negativo registrado. Stock disminuyó de %d a %d (-%d)",
                            stockBefore, stockAfter, Math.abs(difference));
                }
            default:
                return "Movimiento registrado exitosamente";
        }
    }

    private List<InventoryMovement> findAllByOrderByMovementDateDesc() {
        return movementRepository.findAll().stream()
                .sorted((m1, m2) -> m2.getMovementDate().compareTo(m1.getMovementDate()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getProductStatistics(Long productId) {
        log.info("Getting statistics for product ID: {}", productId);

        Integer totalEntries = getTotalEntriesByProduct(productId);
        Integer totalExits = getTotalExitsByProduct(productId);
        Long movementCount = getMovementCountByProduct(productId);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("productId", productId);
        statistics.put("totalEntries", totalEntries);
        statistics.put("totalExits", totalExits);
        statistics.put("movementCount", movementCount);
        statistics.put("netMovement", totalEntries - totalExits);

        return statistics;
    }
}