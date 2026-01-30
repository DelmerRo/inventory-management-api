package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.inventory.InventoryMovementRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockAdjustmentRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockEntryRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockExitRequestDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryMovementResponseDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryOperationResponseDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryStatisticsResponseDTO;
import com.utama.my_inventory.entities.enums.MovementType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface InventoryService {

    // Operaciones básicas
    InventoryOperationResponseDTO registerMovement(InventoryMovementRequestDTO requestDTO, String currentUser);
    List<InventoryMovementResponseDTO> getProductHistory(Long productId);
    List<InventoryMovementResponseDTO> getAllMovements();

    // Operaciones específicas
    InventoryOperationResponseDTO registerEntry(StockEntryRequestDTO requestDTO, String currentUser);
    InventoryOperationResponseDTO registerExit(StockExitRequestDTO requestDTO, String currentUser);
    InventoryOperationResponseDTO registerAdjustment(StockAdjustmentRequestDTO requestDTO, String currentUser);

    // Búsquedas avanzadas
    List<InventoryMovementResponseDTO> searchMovements(Long productId, MovementType movementType,
                                                       String registeredBy, LocalDateTime startDate,
                                                       LocalDateTime endDate);

    List<InventoryMovementResponseDTO> getMovementsByType(String movementType);
    List<InventoryMovementResponseDTO> getMovementsByUser(String username);
    List<InventoryMovementResponseDTO> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    // Estadísticas
    Integer getTotalEntriesByProduct(Long productId);
    Integer getTotalExitsByProduct(Long productId);
    Long getMovementCountByProduct(Long productId);

    // Reportes
    List<Object[]> getMonthlyMovementSummary(int year, int month);
    List<Object[]> getProductMovementSummary(Long productId);

    Map<String, Object> getProductStatistics(Long productId);
}