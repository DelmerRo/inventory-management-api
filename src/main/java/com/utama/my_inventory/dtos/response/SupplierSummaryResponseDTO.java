package com.utama.my_inventory.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta resumida del proveedor para listados")
public record SupplierSummaryResponseDTO(

        @Schema(description = "ID del proveedor", example = "1")
        Long id,

        @Schema(description = "Nombre del proveedor", example = "Tecnología S.A.")
        String name,

        @Schema(description = "Persona de contacto", example = "Juan Pérez")
        String contactPerson,

        @Schema(description = "Email de contacto", example = "contacto@tecnologia.com")
        String email,

        @Schema(description = "Teléfono de contacto", example = "+51 999 888 777")
        String phone,

        @Schema(description = "Cantidad de productos", example = "15")
        Long productCount,

        @Schema(description = "Indica si está activo", example = "true")
        Boolean active
) {}