package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.MovementFilter;
import com.utama.my_inventory.dtos.request.inventory.InventoryMovementRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockAdjustmentRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockEntryRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockExitRequestDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryMovementResponseDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryOperationResponseDTO;
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
    List<InventoryMovementResponseDTO> searchMovements(MovementFilter filter);

    List<InventoryMovementResponseDTO> getMovementsByType(String movementType);
    List<InventoryMovementResponseDTO> getMovementsByUser(String username);
    List<InventoryMovementResponseDTO> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    // Estadísticas
    Integer getTotalEntriesByProduct(Long productId);
    Integer getTotalExitsByProduct(Long productId);
    Long getMovementCountByProduct(Long productId);

    Map<String, Object> getProductStatistics(Long productId);
}