package com.utama.my_inventory.dtos.request.multimedia;

import com.utama.my_inventory.entities.enums.FileType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "DTO para subir archivo multimedia")
public record MultimediaUploadRequestDTO(

        @NotNull(message = "Archivo es obligatorio")
        @Schema(description = "Archivo a subir")
        MultipartFile file,

        @NotNull(message = "Tipo de archivo es obligatorio")
        @Schema(description = "Tipo de archivo", example = "IMAGE")
        FileType fileType
) {}