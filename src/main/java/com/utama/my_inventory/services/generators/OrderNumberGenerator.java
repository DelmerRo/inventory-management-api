package com.utama.my_inventory.services.generators;

import com.utama.my_inventory.repositories.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private final PurchaseOrderRepository purchaseOrderRepository;

    private static final String PREFIX = "PO";
    private static final int SEQUENCE_LENGTH = 6;

    /**
     * Genera el siguiente número de pedido basado en el máximo registro del año corriente.
     */
    public String generateNextOrderNumber() {
        String currentYear = String.valueOf(LocalDate.now().getYear());

        // Obtiene la secuencia más alta de la base de datos (ej: 9)
        Long lastSequence = purchaseOrderRepository.getLastSequenceByYear(currentYear);
        long nextSequence = (lastSequence != null ? lastSequence : 0) + 1;

        return String.format("%s-%s-%0" + SEQUENCE_LENGTH + "d", PREFIX, currentYear, nextSequence);
    }
}
