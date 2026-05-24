package com.utama.my_inventory.dtos.response.multimedia;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de subida de archivo")
public record MultimediaUploadResponseDTO(
        @Schema(description = "ID del archivo creado", example = "1")
        Long fileId,
        @Schema(description = "URL del archivo subido")
        String fileUrl,
        @Schema(description = "Public ID en Cloudinary")
        String publicId,
        @Schema(description = "Mensaje de confirmación", example = "Imagen subida exitosamente")
        String message
) {}