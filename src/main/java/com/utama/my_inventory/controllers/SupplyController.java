package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.supply.SupplyMovementRequestDTO;
import com.utama.my_inventory.dtos.request.supply.SupplyRequestDTO;
import com.utama.my_inventory.dtos.request.supply.SupplyResponseDTO;
import com.utama.my_inventory.dtos.response.supply.SupplyMovementResponseDTO;
import com.utama.my_inventory.services.SupplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplies")
@RequiredArgsConstructor
@Tag(name = "📦 Insumos y Embalaje", description = "Gestión de catálogo, costos y movimientos inmutables de insumos internos (packaging, cintas, etc.)")
public class SupplyController {

    private final SupplyService supplyService;

    @Operation(
            summary = "Obtener todos los insumos activos",
            description = "Retorna una lista completa de los insumos que actualmente se encuentran activos en el sistema, incluyendo su stock actual y costo unitario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de insumos activos obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<ExtendedBaseResponse<List<SupplyResponseDTO>>> getAllSupplies() {
        return ExtendedBaseResponse.ok(supplyService.getAllActiveSupplies(), "Lista de insumos activos obtenida").toResponseEntity();
    }

    @Operation(
            summary = "Crear un nuevo insumo",
            description = "Registra un nuevo insumo en el catálogo estableciendo su costo inicial, unidad de medida y stock base. Valida que el nombre no esté duplicado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Insumo creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error de validación en los datos enviados o el nombre ya existe", content = @Content(schema = @Schema(implementation = ExtendedBaseResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ExtendedBaseResponse<SupplyResponseDTO>> createSupply(@Valid @RequestBody SupplyRequestDTO dto) {
        return ExtendedBaseResponse.created(supplyService.createSupply(dto), "Insumo creado exitosamente").toResponseEntity();
    }

    @Operation(
            summary = "Actualizar información de un insumo",
            description = "Modifica los detalles del catálogo de un insumo (nombre, descripción, medida, costo). IMPORTANTE: Este endpoint no modifica el stock actual, para ello utilice el endpoint de movimientos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Insumo actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "El insumo especificado no existe", content = @Content(schema = @Schema(implementation = ExtendedBaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Error de validación (ej. nombre duplicado)", content = @Content(schema = @Schema(implementation = ExtendedBaseResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ExtendedBaseResponse<SupplyResponseDTO>> updateSupply(
            @Parameter(description = "ID único del insumo", example = "1") @PathVariable Long id,
            @Valid @RequestBody SupplyRequestDTO dto) {
        return ExtendedBaseResponse.ok(supplyService.updateSupply(id, dto), "Insumo actualizado exitosamente").toResponseEntity();
    }

    @Operation(
            summary = "Desactivar un insumo (Borrado lógico)",
            description = "Realiza un borrado lógico del insumo cambiando su estado a inactivo. Ya no aparecerá en el listado principal ni en los tableros de costos, pero se mantendrá su historial."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Insumo desactivado correctamente"),
            @ApiResponse(responseCode = "404", description = "El insumo especificado no existe", content = @Content(schema = @Schema(implementation = ExtendedBaseResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ExtendedBaseResponse<Void>> deactivateSupply(
            @Parameter(description = "ID único del insumo a desactivar", example = "1") @PathVariable Long id) {
        supplyService.deactivateSupply(id);
        return ExtendedBaseResponse.<Void>ok(null, "Insumo desactivado correctamente").toResponseEntity();
    }

    @Operation(
            summary = "Registrar entrada, salida o ajuste de stock en un insumo",
            description = "Registra un movimiento en el historial inmutable del insumo y recalcula su stock actual. Permite tipos de movimiento: ENTRADA, SALIDA o AJUSTE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movimiento registrado y stock actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Stock insuficiente para realizar una SALIDA, o falta el motivo en un AJUSTE", content = @Content(schema = @Schema(implementation = ExtendedBaseResponse.class))),
            @ApiResponse(responseCode = "404", description = "El insumo especificado no existe", content = @Content(schema = @Schema(implementation = ExtendedBaseResponse.class)))
    })
    @PostMapping("/{id}/movements")
    public ResponseEntity<ExtendedBaseResponse<SupplyMovementResponseDTO>> registerMovement(
            @Parameter(description = "ID único del insumo a afectar", example = "1") @PathVariable Long id,
            @Valid @RequestBody SupplyMovementRequestDTO dto) {
        return ExtendedBaseResponse.created(supplyService.registerMovement(id, dto), "Movimiento registrado exitosamente").toResponseEntity();
    }

    @Operation(
            summary = "Obtener el historial de movimientos",
            description = "Retorna los últimos 50 movimientos (entradas, salidas, ajustes) registrados para un insumo específico, ordenados desde el más reciente al más antiguo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de movimientos obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "El insumo especificado no existe", content = @Content(schema = @Schema(implementation = ExtendedBaseResponse.class)))
    })
    @GetMapping("/{id}/movements")
    public ResponseEntity<ExtendedBaseResponse<List<SupplyMovementResponseDTO>>> getSupplyMovements(
            @Parameter(description = "ID único del insumo", example = "1") @PathVariable Long id) {
        return ExtendedBaseResponse.ok(supplyService.getMovementsBySupply(id), "Historial de movimientos obtenido").toResponseEntity();
    }
}