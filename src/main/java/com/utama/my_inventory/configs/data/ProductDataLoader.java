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
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductDataLoader {

    @Getter
    private final ProductRepository productRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final SubcategoryDataLoader subcategoryDataLoader;
    private final SupplierDataLoader supplierDataLoader;

    @Transactional
    public void load() {
        if (productRepository.count() > 0) {
            System.out.println("⚠️ Ya existen productos en la base de datos. No se cargarán nuevos.");
            return;
        }

        // Asegurar que las dependencias existen
        ensureDependenciesExist();

        System.out.println("\n🎯 Creando producto de ejemplo...");
        System.out.println("================================================");

        // Definir un solo producto
        String categoryName = "Living";
        String subcategoryName = "Alfombras";
        String productName = "Alfombra Moderna Gris";

        // Obtener subcategoría
        Subcategory subcategory = subcategoryDataLoader.getSubcategoryByNameAndCategory(subcategoryName, categoryName);

        // Obtener proveedor
        Supplier supplier = supplierDataLoader.getDefaultSupplier();

        // Crear producto
        Product product = createSingleProduct(categoryName, subcategoryName, productName);

        // Guardar producto
        Product savedProduct = productRepository.save(product);
        System.out.println("✅ Producto creado: " + productName);

        // Crear relación producto-proveedor
        String supplierSku = generateSupplierSku(productName, supplier.getName());

        ProductSupplier productSupplier = ProductSupplier.builder()
                .product(savedProduct)
                .supplier(supplier)
                .supplierSku(supplierSku)
                .isPrimary(true)
                .notes("Proveedor principal para " + productName)
                .build();

        productSupplierRepository.save(productSupplier);
        System.out.println("   📦 " + productName + " → " + supplier.getName() +
                " (SKU: " + supplierSku + ", PRINCIPAL)");

        // Resumen final
        System.out.println("\n📊 RESUMEN DE PRODUCTOS:");
        System.out.println("================================");
        System.out.println("Total productos creados: 1");
        System.out.println("Total relaciones producto-proveedor: 1");
        System.out.println("Proveedor asociado: " + supplier.getName());
        System.out.println("================================");
    }

    /**
     * Crear un solo producto con datos realistas
     */
    private Product createSingleProduct(String categoryName, String subcategoryName, String productName) {
        Subcategory subcategory = subcategoryDataLoader.getSubcategoryByNameAndCategory(subcategoryName, categoryName);

        // Precios realistas
        BigDecimal costPrice = new BigDecimal("1250.00");
        BigDecimal salePrice = new BigDecimal("1750.00");

        // SKU temporal (se regenerará con el ID real al guardar)
        String tempSku = "TEMP_" + System.currentTimeMillis();

        Product product = Product.builder()
                .sku(tempSku) // SKU temporal
                .name(productName)
                .description(generateDescription(productName, subcategoryName))
                .costPrice(costPrice)
                .salePrice(salePrice)
                .currentStock(50)
                .subcategory(subcategory)
                .weight(new BigDecimal("2.50").setScale(2, RoundingMode.HALF_UP))
                .length(new BigDecimal("160.0").setScale(1, RoundingMode.HALF_UP))
                .width(new BigDecimal("120.0").setScale(1, RoundingMode.HALF_UP))
                .height(new BigDecimal("1.5").setScale(1, RoundingMode.HALF_UP))
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
        return String.format("%s-%s-001", supplierCode, productCode);
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
}