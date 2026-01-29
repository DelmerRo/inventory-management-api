package com.utama.my_inventory.dtos.request.multimedia;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.utama.my_inventory.entities.enums.FileType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para crear archivo multimedia desde URL")
public record MultimediaCreateRequestDTO(

        @NotNull(message = "Tipo de archivo es obligatorio")
        @Schema(description = "Tipo de archivo", example = "IMAGE")
        FileType fileType,

        @NotBlank(message = "URL es obligatoria")
        @Pattern(regexp = "^(https?|ftp|file)://.+$",
                message = "URL debe ser válida (http://, https://, ftp://)")
        @Size(max = 500, message = "URL no puede exceder 500 caracteres")
        @Schema(description = "URL del archivo", example = "https://res.cloudinary.com/cloudname/image/upload/product.jpg")
        String fileUrl,

        @NotBlank(message = "Nombre de archivo es obligatorio")
        @Size(max = 255, message = "Nombre no puede exceder 255 caracteres")
        @Schema(description = "Nombre del archivo", example = "product-image.jpg")
        String fileName
) {}