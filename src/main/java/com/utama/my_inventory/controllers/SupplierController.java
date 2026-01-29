package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.SupplierRequestDTO;
import com.utama.my_inventory.dtos.response.SupplierResponseDTO;
import com.utama.my_inventory.dtos.response.SupplierSummaryResponseDTO;
import com.utama.my_inventory.services.SupplierService;
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

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Proveedores", description = "API para gestión de proveedores")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    @Operation(summary = "Crear nuevo proveedor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proveedor creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    })
    public ResponseEntity<ExtendedBaseResponse<SupplierResponseDTO>> createSupplier(
            @Valid @RequestBody SupplierRequestDTO requestDTO) {

        SupplierResponseDTO supplier = supplierService.createSupplier(requestDTO);
        return ExtendedBaseResponse.created(supplier, "Proveedor creado exitosamente")
                .toResponseEntity();
    }

    @GetMapping
    @Operation(summary = "Listar todos los proveedores")
    public ResponseEntity<ExtendedBaseResponse<List<SupplierResponseDTO>>> getAllSuppliers() {

        List<SupplierResponseDTO> suppliers = supplierService.getAllSuppliers();
        return ExtendedBaseResponse.ok(suppliers, "Proveedores obtenidos correctamente")
                .toResponseEntity();
    }

    @GetMapping("/summary")
    @Operation(summary = "Listar proveedores (resumen)")
    public ResponseEntity<ExtendedBaseResponse<List<SupplierSummaryResponseDTO>>> getAllSuppliersSummary() {

        List<SupplierSummaryResponseDTO> suppliers = supplierService.getAllSuppliersSummary();
        return ExtendedBaseResponse.ok(suppliers, "Resumen de proveedores obtenido")
                .toResponseEntity();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener proveedor por ID")
    public ResponseEntity<ExtendedBaseResponse<SupplierResponseDTO>> getSupplierById(
            @Parameter(description = "ID del proveedor", example = "1")
            @PathVariable Long id) {

        SupplierResponseDTO supplier = supplierService.getSupplierById(id);
        return ExtendedBaseResponse.ok(supplier, "Proveedor encontrado")
                .toResponseEntity();
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Obtener proveedor por nombre")
    public ResponseEntity<ExtendedBaseResponse<SupplierResponseDTO>> getSupplierByName(
            @Parameter(description = "Nombre del proveedor", example = "Tecnología S.A.")
            @PathVariable String name) {

        SupplierResponseDTO supplier = supplierService.getSupplierByName(name);
        return ExtendedBaseResponse.ok(supplier, "Proveedor encontrado")
                .toResponseEntity();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar proveedor existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedor actualizado"),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado"),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    })
    public ResponseEntity<ExtendedBaseResponse<SupplierResponseDTO>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequestDTO requestDTO) {

        SupplierResponseDTO supplier = supplierService.updateSupplier(id, requestDTO);
        return ExtendedBaseResponse.ok(supplier, "Proveedor actualizado exitosamente")
                .toResponseEntity();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar proveedor (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedor eliminado"),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar, tiene productos asociados")
    })
    public ResponseEntity<ExtendedBaseResponse<Void>> deleteSupplier(
            @PathVariable Long id) {

        supplierService.deleteSupplier(id);
        return ExtendedBaseResponse.<Void>ok(null, "Proveedor eliminado exitosamente")
                .toResponseEntity();
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Activar/desactivar proveedor")
    public ResponseEntity<ExtendedBaseResponse<SupplierResponseDTO>> toggleSupplierStatus(
            @PathVariable Long id) {

        SupplierResponseDTO supplier = supplierService.toggleSupplierStatus(id);
        String message = supplier.active()
                ? "Proveedor activado exitosamente"
                : "Proveedor desactivado exitosamente";

        return ExtendedBaseResponse.ok(supplier, message)
                .toResponseEntity();
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar proveedores con filtros")
    public ResponseEntity<ExtendedBaseResponse<List<SupplierResponseDTO>>> searchSuppliers(
            @Parameter(description = "Nombre del proveedor") @RequestParam(required = false) String name,
            @Parameter(description = "Persona de contacto") @RequestParam(required = false) String contactPerson,
            @Parameter(description = "Email") @RequestParam(required = false) String email) {

        List<SupplierResponseDTO> suppliers = supplierService.searchSuppliers(name, contactPerson, email);
        return ExtendedBaseResponse.ok(suppliers, "Búsqueda completada")
                .toResponseEntity();
    }

    @GetMapping("/with-products")
    @Operation(summary = "Obtener proveedores con productos activos")
    public ResponseEntity<ExtendedBaseResponse<List<SupplierResponseDTO>>> getSuppliersWithActiveProducts() {

        List<SupplierResponseDTO> suppliers = supplierService.getSuppliersWithActiveProducts();
        return ExtendedBaseResponse.ok(suppliers, "Proveedores con productos activos obtenidos")
                .toResponseEntity();
    }

    @GetMapping("/top-suppliers")
    @Operation(summary = "Obtener top proveedores por cantidad de productos")
    public ResponseEntity<ExtendedBaseResponse<List<SupplierSummaryResponseDTO>>> getTopSuppliers(
            @Parameter(description = "Límite de resultados", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        List<SupplierSummaryResponseDTO> suppliers = supplierService.getTopSuppliersByProductCount(limit);
        return ExtendedBaseResponse.ok(suppliers, "Top proveedores obtenidos")
                .toResponseEntity();
    }

    @GetMapping("/statistics/count")
    @Operation(summary = "Obtener cantidad total de proveedores")
    public ResponseEntity<ExtendedBaseResponse<Long>> getSupplierCount() {

        Long count = supplierService.getTotalSupplierCount();
        return ExtendedBaseResponse.ok(count, "Cantidad de proveedores obtenida")
                .toResponseEntity();
    }
}