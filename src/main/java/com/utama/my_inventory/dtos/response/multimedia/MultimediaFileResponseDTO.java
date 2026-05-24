package com.utama.my_inventory.dtos.response.multimedia;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de listado de archivos")
public record MultimediaFileResponseDTO(
        @Schema(description = "ID del archivo", example = "1")
        Long id,
        @Schema(description = "Nombre del archivo", example = "product-image.jpg")
        String fileName,
        @Schema(description = "URL del archivo")
        String fileUrl,
        @Schema(description = "Tipo de archivo", example = "IMAGE")
        String fileType,
        @Schema(description = "Fecha de creación")
        LocalDateTime createdAt
) {}