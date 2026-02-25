package com.utama.my_inventory.dtos.request;

import com.utama.my_inventory.entities.enums.MovementType;

import java.time.LocalDateTime;

public record MovementFilter(
        Long productId,
        MovementType movementType,
        String registeredBy,
        LocalDateTime startDate,
        LocalDateTime endDate
) {}