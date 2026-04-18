package com.utama.my_inventory.configs.data;

import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.ProductSupplier;
import com.utama.my_inventory.entities.Subcategory;
import com.utama.my_inventory.entities.Supplier;
import com.utama.my_inventory.repositories.ProductRepository;
import com.utama.my_inventory.repositories.ProductSupplierRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class ProductDataLoader {

    @Getter
    private final ProductRepository productRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final SubcategoryDataLoader subcategoryDataLoader;
    private final SupplierDataLoader supplierDataLoader;
    private static final Random random = new Random();
    private final AtomicInteger skuCounter = new AtomicInteger(1000);

    @Transactional
    public void load() {
        if (productRepository.count() > 0) {
            System.out.println("⚠️ Ya existen productos en la base de datos. No se cargarán nuevos.");
            return;
        }

        // Asegurar que las dependencias existen
        ensureDependenciesExist();

        List<Product> products = new ArrayList<>();
        List<ProductSupplier> productSuppliers = new ArrayList<>();

        // Seleccionar solo 10 subcategorías para tener productos variados
        List<SubcategoryInfo> selectedSubcategories = getSelectedSubcategories();

        int totalProducts = 0;
        int productsToCreate = Math.min(10, selectedSubcategories.size());

        System.out.println("\n🎯 Creando " + productsToCreate + " productos (máximo 10)...");
        System.out.println("================================================");

        List<Supplier> allSuppliers = supplierDataLoader.getAllSuppliers();

        for (int i = 0; i < productsToCreate && totalProducts < 10; i++) {
            SubcategoryInfo info = selectedSubcategories.get(i);

            // Crear producto sin proveedor aún
            Product product = createSingleProduct(
                    info.categoryName,
                    info.subcategoryName,
                    info.productName,
                    i + 1
            );

            products.add(product);
            totalProducts++;

            System.out.println("✅ [" + totalProducts + "] " + info.subcategoryName + " - " + info.productName);
        }

        // Guardar productos primero (sin proveedores aún)
        List<Product> savedProducts = productRepository.saveAll(products);

        // Ahora asociar proveedores a cada producto
        System.out.println("\n🔗 Asociando proveedores a los productos...");
        System.out.println("================================================");

        int productIndex = 0;
        for (Product savedProduct : savedProducts) {
            SubcategoryInfo info = selectedSubcategories.get(productIndex);

            // Seleccionar 1 o 2 proveedores aleatorios
            int numSuppliers = random.nextInt(2) + 1; // 1 o 2 proveedores
            List<Supplier> selectedSuppliers = getRandomSuppliers(allSuppliers, numSuppliers);

            boolean isFirst = true;
            for (Supplier supplier : selectedSuppliers) {
                String supplierSku = generateSupplierSku(info.productName, supplier.getName());

                ProductSupplier productSupplier = ProductSupplier.builder()
                        .product(savedProduct)
                        .supplier(supplier)
                        .supplierSku(supplierSku)
                        .isPrimary(isFirst) // El primero es el principal
                        .notes("Proveedor " + (isFirst ? "principal" : "secundario") + " para " + info.productName)
                        .build();

                productSuppliers.add(productSupplier);

                System.out.println("   📦 " + info.productName + " → " + supplier.getName() +
                        " (SKU: " + supplierSku + ", " + (isFirst ? "PRINCIPAL" : "secundario") + ")");

                isFirst = false;
            }
            productIndex++;
        }

        // Guardar todas las relaciones producto-proveedor
        productSupplierRepository.saveAll(productSuppliers);

        // Resumen final
        System.out.println("\n📊 RESUMEN DE PRODUCTOS:");
        System.out.println("================================");
        System.out.println("Total productos creados: " + totalProducts);
        System.out.println("Total relaciones producto-proveedor: " + productSuppliers.size());
        System.out.println("Proveedores disponibles: " + allSuppliers.size());
        System.out.println("================================");
    }

    /**
     * Define 10 subcategorías con nombres de productos específicos
     */
    private List<SubcategoryInfo> getSelectedSubcategories() {
        return Arrays.asList(
                new SubcategoryInfo("Living", "Alfombras", "Alfombra Moderna Gris"),
                new SubcategoryInfo("Living", "Almohadones", "Almohadón Terciopelo Azul"),
                new SubcategoryInfo("Dormitorio", "Ropa de cama", "Juego de Sábanas Algodón"),
                new SubcategoryInfo("Dormitorio", "Mantas", "Manta de Vellón"),
                new SubcategoryInfo("Cocina & Mesa", "Mantelería", "Mantel Rectangular Lino"),
                new SubcategoryInfo("Cocina & Mesa", "Textil de Cocina", "Delantal Chef"),
                new SubcategoryInfo("Baño", "Toallas", "Toalla Baño Algodón"),
                new SubcategoryInfo("Baño", "Alfombras de baño", "Alfombra Baño Antideslizante"),
                new SubcategoryInfo("Decoración", "Velas & Aromas", "Vela de Soja Aromática"),
                new SubcategoryInfo("Decoración", "Jarrones", "Jarrón de Cerámica Alta")
        );
    }

    /**
     * Crear un solo producto con datos realistas (sin proveedor asignado aún)
     */
    private Product createSingleProduct(String categoryName, String subcategoryName, String productName, int index) {
        Subcategory subcategory = subcategoryDataLoader.getSubcategoryByNameAndCategory(subcategoryName, categoryName);

        // Precios realistas para productos de decoración
        BigDecimal costPrice = generateRealisticCostPrice(productName);
        BigDecimal salePrice = costPrice.multiply(BigDecimal.valueOf(1.4 + (random.nextDouble() * 0.3)))
                .setScale(2, RoundingMode.HALF_UP);

        // Generar SKU temporal (se regenerará con el ID real al guardar)
        String tempSku = "TEMP_" + System.currentTimeMillis() + "_" + index;

        Product product = Product.builder()
                .sku(tempSku) // SKU temporal
                .name(productName)
                .description(generateDescription(productName, subcategoryName))
                .costPrice(costPrice)
                .salePrice(salePrice)
                .currentStock(random.nextInt(80) + 20) // Stock entre 20 y 100 unidades
                .subcategory(subcategory)
                .weight(BigDecimal.valueOf(random.nextDouble() * 50 + 0.5) // 0.5 - 50.5 kg
                        .setScale(2, RoundingMode.HALF_UP))
                .length(BigDecimal.valueOf(random.nextDouble() * 200 + 10) // 10 - 210 cm
                        .setScale(1, RoundingMode.HALF_UP))
                .width(BigDecimal.valueOf(random.nextDouble() * 100 + 10) // 10 - 110 cm
                        .setScale(1, RoundingMode.HALF_UP))
                .height(BigDecimal.valueOf(random.nextDouble() * 50 + 1) // 1 - 51 cm
                        .setScale(1, RoundingMode.HALF_UP))
                .measureUnit("cm")
                .active(true)
                .build();

        return product;
    }

    /**
     * Generar SKU del proveedor
     */
    private String generateSupplierSku(String productName, String supplierName) {
        String productCode = productName.substring(0, Math.min(4, productName.length()))
                .toUpperCase()
                .replace(" ", "");
        String supplierCode = supplierName.substring(0, Math.min(3, supplierName.length()))
                .toUpperCase()
                .replace(" ", "");
        int randomNum = random.nextInt(1000);
        return String.format("%s-%s-%03d", supplierCode, productCode, randomNum);
    }

    /**
     * Seleccionar proveedores aleatorios sin repetir
     */
    private List<Supplier> getRandomSuppliers(List<Supplier> suppliers, int count) {
        if (suppliers.isEmpty()) return List.of();

        List<Supplier> shuffled = new ArrayList<>(suppliers);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    /**
     * Generar precio de costo realista según el tipo de producto
     */
    private BigDecimal generateRealisticCostPrice(String productName) {
        int basePrice;

        if (productName.contains("Alfombra")) {
            basePrice = random.nextInt(3000) + 1000; // $1000 - $4000
        } else if (productName.contains("Juego") || productName.contains("Manta")) {
            basePrice = random.nextInt(2000) + 500; // $500 - $2500
        } else if (productName.contains("Toalla") || productName.contains("Delantal")) {
            basePrice = random.nextInt(1000) + 200; // $200 - $1200
        } else if (productName.contains("Jarrón") || productName.contains("Vela")) {
            basePrice = random.nextInt(800) + 150; // $150 - $950
        } else if (productName.contains("Almohadón")) {
            basePrice = random.nextInt(1500) + 300; // $300 - $1800
        } else {
            basePrice = random.nextInt(1500) + 300; // $300 - $1800
        }

        return BigDecimal.valueOf(basePrice);
    }

    /**
     * Generar descripción del producto
     */
    private String generateDescription(String productName, String subcategoryName) {
        return String.format(
                "Descripción de %s para %s. Producto de alta calidad, ideal para decoración y uso diario. " +
                        "Fabricado con materiales premium. Disponible en diversas presentaciones.",
                productName.toLowerCase(),
                subcategoryName.toLowerCase()
        );
    }

    private void ensureDependenciesExist() {
        // Verificar que subcategorías existen
        if (subcategoryDataLoader.getAllSubcategories().isEmpty()) {
            subcategoryDataLoader.load();
        }

        // Verificar que proveedores existen
        if (supplierDataLoader.getAllSuppliers().isEmpty()) {
            supplierDataLoader.load();
        }
    }

    /**
     * Clase auxiliar para almacenar información de subcategoría
     */
    private static class SubcategoryInfo {
        final String categoryName;
        final String subcategoryName;
        final String productName;

        SubcategoryInfo(String categoryName, String subcategoryName, String productName) {
            this.categoryName = categoryName;
            this.subcategoryName = subcategoryName;
            this.productName = productName;
        }
    }
}