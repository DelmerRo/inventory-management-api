// dtos/request/QuickProductRequestDTO.java
package com.utama.my_inventory.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickProductRequestDTO {

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    private String name;

    @NotBlank(message = "El SKU del proveedor es obligatorio")
    @Size(max = 50, message = "SKU no puede exceder 50 caracteres")
    private String supplierSku;

    @NotNull(message = "La subcategoría es obligatoria")
    private Long subcategoryId;
}