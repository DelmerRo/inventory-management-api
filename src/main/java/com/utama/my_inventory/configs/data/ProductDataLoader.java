package com.utama.my_inventory.configs.data;

import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.Subcategory;
import com.utama.my_inventory.repositories.ProductRepository;
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
    private final SubcategoryDataLoader subcategoryDataLoader;
    private final SupplierDataLoader supplierDataLoader;
    private static final Random random = new Random();
    private final AtomicInteger skuCounter = new AtomicInteger(1000);

    @Transactional
    public void load() {
        if (productRepository.count() > 0) {
            return;
        }

        // Asegurar que las dependencias existen
        ensureDependenciesExist();

        List<Product> products = new ArrayList<>();
        Map<String, List<String>> subcategoriesByCategory = subcategoryDataLoader.getSubcategoriesGroupedByCategory();

        // Definir 3 subcategorías sin productos
        Set<String> subcategoriesWithoutProducts = new HashSet<>(Arrays.asList(
                "Cubrecamas & acolchados",  // De Dormitorio
                "Organización de Cocina",    // De Cocina & Mesa
                "Relojes"                    // De Decoración
        ));

        int totalProducts = 0;

        // Crear productos para cada subcategoría (excepto las 3 excluidas)
        for (Map.Entry<String, List<String>> entry : subcategoriesByCategory.entrySet()) {
            String categoryName = entry.getKey();
            List<String> subcategoryNames = entry.getValue();

            for (String subcategoryName : subcategoryNames) {
                if (subcategoriesWithoutProducts.contains(subcategoryName)) {
                    System.out.println("⏭️  Omitiendo productos para: " + subcategoryName);
                    continue;
                }

                // Crear 6 productos para esta subcategoría
                List<Product> subcategoryProducts = createProductsForSubcategory(
                        categoryName,
                        subcategoryName,
                        6
                );

                products.addAll(subcategoryProducts);
                totalProducts += subcategoryProducts.size();

                System.out.println("✅ " + subcategoryName + ": " + subcategoryProducts.size() + " productos");
            }
        }

        // Guardar todos los productos
        productRepository.saveAll(products);

        // Resumen final
        System.out.println("\n📊 RESUMEN DE PRODUCTOS:");
        System.out.println("================================");
        System.out.println("Total productos creados: " + totalProducts);
        System.out.println("Subcategorías con productos: " + (subcategoriesByCategory.values().stream()
                .flatMap(List::stream)
                .count() - subcategoriesWithoutProducts.size()));
        System.out.println("Subcategorías sin productos: " + subcategoriesWithoutProducts.size());
        System.out.println("Proveedores utilizados: " + supplierDataLoader.getAllSuppliers().size());
        System.out.println("================================");
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

    private List<Product> createProductsForSubcategory(String categoryName, String subcategoryName, int count) {
        List<Product> products = new ArrayList<>();
        Subcategory subcategory = subcategoryDataLoader.getSubcategoryByNameAndCategory(subcategoryName, categoryName);

        // Obtener nombres de productos específicos por subcategoría
        List<String> productNames = getProductNamesForSubcategory(subcategoryName);

        for (int i = 1; i <= count; i++) {
            String productName;
            if (i <= productNames.size()) {
                productName = productNames.get(i - 1);
            } else {
                productName = "Producto " + i + " - " + subcategoryName;
            }

            // VALORES MÁS CONSERVADORES QUE CUMPLEN CON @Digits
            Product product = Product.builder()
                    .sku(generateSKU(categoryName, subcategoryName, i))
                    .name(productName)
                    .description("Descripción de " + productName.toLowerCase() + " para " + subcategoryName.toLowerCase())
                    .costPrice(generateRandomPrice(500, 5000))
                    .salePrice(generateRandomPrice(1000, 10000))
                    .currentStock(random.nextInt(50) + 10)
                    .subcategory(subcategory)
                    .supplier(supplierDataLoader.getRandomSupplier())
                    // MÁXIMO: 5 enteros (hasta 99999) y 3 decimales
                    .weight(BigDecimal.valueOf(random.nextDouble() * 99.999 + 0.001) // 0.001-100.0 kg
                            .setScale(3, RoundingMode.HALF_UP))
                    // MÁXIMO: 5 enteros (hasta 99999) y 2 decimales
                    .length(BigDecimal.valueOf(random.nextDouble() * 999.99 + 0.01) // 0.01-1000.0 cm
                            .setScale(2, RoundingMode.HALF_UP))
                    .width(BigDecimal.valueOf(random.nextDouble() * 99.99 + 0.01)   // 0.01-100.0 cm
                            .setScale(2, RoundingMode.HALF_UP))
                    .height(BigDecimal.valueOf(random.nextDouble() * 49.99 + 0.01)  // 0.01-50.0 cm
                            .setScale(2, RoundingMode.HALF_UP))
                    .measureUnit("cm")
                    .active(true)
                    .build();

            // Asegurar que salePrice >= costPrice
            if (product.getSalePrice().compareTo(product.getCostPrice()) < 0) {
                product.setSalePrice(product.getCostPrice().multiply(BigDecimal.valueOf(1.3)));
            }

            products.add(product);
        }

        return products;
    }

    private List<String> getProductNamesForSubcategory(String subcategoryName) {
        Map<String, List<String>> productExamples = new HashMap<>();

        // Living
        productExamples.put("Alfombras", Arrays.asList(
                "Alfombra Persa Roja", "Alfombra Moderna Gris", "Alfombra de Yute Natural",
                "Alfombra Shaggy Blanca", "Alfombra Geométrica", "Alfombra Vintage"
        ));
        productExamples.put("Almohadones", Arrays.asList(
                "Almohadón Terciopelo Azul", "Almohadón Lino Crudo", "Almohadón Estampado Tribal",
                "Almohadón Chenille", "Almohadón Pluma", "Almohadón Bordado"
        ));
        productExamples.put("Cortinas", Arrays.asList(
                "Cortina Blackout Beige", "Cortina Sheer Blanca", "Cortina Panel Japones",
                "Cortina Romana", "Cortina Persianas", "Cortina Voile"
        ));
        productExamples.put("Mantas", Arrays.asList(
                "Manta de Lana Merino", "Manta de Algodón", "Manta de Punto Grueso",
                "Manta Polar", "Manta Tejida", "Manta Estampada"
        ));

        // Dormitorio
        productExamples.put("Ropa de cama", Arrays.asList(
                "Juego de Sábanas Algodón 300 Hilos", "Funda Nórdica Queen", "Cobertor Invierno",
                "Sábana Ajustable", "Juego Cama 4 Piezas", "Funda Edredón"
        ));
        productExamples.put("Mantas", Arrays.asList(
                "Manta Cama Doble", "Manta Eléctrica", "Manta de Vellón",
                "Manta Tejida a Mano", "Manta Microfibra", "Manta Ponderada"
        ));
        productExamples.put("Pie de cama", Arrays.asList(
                "Banco Pie Cama", "Organizador Bajo Cama", "Cajonera Pie Cama",
                "Baúl Decorativo", "Banquito Tocado", "Estante Pie Cama"
        ));
        productExamples.put("Textiles decorativos", Arrays.asList(
                "Cortina Dormitorio", "Alfombra Dormitorio", "Cojín Cama",
                "Cubrelecho", "Colcha Patchwork", "Cubre Cabecero"
        ));

        // Cocina & Mesa
        productExamples.put("Mantelería", Arrays.asList(
                "Mantel Rectangular Lino", "Individuales de Tela", "Mantel Cuadros",
                "Mantel Circular", "Individuales PVC", "Mantel Ajustable"
        ));
        productExamples.put("Textil de Cocina", Arrays.asList(
                "Delantal Chef", "Guante Horno", "Paño Cocina",
                "Agarraderas", "Cubreolla", "Repasador"
        ));
        productExamples.put("Mesa", Arrays.asList(
                "Servilletas Tela", "Portaservilletas Madera", "Centro Mesa Florero",
                "Salero Pimentero", "Mantelera", "Sousplat"
        ));
        productExamples.put("Accesorios de mesa", Arrays.asList(
                "Aceitera", "Vinagrera", "Molinillo Pimienta",
                "Salsera", "Azucarera", "Mantequillero"
        ));

        // Baño
        productExamples.put("Toallas", Arrays.asList(
                "Toalla Baño Algodón", "Toalla Manos", "Toalla Rostro",
                "Toalla Spa", "Juego Toallas", "Toalla Bebé"
        ));
        productExamples.put("Alfombras de baño", Arrays.asList(
                "Alfombra Baño Antideslizante", "Alfombra Ducha", "Alfombra Vanitory",
                "Set Alfombras", "Alfombra Memory Foam", "Alfombra Microfibra"
        ));
        productExamples.put("Cortinas de baño", Arrays.asList(
                "Cortina Baño PVC", "Cortina Tela", "Cortina Liner",
                "Cortina Estampada", "Cortina Opaca", "Cortina Transparente"
        ));
        productExamples.put("Batas", Arrays.asList(
                "Bata Algodón", "Albornoz", "Bata Seda",
                "Bata Corta", "Bata Capucha", "Bata Hotel"
        ));
        productExamples.put("Accesorios de baño", Arrays.asList(
                "Portarrollos", "Jabonera", "Estante Ducha",
                "Ganchos Toalla", "Espejo Baño", "Organizador"
        ));

        // Decoración
        productExamples.put("Velas & Aromas", Arrays.asList(
                "Vela de Soja Aromática", "Difusor Aromático", "Velón Decorativo",
                "Vela Flotante", "Sahumerios", "Spray Ambiental"
        ));
        productExamples.put("Objetos decorativos", Arrays.asList(
                "Estatuilla Cerámica", "Figura Resina", "Escultura Moderna",
                "Portaretratos", "Cenicero Decorativo", "Estatuilla Animal"
        ));
        productExamples.put("Jarrones", Arrays.asList(
                "Jarrón de Cerámica Alta", "Jarrón de Vidrio", "Jarrón Minimalista",
                "Jarrón Buda", "Jarrón Metal", "Jarrón Barro"
        ));
        productExamples.put("Cuadros & Láminas", Arrays.asList(
                "Cuadro Abstracto", "Lámina Botánica", "Retro Wall Art",
                "Cuadro Paisaje", "Lámina Frases", "Cuadro Moderno"
        ));
        productExamples.put("Decoración de pared", Arrays.asList(
                "Espejo Redondo", "Reloj Pared", "Adhesivo Mural",
                "Estante Flotante", "Pizarra", "Pergamino"
        ));
        productExamples.put("Bandejas decorativas", Arrays.asList(
                "Bandeja Madera", "Bandeja Metal", "Bandeja Rattan",
                "Bandeja Servicio", "Bandeja Centro", "Bandeja Minimal"
        ));
        productExamples.put("Centros de mesa", Arrays.asList(
                "Centro Mesa Floral", "Centro Mesa Velas", "Centro Mesa Moderno",
                "Centro Mesa Rustico", "Centro Mesa Cristal", "Centro Mesa Natural"
        ));
        productExamples.put("Iluminación", Arrays.asList(
                "Lámpara de Pie", "Velador Moderno", "Lámpara Colgante",
                "Lámpara Mesa", "Luz LED", "Lámpara Industrial"
        ));
        productExamples.put("Espejos", Arrays.asList(
                "Espejo Redondo Pared", "Espejo Rectangular", "Espejo de Pie",
                "Espejo Sol", "Espejo Vintage", "Espejo Baño"
        ));

        // Personalizados
        productExamples.put("Textil personalizado", Arrays.asList(
                "Almohadón Personalizado", "Manta con Nombre", "Toalla Bordada",
                "Delantal Personalizado", "Mantel Bordado", "Cojín Foto"
        ));
        productExamples.put("Regalos personalizados", Arrays.asList(
                "Jarrón Grabado", "Cuadro Personalizado", "Bandeja Iniciales",
                "Portaretratos Familiar", "Llavero Personalizado", "Taza Foto"
        ));
        productExamples.put("Pedidos a medida", Arrays.asList(
                "Cortina a Medida", "Alfombra Personalizada", "Cabecero a Medida",
                "Mantel Medidas Especiales", "Cubrelecho Medida", "Funda Sofá Personalizada"
        ));
        productExamples.put("Bordados", Arrays.asList(
                "Toalla Bordada", "Delantal Bordado", "Cojín Bordado",
                "Mantel Bordado", "Bata Bordada", "Paño Bordado"
        ));
        productExamples.put("Grabados", Arrays.asList(
                "Jarrón Grabado Láser", "Bandeja Grabada", "Espejo Grabado",
                "Marco Grabado", "Cenicero Grabado", "Portavelas Grabado"
        ));
        productExamples.put("Monogramas", Arrays.asList(
                "Toalla Monograma", "Almohadón Iniciales", "Mantel Monograma",
                "Delantal Iniciales", "Bata Monograma", "Cojín Inicial"
        ));

        return productExamples.getOrDefault(subcategoryName, Arrays.asList(
                "Producto A - " + subcategoryName,
                "Producto B - " + subcategoryName,
                "Producto C - " + subcategoryName,
                "Producto D - " + subcategoryName,
                "Producto E - " + subcategoryName,
                "Producto F - " + subcategoryName
        ));
    }

    private String generateSKU(String category, String subcategory, int number) {
        String catCode = getCategoryCode(category);
        String subCatCode = getSubcategoryCode(subcategory);
        return String.format("%s-%s-%04d", catCode, subCatCode, skuCounter.getAndIncrement());
    }

    private String getCategoryCode(String category) {
        Map<String, String> codes = Map.of(
                "Living", "LIV",
                "Dormitorio", "DOR",
                "Cocina & Mesa", "CKM",
                "Baño", "BAN",
                "Decoración", "DEC",
                "Personalizados", "PER"
        );
        return codes.getOrDefault(category, "GEN");
    }

    private String getSubcategoryCode(String subcategory) {
        // Tomar las primeras 3 letras en mayúsculas, reemplazar espacios
        String code = subcategory.substring(0, Math.min(3, subcategory.length()))
                .toUpperCase()
                .replace(" ", "")
                .replace("&", "Y");
        return code.length() < 3 ? code + "X" : code;
    }

    private BigDecimal generateRandomPrice(int min, int max) {
        int price = random.nextInt(max - min) + min;
        return BigDecimal.valueOf(price);
    }
}