package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.UpdateSupplierSkuRequestDTO;
import com.utama.my_inventory.dtos.response.SupplierAssociationResponseDTO;
import com.utama.my_inventory.services.ProductSupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-suppliers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Relaciones Producto-Proveedor", description = "API para gestionar relaciones entre productos y proveedores")
public class ProductSupplierController {

    private final ProductSupplierService productSupplierService;

    @GetMapping
    @Operation(summary = "Listar todas las relaciones producto-proveedor")
    public ResponseEntity<ExtendedBaseResponse<List<SupplierAssociationResponseDTO>>> getAllRelations() {
        List<SupplierAssociationResponseDTO> relations = productSupplierService.getAllRelations();
        return ExtendedBaseResponse.ok(relations, "Relaciones obtenidas correctamente")
                .toResponseEntity();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener relación por ID")
    public ResponseEntity<ExtendedBaseResponse<SupplierAssociationResponseDTO>> getRelationById(
            @PathVariable Long id) {
        SupplierAssociationResponseDTO relation = productSupplierService.getRelationById(id);
        return ExtendedBaseResponse.ok(relation, "Relación encontrada")
                .toResponseEntity();
    }

    @PatchMapping("/{id}/sku")
    @Operation(summary = "Actualizar SKU de una relación específica")
    public ResponseEntity<ExtendedBaseResponse<SupplierAssociationResponseDTO>> updateRelationSku(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplierSkuRequestDTO request) {
        SupplierAssociationResponseDTO relation = productSupplierService.updateSku(id, request.supplierSku());
        return ExtendedBaseResponse.ok(relation, "SKU actualizado correctamente")
                .toResponseEntity();
    }

    @PatchMapping("/{id}/primary")
    @Operation(summary = "Marcar/desmarcar como proveedor principal")
    public ResponseEntity<ExtendedBaseResponse<SupplierAssociationResponseDTO>> togglePrimary(
            @PathVariable Long id) {
        SupplierAssociationResponseDTO relation = productSupplierService.togglePrimary(id);
        String message = relation.isPrimary() ? "Marcado como principal" : "Desmarcado como principal";
        return ExtendedBaseResponse.ok(relation, message)
                .toResponseEntity();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una relación específica")
    public ResponseEntity<ExtendedBaseResponse<Void>> deleteRelation(@PathVariable Long id) {
        productSupplierService.deleteRelation(id);
        return ExtendedBaseResponse.<Void>ok(null, "Relación eliminada correctamente")
                .toResponseEntity();
    }
}