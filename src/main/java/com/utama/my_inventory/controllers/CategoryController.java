package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.CategoryRequestDTO;
import com.utama.my_inventory.dtos.response.CategoryResponseDTO;
import com.utama.my_inventory.dtos.response.SubcategoryResponseDTO;
import com.utama.my_inventory.services.CategoryService;
import com.utama.my_inventory.services.SubcategoryService;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Categorías", description = "API para gestión de categorías de productos")
public class CategoryController {

    private final CategoryService categoryService;
    private final SubcategoryService subcategoryService;

    // ✅ UN SOLO ENDPOINT - Siempre trae TODAS las categorías (activas + inactivas)
    @GetMapping
    @Operation(summary = "Listar todas las categorías")
    public ResponseEntity<ExtendedBaseResponse<List<CategoryResponseDTO>>> getAllCategories() {
        log.info("GET /api/categories - Obteniendo todas las categorías");
        List<CategoryResponseDTO> categories = categoryService.getAllCategories();
        log.info("Categorías encontradas: {}", categories.size());
        return ExtendedBaseResponse.ok(categories, "Categorías obtenidas correctamente")
                .toResponseEntity();
    }

    @PostMapping
    @Operation(summary = "Crear nueva categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "Categoría duplicada")
    })
    public ResponseEntity<ExtendedBaseResponse<CategoryResponseDTO>> createCategory(
            @Valid @RequestBody CategoryRequestDTO requestDTO) {

        CategoryResponseDTO category = categoryService.createCategory(requestDTO);
        return ExtendedBaseResponse.created(category, "Categoría creada exitosamente")
                .toResponseEntity();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría por ID")
    public ResponseEntity<ExtendedBaseResponse<CategoryResponseDTO>> getCategoryById(
            @Parameter(description = "ID de la categoría", example = "1")
            @PathVariable Long id) {

        CategoryResponseDTO category = categoryService.getCategoryById(id);
        return ExtendedBaseResponse.ok(category, "Categoría encontrada")
                .toResponseEntity();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoría existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    })
    public ResponseEntity<ExtendedBaseResponse<CategoryResponseDTO>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO requestDTO) {

        CategoryResponseDTO category = categoryService.updateCategory(id, requestDTO);
        return ExtendedBaseResponse.ok(category, "Categoría actualizada exitosamente")
                .toResponseEntity();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría eliminada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar, tiene subcategorías")
    })
    public ResponseEntity<ExtendedBaseResponse<Void>> deleteCategory(
            @PathVariable Long id) {

        categoryService.deleteCategory(id);
        return ExtendedBaseResponse.<Void>ok(null, "Categoría eliminada exitosamente")
                .toResponseEntity();
    }

    @GetMapping("/{id}/subcategories")
    @Operation(summary = "Obtener subcategorías de una categoría")
    public ResponseEntity<ExtendedBaseResponse<List<SubcategoryResponseDTO>>> getSubcategoriesByCategory(
            @PathVariable Long id) {

        List<SubcategoryResponseDTO> subcategories =
                subcategoryService.getSubcategoriesByCategoryId(id);

        return ExtendedBaseResponse.ok(subcategories, "Subcategorías obtenidas correctamente")
                .toResponseEntity();
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Activar/desactivar categoría")
    public ResponseEntity<ExtendedBaseResponse<CategoryResponseDTO>> toggleCategoryStatus(
            @PathVariable Long id) {

        CategoryResponseDTO category = categoryService.toggleCategoryStatus(id);
        String message = category.active()
                ? "Categoría activada exitosamente"
                : "Categoría desactivada exitosamente";

        return ExtendedBaseResponse.ok(category, message)
                .toResponseEntity();
    }
}