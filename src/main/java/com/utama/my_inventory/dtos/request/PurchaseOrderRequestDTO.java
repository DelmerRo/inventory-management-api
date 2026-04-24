package com.utama.my_inventory.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para crear o actualizar un pedido de compra")
public class PurchaseOrderRequestDTO {

    @NotNull(message = "ID del proveedor es obligatorio")
    @Schema(description = "ID del proveedor", example = "1")
    private Long supplierId;

    @NotNull(message = "Fecha del pedido es obligatoria")
    @Schema(description = "Fecha del pedido", example = "2024-12-18T10:00:00")
    private LocalDateTime orderDate;

    @Schema(description = "Fecha estimada de entrega", example = "2024-12-25T10:00:00")
    private LocalDateTime expectedDeliveryDate;

    @Size(max = 500)
    @Schema(description = "Notas adicionales", example = "Pedido urgente - Stock bajo")
    private String notes;

    @Builder.Default
    @Schema(description = "Lista de productos del pedido")
    private List<PurchaseOrderItemRequestDTO> items = new ArrayList<>();
}