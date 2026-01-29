package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.*;
import com.utama.my_inventory.dtos.request.inventory.InventoryMovementRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockAdjustmentRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockEntryRequestDTO;
import com.utama.my_inventory.dtos.request.inventory.StockExitRequestDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryMovementResponseDTO;
import com.utama.my_inventory.dtos.response.inventory.InventoryOperationResponseDTO;
import com.utama.my_inventory.services.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventario", description = "API para gestión de movimientos de inventario")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/movement")
    @Operation(summary = "Registrar movimiento de inventario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimiento registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente")
    })
    public ResponseEntity<ExtendedBaseResponse<InventoryOperationResponseDTO>> registerMovement(
            @Valid @RequestBody InventoryMovementRequestDTO requestDTO,
            @RequestParam(defaultValue = "system") String currentUser) {

        InventoryOperationResponseDTO result = inventoryService.registerMovement(requestDTO, currentUser);
        return ExtendedBaseResponse.ok(result, "Movimiento registrado exitosamente")
                .toResponseEntity();
    }

    @PostMapping("/entry")
    @Operation(summary = "Registrar entrada de stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrada registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<ExtendedBaseResponse<InventoryOperationResponseDTO>> registerEntry(
            @Valid @RequestBody StockEntryRequestDTO requestDTO,
            @RequestParam(defaultValue = "system") String currentUser) {

        InventoryOperationResponseDTO result = inventoryService.registerEntry(requestDTO, currentUser);
        return ExtendedBaseResponse.ok(result, "Entrada de stock registrada exitosamente")
                .toResponseEntity();
    }

    @PostMapping("/exit")
    @Operation(summary = "Registrar salida de stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Salida registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente")
    })
    public ResponseEntity<ExtendedBaseResponse<InventoryOperationResponseDTO>> registerExit(
            @Valid @RequestBody StockExitRequestDTO requestDTO,
            @RequestParam(defaultValue = "system") String currentUser) {

        InventoryOperationResponseDTO result = inventoryService.registerExit(requestDTO, currentUser);
        return ExtendedBaseResponse.ok(result, "Salida de stock registrada exitosamente")
                .toResponseEntity();
    }

    @PostMapping("/adjustment")
    @Operation(summary = "Registrar ajuste de stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ajuste registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente")
    })
    public ResponseEntity<ExtendedBaseResponse<InventoryOperationResponseDTO>> registerAdjustment(
            @Valid @RequestBody StockAdjustmentRequestDTO requestDTO,
            @RequestParam(defaultValue = "system") String currentUser) {

        InventoryOperationResponseDTO result = inventoryService.registerAdjustment(requestDTO, currentUser);
        return ExtendedBaseResponse.ok(result, "Ajuste de stock registrado exitosamente")
                .toResponseEntity();
    }

    @GetMapping("/product/{productId}/history")
    @Operation(summary = "Obtener historial de movimientos de un producto")
    public ResponseEntity<ExtendedBaseResponse<List<InventoryMovementResponseDTO>>> getProductHistory(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long productId) {

        List<InventoryMovementResponseDTO> history = inventoryService.getProductHistory(productId);
        return ExtendedBaseResponse.ok(history, "Historial obtenido correctamente")
                .toResponseEntity();
    }

    @GetMapping("/movements")
    @Operation(summary = "Obtener todos los movimientos de inventario")
    public ResponseEntity<ExtendedBaseResponse<List<InventoryMovementResponseDTO>>> getAllMovements() {

        List<InventoryMovementResponseDTO> movements = inventoryService.getAllMovements();
        return ExtendedBaseResponse.ok(movements, "Movimientos obtenidos correctamente")
                .toResponseEntity();
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar movimientos con filtros")
    public ResponseEntity<ExtendedBaseResponse<List<InventoryMovementResponseDTO>>> searchMovements(
            @Parameter(description = "ID del producto") @RequestParam(required = false) Long productId,
            @Parameter(description = "Tipo de movimiento (ENTRADA/SALIDA/AJUSTE)")
            @RequestParam(required = false) String movementType,
            @Parameter(description = "Usuario que registró") @RequestParam(required = false) String registeredBy,
            @Parameter(description = "Fecha inicio (YYYY-MM-DDTHH:MM:SS)")
            @RequestParam(required = false) LocalDateTime startDate,
            @Parameter(description = "Fecha fin (YYYY-MM-DDTHH:MM:SS)")
            @RequestParam(required = false) LocalDateTime endDate) {

        List<InventoryMovementResponseDTO> movements = inventoryService.searchMovements(
                productId, movementType, registeredBy, startDate, endDate);

        return ExtendedBaseResponse.ok(movements, "Búsqueda completada")
                .toResponseEntity();
    }

    @GetMapping("/type/{movementType}")
    @Operation(summary = "Obtener movimientos por tipo")
    public ResponseEntity<ExtendedBaseResponse<List<InventoryMovementResponseDTO>>> getMovementsByType(
            @Parameter(description = "Tipo de movimiento", example = "ENTRADA")
            @PathVariable String movementType) {

        List<InventoryMovementResponseDTO> movements = inventoryService.getMovementsByType(movementType);
        return ExtendedBaseResponse.ok(movements, "Movimientos obtenidos por tipo")
                .toResponseEntity();
    }

    @GetMapping("/user/{username}")
    @Operation(summary = "Obtener movimientos por usuario")
    public ResponseEntity<ExtendedBaseResponse<List<InventoryMovementResponseDTO>>> getMovementsByUser(
            @Parameter(description = "Nombre de usuario", example = "admin")
            @PathVariable String username) {

        List<InventoryMovementResponseDTO> movements = inventoryService.getMovementsByUser(username);
        return ExtendedBaseResponse.ok(movements, "Movimientos obtenidos por usuario")
                .toResponseEntity();
    }

    @GetMapping("/product/{productId}/statistics")
    @Operation(summary = "Obtener estadísticas de movimientos de un producto")
    public ResponseEntity<ExtendedBaseResponse<Map<String, Object>>> getProductStatistics(
            @PathVariable Long productId) {

        Map<String, Object> statistics = inventoryService.getProductStatistics(productId);
        return ExtendedBaseResponse.ok(statistics, "Estadísticas obtenidas")
                .toResponseEntity();
    }

    @GetMapping("/date-range")
    @Operation(summary = "Obtener movimientos por rango de fechas")
    public ResponseEntity<ExtendedBaseResponse<List<InventoryMovementResponseDTO>>> getMovementsByDateRange(
            @Parameter(description = "Fecha inicio", example = "2024-01-01T00:00:00")
            @RequestParam LocalDateTime startDate,
            @Parameter(description = "Fecha fin", example = "2024-12-31T23:59:59")
            @RequestParam LocalDateTime endDate) {

        List<InventoryMovementResponseDTO> movements = inventoryService.getMovementsByDateRange(startDate, endDate);
        return ExtendedBaseResponse.ok(movements, "Movimientos obtenidos por rango de fechas")
                .toResponseEntity();
    }
}