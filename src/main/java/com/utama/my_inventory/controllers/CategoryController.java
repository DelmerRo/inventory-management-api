package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ApiResponseDTO;
import com.utama.my_inventory.dtos.request.CategoryRequestDTO;
import com.utama.my_inventory.dtos.response.CategoryResponseDTO;
import com.utama.my_inventory.dtos.response.SubcategoryResponseDTO;
import com.utama.my_inventory.services.impl.CategoryServiceImpl;
import com.utama.my_inventory.services.impl.SubcategoryServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categorías", description = "API para gestión de categorías de productos")
public class CategoryController {

    private final CategoryServiceImpl categoryService;
    private final SubcategoryServiceImpl subcategoryService;

    @PostMapping
    @Operation(
            summary = "Crear nueva categoría",
            description = "Crea una nueva categoría de productos"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre")
    })
    public ResponseEntity<ApiResponseDTO<CategoryResponseDTO>> createCategory(
            @Valid @RequestBody CategoryRequestDTO requestDTO) {
        CategoryResponseDTO category = categoryService.createCategory(requestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(category, "Categoría creada exitosamente"));
    }

    @GetMapping
    @Operation(
            summary = "Listar todas las categorías",
            description = "Obtiene una lista de todas las categorías activas"
    )
    @ApiResponse(responseCode = "200", description = "Lista de categorías obtenida exitosamente")
    public ResponseEntity<ApiResponseDTO<List<CategoryResponseDTO>>> getAllCategories() {
        List<CategoryResponseDTO> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponseDTO.success(categories));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener categoría por ID",
            description = "Obtiene los detalles de una categoría específica"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    public ResponseEntity<ApiResponseDTO<CategoryResponseDTO>> getCategoryById(
            @Parameter(description = "ID de la categoría", example = "1")
            @PathVariable Long id) {
        CategoryResponseDTO category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(category));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar categoría",
            description = "Actualiza los datos de una categoría existente"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre")
    })
    public ResponseEntity<ApiResponseDTO<CategoryResponseDTO>> updateCategory(
            @Parameter(description = "ID de la categoría", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO requestDTO) {
        CategoryResponseDTO category = categoryService.updateCategory(id, requestDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(category, "Categoría actualizada exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar categoría",
            description = "Elimina una categoría. No se puede eliminar si tiene subcategorías asociadas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoría eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque tiene subcategorías asociadas")
    })
    public ResponseEntity<ApiResponseDTO<Void>> deleteCategory(
            @Parameter(description = "ID de la categoría", example = "1")
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDTO.success(null, "Categoría eliminada exitosamente"));
    }

    @GetMapping("/{id}/subcategories")
    @Operation(
            summary = "Obtener subcategorías de una categoría",
            description = "Obtiene todas las subcategorías asociadas a una categoría específica"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subcategorías obtenidas exitosamente"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    public ResponseEntity<ApiResponseDTO<List<SubcategoryResponseDTO>>> getSubcategoriesByCategory(
            @Parameter(description = "ID de la categoría", example = "1")
            @PathVariable Long id) {
        List<SubcategoryResponseDTO> subcategories = subcategoryService.getSubcategoriesByCategoryId(id);
        return ResponseEntity.ok(ApiResponseDTO.success(subcategories));
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(
            summary = "Cambiar estado de categoría",
            description = "Activa o desactiva una categoría (toggle)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado cambiado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    public ResponseEntity<ApiResponseDTO<CategoryResponseDTO>> toggleCategoryStatus(
            @Parameter(description = "ID de la categoría", example = "1")
            @PathVariable Long id) {
        CategoryResponseDTO category = categoryService.toggleCategoryStatus(id);
        String message = category.active() ? "Categoría activada" : "Categoría desactivada";
        return ResponseEntity.ok(ApiResponseDTO.success(category, message));
    }
}