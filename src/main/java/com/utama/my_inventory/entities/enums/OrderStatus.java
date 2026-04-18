package com.utama.my_inventory.entities.enums;

public enum OrderStatus {
    PENDIENTE("Pendiente"),
    ENVIADO("Enviado"),
    PARCIAL("Recibido Parcial"),
    COMPLETADO("Completado"),
    CANCELADO("Cancelado");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}