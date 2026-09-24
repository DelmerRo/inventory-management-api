package com.utama.my_inventory.dtos.response.dashboard;

public record DashboardResponseDTO(
        ImmobilizedCapitalDTO capital,
        ProfitabilityDTO profitability,
        PredictionDTO predictions
) {}
