package com.utama.my_inventory.entities.enums;

public enum PackagingCalculationType {
    FIJO,           // Ej: 1 etiqueta térmica, 1 caja
    AREA_CM2,       // Ej: Cartón o papel burbuja basado en la superficie del producto
    PERIMETRO_CM    // Ej: Cinta de embalaje basada en las medidas de la caja
}