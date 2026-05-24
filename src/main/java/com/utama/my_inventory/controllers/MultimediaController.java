package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaFileResponseDTO;
import com.utama.my_inventory.dtos.response.multimedia.MultimediaUploadResponseDTO;
import com.utama.my_inventory.services.MultimediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}