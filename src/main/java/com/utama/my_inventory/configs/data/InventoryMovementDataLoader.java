package com.utama.my_inventory.configs.data;

import com.utama.my_inventory.entities.InventoryMovement;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.enums.MovementType;
import com.utama.my_inventory.repositories.InventoryMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class InventoryMovementDataLoader {

    private final InventoryMovementRepository inventoryMovementRepository;
    private final ProductDataLoader productDataLoader;
    private static final Random random = new Random();

    @Transactional
    public void load() {
        if (inventoryMovementRepository.count() > 0) {
            return;
        }

        // Asegurar que productos existen
        if (productDataLoader.getProductRepository().count() == 0) {
            productDataLoader.load();
        }

        List<InventoryMovement> movements = new ArrayList<>();
        List<Product> allProducts = productDataLoader.getProductRepository().findAll();

        // Crear 3-5 movimientos por producto
        for (Product product : allProducts) {
            int movementCount = random.nextInt(3) + 3; // 3-5 movimientos

            // Primero algunas ENTRADAS para crear stock inicial
            for (int i = 0; i < movementCount; i++) {
                InventoryMovement movement = createInitialStockMovement(product, i + 1);
                movements.add(movement);

                // Actualizar stock del producto (simulando lo que haría el sistema)
                if (movement.getMovementType() == MovementType.ENTRADA) {
                    product.addStock(movement.getQuantity(), movement.getReason(), movement.getRegisteredBy());
                } else if (movement.getMovementType() == MovementType.SALIDA) {
                    product.removeStock(movement.getQuantity(), movement.getReason(), movement.getRegisteredBy());
                }
            }

            // Añadir un ajuste ocasional
            if (random.nextBoolean()) {
                InventoryMovement adjustment = createAdjustmentMovement(product);
                movements.add(adjustment);
            }
        }

        inventoryMovementRepository.saveAll(movements);
        System.out.println("✅ Movimientos de inventario creados: " + movements.size());

        // Mostrar resumen
        printMovementSummary(movements);
    }

    @Transactional
    public void loadEssential() {
        if (inventoryMovementRepository.count() > 0) {
            return;
        }

        // Solo movimientos esenciales
        List<InventoryMovement> essentialMovements = new ArrayList<>();
        List<Product> products = productDataLoader.getProductRepository().findAll();

        // Tomar solo 10 productos para movimientos esenciales
        int limit = Math.min(10, products.size());
        for (int i = 0; i < limit; i++) {
            Product product = products.get(i);

            // CORRECCIÓN: Asegurar que unitCost tenga máximo 2 decimales
            BigDecimal unitCost = product.getCostPrice() != null ?
                    product.getCostPrice().setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_UP);

            // Una entrada inicial por producto
            InventoryMovement entry = InventoryMovement.builder()
                    .product(product)
                    .quantity(random.nextInt(50) + 10) // 10-60 unidades
                    .movementType(MovementType.ENTRADA)
                    .reason("Stock inicial")
                    .registeredBy("admin")
                    .unitCost(unitCost) // Ya está con 2 decimales
                    .movementDate(LocalDateTime.now().minusDays(random.nextInt(30)))
                    .build();

            essentialMovements.add(entry);
            product.addStock(entry.getQuantity(), entry.getReason(), entry.getRegisteredBy());
        }

        inventoryMovementRepository.saveAll(essentialMovements);
        System.out.println("✅ Movimientos esenciales creados: " + essentialMovements.size());
    }

    private InventoryMovement createInitialStockMovement(Product product, int movementNumber) {
        MovementType type;
        String reason;
        BigDecimal unitCost = null;

        // Primer movimiento siempre es ENTRADA (stock inicial)
        if (movementNumber == 1) {
            type = MovementType.ENTRADA;
            reason = "Stock inicial";
            // CORRECCIÓN: Asegurar que tenga máximo 2 decimales
            unitCost = product.getCostPrice() != null ?
                    product.getCostPrice().setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Movimientos subsiguientes pueden ser ENTRADA o SALIDA
            type = random.nextBoolean() ? MovementType.ENTRADA : MovementType.SALIDA;

            if (type == MovementType.ENTRADA) {
                reason = getRandomEntryReason();
                // CORRECCIÓN: Asegurar 2 decimales después de la multiplicación
                if (product.getCostPrice() != null) {
                    double factor = 0.9 + random.nextDouble() * 0.2; // +/- 10%
                    unitCost = product.getCostPrice()
                            .multiply(BigDecimal.valueOf(factor))
                            .setScale(2, RoundingMode.HALF_UP);
                } else {
                    unitCost = BigDecimal.valueOf(1000 + random.nextInt(4000))
                            .setScale(2, RoundingMode.HALF_UP);
                }
            } else {
                reason = getRandomExitReason();
                // Para salidas, unitCost es el último costo de entrada (ya con 2 decimales)
                unitCost = product.getCostPrice() != null ?
                        product.getCostPrice().setScale(2, RoundingMode.HALF_UP) :
                        BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_UP);
            }
        }

        return InventoryMovement.builder()
                .product(product)
                .quantity(getRandomQuantity(type, movementNumber))
                .movementType(type)
                .reason(reason)
                .registeredBy(getRandomUser())
                .unitCost(unitCost)
                .movementDate(LocalDateTime.now()
                        .minusDays(random.nextInt(90))
                        .minusHours(random.nextInt(24)))
                .build();
    }

    private InventoryMovement createAdjustmentMovement(Product product) {
        int currentStock = product.getCurrentStock();
        int adjustment = random.nextInt(10) - 5; // -5 a +5 unidades
        int newQuantity = Math.max(1, Math.abs(adjustment));

        MovementType type = adjustment > 0 ? MovementType.AJUSTE : MovementType.AJUSTE;
        String reason = adjustment > 0 ? "Ajuste positivo por inventario físico"
                : "Ajuste negativo por inventario físico";

        // CORRECCIÓN: Asegurar 2 decimales
        BigDecimal unitCost = product.getCostPrice() != null ?
                product.getCostPrice().setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_UP);

        return InventoryMovement.builder()
                .product(product)
                .quantity(newQuantity)
                .movementType(type)
                .reason(reason)
                .registeredBy("inventory_manager")
                .unitCost(unitCost)
                .movementDate(LocalDateTime.now()
                        .minusDays(random.nextInt(30))
                        .minusHours(random.nextInt(24)))
                .build();
    }

    private int getRandomQuantity(MovementType type, int movementNumber) {
        if (movementNumber == 1) {
            // Stock inicial más grande
            return random.nextInt(100) + 50; // 50-150 unidades
        } else if (type == MovementType.ENTRADA) {
            return random.nextInt(30) + 10; // 10-40 unidades
        } else {
            return random.nextInt(20) + 5; // 5-25 unidades
        }
    }

    private String getRandomEntryReason() {
        String[] reasons = {
                "Reabastecimiento de proveedor",
                "Compra mayorista",
                "Importación",
                "Devolución de cliente",
                "Producción interna",
                "Transferencia de sucursal"
        };
        return reasons[random.nextInt(reasons.length)];
    }

    private String getRandomExitReason() {
        String[] reasons = {
                "Venta a cliente",
                "Muestra para exhibición",
                "Daño o pérdida",
                "Transferencia a otra sucursal",
                "Donación",
                "Uso interno"
        };
        return reasons[random.nextInt(reasons.length)];
    }

    private String getRandomUser() {
        String[] users = {
                "admin",
                "maria_gonzalez",
                "carlos_lopez",
                "ana_rodriguez",
                "pedro_sanchez",
                "laura_fernandez"
        };
        return users[random.nextInt(users.length)];
    }

    private void printMovementSummary(List<InventoryMovement> movements) {
        long entryCount = movements.stream()
                .filter(m -> m.getMovementType() == MovementType.ENTRADA)
                .count();

        long exitCount = movements.stream()
                .filter(m -> m.getMovementType() == MovementType.SALIDA)
                .count();

        long adjustmentCount = movements.stream()
                .filter(m -> m.getMovementType() == MovementType.AJUSTE)
                .count();

        System.out.println("\n📊 RESUMEN DE MOVIMIENTOS:");
        System.out.println("================================");
        System.out.println("Total movimientos: " + movements.size());
        System.out.println("Entradas (ENTRADA): " + entryCount);
        System.out.println("Salidas (SALIDA): " + exitCount);
        System.out.println("Ajustes (AJUSTE): " + adjustmentCount);

        BigDecimal totalValue = movements.stream()
                .map(InventoryMovement::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("Valor total movido: $" + totalValue.setScale(2, RoundingMode.HALF_UP));
        System.out.println("================================");
    }
}