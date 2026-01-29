package com.utama.my_inventory.dtos.response.multimedia;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.utama.my_inventory.entities.enums.FileType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta con información del archivo multimedia")
public record MultimediaFileResponseDTO(

        @Schema(description = "ID del archivo", example = "1")
        Long id,

        @Schema(description = "ID del producto asociado", example = "1")
        Long productId,

        @Schema(description = "Tipo de archivo", example = "IMAGE")
        FileType fileType,

        @Schema(description = "URL del archivo en Cloudinary")
        String fileUrl,

        @Schema(description = "URL segura (HTTPS) del archivo")
        String secureUrl,

        @Schema(description = "Nombre del archivo", example = "product-image.jpg")
        String fileName,

        @Schema(description = "Tamaño del archivo en bytes", example = "102400")
        Long fileSize,

        @Schema(description = "Extensión del archivo", example = "jpg")
        String fileExtension,

        @Schema(description = "Tipo MIME", example = "image/jpeg")
        String mimeType,

        @Schema(description = "Ancho de la imagen (si es imagen)", example = "800")
        Integer width,

        @Schema(description = "Alto de la imagen (si es imagen)", example = "600")
        Integer height,

        @Schema(description = "Fecha de subida")
        LocalDateTime uploadedAt
) {}