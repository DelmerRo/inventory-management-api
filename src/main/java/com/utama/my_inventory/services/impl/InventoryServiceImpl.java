package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.MovementFilter;
import com.utama.my_inventory.dtos.request.inventory.InventoryMovementRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockAdjustmentRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockEntryRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockExitRequestDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryMovementResponseDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryOperationResponseDTO;
import com.utama.my_inventory.entities.InventoryMovement;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.enums.MovementType;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.mapper.InventoryMovementMapper;
import com.utama.my_inventory.repositories.InventoryMovementRepository;
import com.utama.my_inventory.repositories.ProductRepository;
import com.utama.my_inventory.services.InventoryService;
import com.utama.my_inventory.utils.InventoryMovementSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
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

    // ==================== REGISTRO DE MOVIMIENTOS ====================

    @Override
    @Transactional
    public InventoryOperationResponseDTO registerMovement(InventoryMovementRequestDTO requestDTO, String currentUser) {
        log.info("Registering inventory movement - Product ID: {}, Type: {}, Quantity: {}",
                requestDTO.productId(), requestDTO.movementType(), requestDTO.quantity());

        Product product = findActiveProductById(requestDTO.productId());
        String user = resolveUser(requestDTO.registeredBy(), currentUser);

        validateMovement(requestDTO, product);

        int stockBefore = product.getCurrentStock();
        InventoryMovement movement = buildMovement(requestDTO, product, user);
        updateProductStock(product, movement);

        InventoryMovement savedMovement = movementRepository.save(movement);
        Product updatedProduct = productRepository.save(product);

        logMovementResult(savedMovement, stockBefore, updatedProduct.getCurrentStock());

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
        log.info("Registering stock entry - Product ID: {}, Quantity: {}",
                requestDTO.productId(), requestDTO.quantity());

        InventoryMovementRequestDTO movementDTO = InventoryMovementRequestDTO.builder()
                .productId(requestDTO.productId())
                .quantity(requestDTO.quantity())
                .movementType(MovementType.ENTRADA)
                .reason(requestDTO.reason())
                .registeredBy(currentUser)
                .unitCost(requestDTO.unitCost())
                .build();

        return registerMovement(movementDTO, currentUser);
    }

    @Override
    @Transactional
    public InventoryOperationResponseDTO registerExit(StockExitRequestDTO requestDTO, String currentUser) {
        log.info("Registering stock exit - Product ID: {}, Quantity: {}",
                requestDTO.productId(), requestDTO.quantity());

        InventoryMovementRequestDTO movementDTO = InventoryMovementRequestDTO.builder()
                .productId(requestDTO.productId())
                .quantity(requestDTO.quantity())
                .movementType(MovementType.SALIDA)
                .reason(requestDTO.reason())
                .registeredBy(currentUser)
                .build();

        return registerMovement(movementDTO, currentUser);
    }

    @Override
    @Transactional
    public InventoryOperationResponseDTO registerAdjustment(StockAdjustmentRequestDTO requestDTO, String currentUser) {
        log.info("Registering stock adjustment - Product ID: {}, Type: {}",
                requestDTO.productId(), requestDTO.adjustmentType());

        Product product = findActiveProductById(requestDTO.productId());
        AdjustmentResult adjustment = calculateAdjustment(requestDTO, product);

        InventoryMovementRequestDTO movementDTO = InventoryMovementRequestDTO.builder()
                .productId(requestDTO.productId())
                .quantity(adjustment.quantity())
                .movementType(adjustment.movementType())
                .reason(requestDTO.reason() + " (Ajuste: " + adjustment.quantity() + " unidades)")
                .registeredBy(currentUser)
                .build();

        return registerMovement(movementDTO, currentUser);
    }

    // ==================== CONSULTAS DE MOVIMIENTOS ====================

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> getProductHistory(Long productId) {
        log.info("Retrieving inventory history for product ID: {}", productId);

        verifyProductExists(productId);

        return movementRepository.findByProductIdOrderByMovementDateDesc(productId)
                .stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> getAllMovements() {
        log.info("Retrieving all inventory movements");

        return movementRepository.findAllByOrderByMovementDateDesc()
                .stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> searchMovements(MovementFilter filter) {
        log.info("Searching movements with filters");

        Specification<InventoryMovement> spec = InventoryMovementSpecification.filter(filter);

        return movementRepository.findAll(spec)
                .stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> getMovementsByType(String movementType) {
        log.info("Retrieving movements by type: {}", movementType);

        MovementType type = MovementType.valueOf(movementType.toUpperCase());

        return movementRepository.findByMovementTypeOrderByMovementDateDesc(type)
                .stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> getMovementsByUser(String username) {
        log.info("Retrieving movements by user: {}", username);

        return movementRepository.findByRegisteredByOrderByMovementDateDesc(username)
                .stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponseDTO> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Retrieving movements between {} and {}", startDate, endDate);

        return movementRepository.findByMovementDateBetweenOrderByMovementDateDesc(startDate, endDate)
                .stream()
                .map(movementMapper::toResponseDTO)
                .toList();
    }

    // ==================== ESTADÍSTICAS ====================

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
    public Map<String, Object> getProductStatistics(Long productId) {
        log.info("Getting statistics for product ID: {}", productId);

        Product product = findProductById(productId);
        Integer totalEntries = getTotalEntriesByProduct(productId);
        Integer totalExits = getTotalExitsByProduct(productId);
        Long movementCount = getMovementCountByProduct(productId);

        return buildStatisticsResponse(product, totalEntries, totalExits, movementCount);
    }

    // ==================== MÉTODOS PRIVADOS - VALIDACIONES ====================

    private Product findActiveProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or inactive with ID: " + id));
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    private void verifyProductExists(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with ID: " + id);
        }
    }

    private String resolveUser(String requestUser, String defaultUser) {
        String user = requestUser != null ? requestUser : defaultUser;
        if (user == null || user.trim().isEmpty()) {
            throw new BusinessException("El usuario registrador es obligatorio");
        }
        return user;
    }

    private void validateMovement(InventoryMovementRequestDTO requestDTO, Product product) {
        if (requestDTO.movementType() == MovementType.SALIDA &&
                product.getCurrentStock() < requestDTO.quantity()) {
            throw new BusinessException(String.format(
                    "Stock insuficiente. Disponible: %d, Solicitado: %d",
                    product.getCurrentStock(), requestDTO.quantity()));
        }

        if (requestDTO.movementType() == MovementType.AJUSTE &&
                (requestDTO.reason() == null || requestDTO.reason().trim().isEmpty())) {
            throw new BusinessException("El motivo es obligatorio para ajustes de inventario");
        }
    }

    // ==================== MÉTODOS PRIVADOS - CONSTRUCCIÓN ====================

    private InventoryMovement buildMovement(InventoryMovementRequestDTO requestDTO, Product product, String user) {
        return InventoryMovement.builder()
                .product(product)
                .quantity(requestDTO.quantity())
                .movementType(requestDTO.movementType())
                .reason(requestDTO.reason())
                .registeredBy(user)
                .unitCost(requestDTO.unitCost())
                .movementDate(LocalDateTime.now())
                .build();
    }

    private void updateProductStock(Product product, InventoryMovement movement) {
        int currentStock = product.getCurrentStock();

        switch (movement.getMovementType()) {
            case ENTRADA, AJUSTE -> product.setCurrentStock(currentStock + movement.getQuantity());
            case SALIDA -> product.setCurrentStock(currentStock - movement.getQuantity());
        }
    }

    private AdjustmentResult calculateAdjustment(StockAdjustmentRequestDTO requestDTO, Product product) {
        int currentStock = product.getCurrentStock();

        if (requestDTO.newStock() != null) {
            int difference = requestDTO.newStock() - currentStock;
            if (difference == 0) {
                throw new BusinessException("El stock actual ya es igual al stock objetivo");
            }
            MovementType type = difference > 0 ? MovementType.ENTRADA : MovementType.SALIDA;
            return new AdjustmentResult(type, Math.abs(difference));
        }

        if (requestDTO.adjustmentQuantity() != null && requestDTO.adjustmentType() != null) {
            MovementType type = MovementType.valueOf(requestDTO.adjustmentType().toUpperCase());
            int quantity = requestDTO.adjustmentQuantity();

            if (type == MovementType.SALIDA && currentStock < quantity) {
                throw new BusinessException(String.format(
                        "Stock insuficiente para ajuste. Disponible: %d, Ajuste solicitado: %d",
                        currentStock, quantity));
            }
            return new AdjustmentResult(type, quantity);
        }

        throw new BusinessException("Debe especificar nuevo stock o cantidad de ajuste");
    }

    private record AdjustmentResult(MovementType movementType, int quantity) {}

    // ==================== MÉTODOS PRIVADOS - RESPUESTAS ====================

    private String getMovementMessage(MovementType type, int stockBefore, int stockAfter) {
        int difference = stockAfter - stockBefore;

        return switch (type) {
            case ENTRADA -> String.format("Entrada registrada. Stock aumentó de %d a %d (+%d)",
                    stockBefore, stockAfter, difference);
            case SALIDA -> String.format("Salida registrada. Stock disminuyó de %d a %d (-%d)",
                    stockBefore, stockAfter, Math.abs(difference));
            case AJUSTE -> difference > 0
                    ? String.format("Ajuste positivo registrado. Stock aumentó de %d a %d (+%d)",
                    stockBefore, stockAfter, difference)
                    : String.format("Ajuste negativo registrado. Stock disminuyó de %d a %d (-%d)",
                    stockBefore, stockAfter, Math.abs(difference));
        };
    }

    private void logMovementResult(InventoryMovement movement, int stockBefore, int stockAfter) {
        log.info("✅ Movimiento registrado - ID: {}, Tipo: {}, Producto: {}, Stock: {} → {}",
                movement.getId(), movement.getMovementType(),
                movement.getProduct().getName(), stockBefore, stockAfter);
    }

    private Map<String, Object> buildStatisticsResponse(Product product, Integer entries, Integer exits, Long count) {
        int totalEntries = entries != null ? entries : 0;
        int totalExits = exits != null ? exits : 0;

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("productId", product.getId());
        statistics.put("productName", product.getName());
        statistics.put("productActive", product.getActive());
        statistics.put("currentStock", product.getCurrentStock());
        statistics.put("totalEntries", totalEntries);
        statistics.put("totalExits", totalExits);
        statistics.put("movementCount", count != null ? count : 0L);
        statistics.put("netMovement", totalEntries - totalExits);

        return statistics;
    }
}