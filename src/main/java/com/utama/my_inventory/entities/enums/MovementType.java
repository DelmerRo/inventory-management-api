package com.utama.my_inventory.entities.enums;


/**
 * Tipos de movimiento de inventario
 * ENTRADA: Incrementa el stock (compra, producción)
 * SALIDA:  Decrementa el stock (venta, consumo)
 * AJUSTE:  Ajusta el stock a un valor específico (inventario físico)
 */
public enum MovementType {
    ENTRADA,    // Aumenta el inventario
    SALIDA,     // Disminuye el inventario
    AJUSTE      // Ajusta al valor exacto
}