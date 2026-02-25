package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.BaseResponse;
import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.request.multimedia.MultimediaCreateRequestDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaFileResponseDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaUploadResponseDTO;
import com.utama.my_inventory.entities.enums.FileType;
import com.utama.my_inventory.services.MultimediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/multimedia")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Multimedia", description = "API para gestión de archivos multimedia (imágenes, videos, documentos)")
public class MultimediaController {

    private final MultimediaService multimediaService;

    @PostMapping(value = "/products/{productId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir archivo multimedia a un producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo subido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Archivo inválido o tipo incorrecto"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "413", description = "Archivo demasiado grande")
    })
    public ResponseEntity<ExtendedBaseResponse<MultimediaUploadResponseDTO>> uploadFile(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long productId,

            @Parameter(description = "Archivo a subir", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Tipo de archivo (IMAGE, VIDEO, DOCUMENT)", example = "IMAGE")
            @RequestParam("fileType") String fileType) {

        MultimediaUploadResponseDTO result = multimediaService.uploadFile(productId, file, fileType);
        return ExtendedBaseResponse.ok(result, "Archivo subido exitosamente")
                .toResponseEntity();
    }

    @PostMapping("/products/{productId}/url")
    @Operation(summary = "Crear archivo multimedia desde URL")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Archivo creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "URL inválida"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "URL ya existe para este producto")
    })
    public ResponseEntity<ExtendedBaseResponse<MultimediaFileResponseDTO>> createFromUrl(
            @PathVariable Long productId,
            @Valid @RequestBody MultimediaCreateRequestDTO requestDTO) {

        MultimediaFileResponseDTO file = multimediaService.createFromUrl(productId, requestDTO);
        return ExtendedBaseResponse.created(file, "Archivo multimedia creado exitosamente")
                .toResponseEntity();
    }

    @GetMapping("/products/{productId}/files")
    @Operation(summary = "Listar todos los archivos multimedia de un producto")
    public ResponseEntity<ExtendedBaseResponse<List<MultimediaFileResponseDTO>>> getProductFiles(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long productId) {

        List<MultimediaFileResponseDTO> files = multimediaService.getProductFiles(productId);
        return ExtendedBaseResponse.ok(files, "Archivos obtenidos correctamente")
                .toResponseEntity();
    }

    @GetMapping("/products/{productId}/files/{fileType}")
    @Operation(summary = "Listar archivos multimedia de un producto por tipo")
    public ResponseEntity<ExtendedBaseResponse<List<MultimediaFileResponseDTO>>> getProductFilesByType(
            @PathVariable Long productId,
            @Parameter(description = "Tipo de archivo", example = "IMAGE")
            @PathVariable String fileType) {

        List<MultimediaFileResponseDTO> files = multimediaService.getProductFilesByType(productId, fileType);
        return ExtendedBaseResponse.ok(files, "Archivos obtenidos por tipo correctamente")
                .toResponseEntity();
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Obtener información de un archivo multimedia")
    public ResponseEntity<ExtendedBaseResponse<MultimediaFileResponseDTO>> getFileById(
            @Parameter(description = "ID del archivo", example = "1")
            @PathVariable Long fileId) {

        MultimediaFileResponseDTO file = multimediaService.getFileById(fileId);
        return ExtendedBaseResponse.ok(file, "Archivo encontrado")
                .toResponseEntity();
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Eliminar archivo multimedia")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Archivo no encontrado")
    })
    public ResponseEntity<BaseResponse> deleteFile(
            @Parameter(description = "ID del archivo", example = "1")
            @PathVariable Long fileId) {

        multimediaService.deleteFile(fileId);
        return BaseResponse.ok("Archivo eliminado exitosamente")
                .toResponseEntity();
    }

    @DeleteMapping("/products/{productId}/files")
    @Operation(summary = "Eliminar todos los archivos multimedia de un producto")
    public ResponseEntity<BaseResponse> deleteProductFiles(
            @PathVariable Long productId) {

        multimediaService.deleteProductFiles(productId);
        return BaseResponse.ok("Todos los archivos del producto han sido eliminados")
                .toResponseEntity();
    }

    @GetMapping("/validate-file")
    @Operation(summary = "Validar archivo antes de subir")
    public ResponseEntity<ExtendedBaseResponse<Boolean>> validateFile(
            @Parameter(description = "Tipo de archivo", example = "IMAGE")
            @RequestParam String fileType,

            @Parameter(description = "Tamaño del archivo en bytes", example = "102400")
            @RequestParam Long fileSize,

            @Parameter(description = "Nombre del archivo", example = "product.jpg")
            @RequestParam String fileName) {

        // Validación básica
        boolean isValidName = multimediaService.isValidFileName(fileName);
        boolean isValidSize = false;

        // Determinar tamaño máximo según tipo
        long maxSize = switch (FileType.valueOf(fileType.toUpperCase())) {
            case IMAGE -> 10 * 1024 * 1024; // 10MB
            case VIDEO -> 100 * 1024 * 1024; // 100MB
            case DOCUMENT -> 20 * 1024 * 1024; // 20MB
        };

        isValidSize = fileSize <= maxSize;

        boolean isValid = isValidName && isValidSize;

        return ExtendedBaseResponse.ok(isValid,
                        isValid ? "Archivo válido" : "Archivo no válido")
                .toResponseEntity();
    }

}