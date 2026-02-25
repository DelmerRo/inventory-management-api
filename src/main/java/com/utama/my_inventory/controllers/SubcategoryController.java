package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.SubcategoryRequestDTO;
import com.utama.my_inventory.dtos.response.CategoryResponseDTO;
import com.utama.my_inventory.dtos.response.SubcategoryResponseDTO;
import com.utama.my_inventory.services.SubcategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subcategories")
@RequiredArgsConstructor
@Tag(name = "Subcategorías", description = "API para gestión de subcategorías")
public class SubcategoryController {

    private final SubcategoryService subcategoryService;

    @PostMapping
    @Operation(summary = "Crear nueva subcategoría")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subcategoría creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "Subcategoría duplicada en esta categoría")
    })
    public ResponseEntity<ExtendedBaseResponse<SubcategoryResponseDTO>> createSubcategory(
            @Valid @RequestBody SubcategoryRequestDTO requestDTO) {

        SubcategoryResponseDTO subcategory = subcategoryService.createSubcategory(requestDTO);
        return ExtendedBaseResponse.created(subcategory, "Subcategoría creada exitosamente")
                .toResponseEntity();
    }

    @GetMapping
    @Operation(summary = "Listar todas las subcategorías")
    public ResponseEntity<ExtendedBaseResponse<List<SubcategoryResponseDTO>>> getAllSubcategories() {

        List<SubcategoryResponseDTO> subcategories = subcategoryService.getAllSubcategories();
        return ExtendedBaseResponse.ok(subcategories, "Subcategorías obtenidas correctamente")
                .toResponseEntity();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener subcategoría por ID")
    public ResponseEntity<ExtendedBaseResponse<SubcategoryResponseDTO>> getSubcategoryById(
            @Parameter(description = "ID de la subcategoría", example = "1")
            @PathVariable Long id) {

        SubcategoryResponseDTO subcategory = subcategoryService.getSubcategoryById(id);
        return ExtendedBaseResponse.ok(subcategory, "Subcategoría encontrada")
                .toResponseEntity();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar subcategoría existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subcategoría actualizada"),
            @ApiResponse(responseCode = "404", description = "Subcategoría o categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado en esta categoría")
    })
    public ResponseEntity<ExtendedBaseResponse<SubcategoryResponseDTO>> updateSubcategory(
            @PathVariable Long id,
            @Valid @RequestBody SubcategoryRequestDTO requestDTO) {

        SubcategoryResponseDTO subcategory = subcategoryService.updateSubcategory(id, requestDTO);
        return ExtendedBaseResponse.ok(subcategory, "Subcategoría actualizada exitosamente")
                .toResponseEntity();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar subcategoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subcategoría eliminada"),
            @ApiResponse(responseCode = "404", description = "Subcategoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar, tiene productos asociados")
    })
    public ResponseEntity<ExtendedBaseResponse<Void>> deleteSubcategory(
            @PathVariable Long id) {

        subcategoryService.deleteSubcategory(id);
        return ExtendedBaseResponse.<Void>ok(null, "Subcategoría eliminada exitosamente")
                .toResponseEntity();
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Activar/desactivar subcategoría")
    public ResponseEntity<ExtendedBaseResponse<SubcategoryResponseDTO>> toggleCategoryStatus(
            @PathVariable Long id) {

        SubcategoryResponseDTO subcategory = subcategoryService.toggleSubCategoryStatus(id);
        String message = subcategory.active()
                ? "Subcategoría activada exitosamente"
                : "Subcategoría desactivada exitosamente";

        return ExtendedBaseResponse.ok(subcategory, message)
                .toResponseEntity();
    }
}