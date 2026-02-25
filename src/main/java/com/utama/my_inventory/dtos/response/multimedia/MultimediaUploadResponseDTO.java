package com.utama.my_inventory.dtos.response.multimedia;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de subida de archivo")
public record MultimediaUploadResponseDTO(

        @Schema(description = "ID del archivo creado", example = "1")
        Long fileId,

        @Schema(description = "URL del archivo subido")
        String fileUrl,

        @Schema(description = "URL segura (HTTPS)")
        String secureUrl,

        @Schema(description = "Nombre del archivo", example = "product-image.jpg")
        String fileName,

        @Schema(description = "Tamaño del archivo en bytes", example = "102400")
        Long fileSize,

        @Schema(description = "Tipo MIME", example = "image/jpeg")
        String mimeType,

        @Schema(description = "Public ID en Cloudinary")
        String publicId,

        @Schema(description = "Formato del archivo", example = "jpg")
        String format,

        @Schema(description = "Mensaje de confirmación", example = "Imagen subida exitosamente")
        String message
) {}