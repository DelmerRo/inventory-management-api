package com.utama.my_inventory.controllers;

import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import com.utama.my_inventory.dtos.response.dashboard.DashboardResponseDTO;
import com.utama.my_inventory.services.impl.DashboardServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "📊 Dashboard Gerencial", description = "Métricas financieras, costos integrados y predicciones operativas del negocio")
public class DashboardController {

    private final DashboardServiceImpl dashboardService;

    @Operation(
            summary = "Obtener consolidado financiero y operativo de la empresa",
            description = "Calcula y retorna en tiempo real una vista consolidada del estado financiero. " +
                    "Incluye el capital inmovilizado en mercadería (Costo Promedio Ponderado) y en insumos de embalaje, " +
                    "rentabilidad proyectada aplicando un margen dinámico, y alertas predictivas de quiebre de stock basadas " +
                    "en la velocidad de ventas de los últimos 30 días."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Métricas del Dashboard calculadas y obtenidas exitosamente"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor al procesar el cálculo de métricas financieras",
                    content = @Content(schema = @Schema(implementation = ExtendedBaseResponse.class))
            )
    })
    @GetMapping("/financials")
    public ResponseEntity<ExtendedBaseResponse<DashboardResponseDTO>> getFinancialDashboard(
            @Parameter(
                    description = "Porcentaje de margen esperado para proyectar ingresos futuros. Se envía como número decimal (ej. 35.0 equivale al 35%). Si se omite, toma 35.0 por defecto.",
                    example = "35.0"
            )
            @RequestParam(defaultValue = "35.0") BigDecimal targetMarginPercentage) {

        DashboardResponseDTO dashboard = dashboardService.getConsolidatedDashboard(targetMarginPercentage);

        return ExtendedBaseResponse.ok(dashboard, "Métricas del Dashboard calculadas exitosamente")
                .toResponseEntity();
    }
}