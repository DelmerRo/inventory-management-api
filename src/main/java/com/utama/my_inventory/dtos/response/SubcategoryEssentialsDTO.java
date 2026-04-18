package com.utama.my_inventory.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Información esencial de subcategoría")
public record SubcategoryEssentialsDTO(

        @Schema(description = "ID de la subcategoría", example = "1")
        Long id,

        @Schema(description = "Nombre de la subcategoría", example = "Alfombras")
        String name,

        @Schema(description = "Indica si está activa", example = "true")
        Boolean active,

        @Schema(description = "ID de la categoría padre", example = "1")
        Long categoryId,

        @Schema(description = "Nombre de la categoría padre", example = "Living")
        String categoryName
) {}