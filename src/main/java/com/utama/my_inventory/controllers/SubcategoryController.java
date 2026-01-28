package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ApiResponseDTO;
import com.utama.my_inventory.dtos.request.SubcategoryRequestDTO;
import com.utama.my_inventory.dtos.response.SubcategoryResponseDTO;
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
@RequestMapping("/api/subcategories")
@RequiredArgsConstructor
@Tag(name = "Subcategorías", description = "API para gestión de subcategorías de productos")
public class SubcategoryController {

    private final SubcategoryServiceImpl subcategoryService;

    @PostMapping
    @Operation(
            summary = "Crear nueva subcategoría",
            description = "Crea una nueva subcategoría asociada a una categoría existente"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subcategoría creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "Ya existe una subcategoría con ese nombre en la categoría")
    })
    public ResponseEntity<ApiResponseDTO<SubcategoryResponseDTO>> createSubcategory(
            @Valid @RequestBody SubcategoryRequestDTO requestDTO) {
        SubcategoryResponseDTO subcategory = subcategoryService.createSubcategory(requestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(subcategory, "Subcategoría creada exitosamente"));
    }

    @GetMapping
    @Operation(
            summary = "Listar todas las subcategorías",
            description = "Obtiene una lista de todas las subcategorías"
    )
    @ApiResponse(responseCode = "200", description = "Lista de subcategorías obtenida exitosamente")
    public ResponseEntity<ApiResponseDTO<List<SubcategoryResponseDTO>>> getAllSubcategories() {
        List<SubcategoryResponseDTO> subcategories = subcategoryService.getAllSubcategories();
        return ResponseEntity.ok(ApiResponseDTO.success(subcategories));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener subcategoría por ID",
            description = "Obtiene los detalles de una subcategoría específica"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subcategoría encontrada"),
            @ApiResponse(responseCode = "404", description = "Subcategoría no encontrada")
    })
    public ResponseEntity<ApiResponseDTO<SubcategoryResponseDTO>> getSubcategoryById(
            @Parameter(description = "ID de la subcategoría", example = "1")
            @PathVariable Long id) {
        SubcategoryResponseDTO subcategory = subcategoryService.getSubcategoryById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(subcategory));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar subcategoría",
            description = "Actualiza los datos de una subcategoría existente"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subcategoría actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Subcategoría o categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "Ya existe una subcategoría con ese nombre en la categoría")
    })
    public ResponseEntity<ApiResponseDTO<SubcategoryResponseDTO>> updateSubcategory(
            @Parameter(description = "ID de la subcategoría", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody SubcategoryRequestDTO requestDTO) {
        SubcategoryResponseDTO subcategory = subcategoryService.updateSubcategory(id, requestDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(subcategory, "Subcategoría actualizada exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar subcategoría",
            description = "Elimina una subcategoría. No se puede eliminar si tiene productos asociados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Subcategoría eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Subcategoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque tiene productos asociados")
    })
    public ResponseEntity<ApiResponseDTO<Void>> deleteSubcategory(
            @Parameter(description = "ID de la subcategoría", example = "1")
            @PathVariable Long id) {
        subcategoryService.deleteSubcategory(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDTO.success(null, "Subcategoría eliminada exitosamente"));
    }
}