package com.utama.my_inventory.configs.data;

import com.utama.my_inventory.entities.Category;
import com.utama.my_inventory.entities.Subcategory;
import com.utama.my_inventory.repositories.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
public class SubcategoryDataLoader {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryDataLoader categoryDataLoader;

    @Transactional
    public void load() {
        if (subcategoryRepository.count() > 0) {
            return;
        }

        // Primero asegurar que las categorías existen
        if (!categoryRepositoryExists()) {
            categoryDataLoader.load();
        }

        List<Subcategory> subcategories = new ArrayList<>();

        // 1. Living
        Category living = categoryDataLoader.getCategoryByName("Living");
        subcategories.add(Subcategory.builder().name("Alfombras").category(living).build());
        subcategories.add(Subcategory.builder().name("Almohadones").category(living).build());
        subcategories.add(Subcategory.builder().name("Cortinas").category(living).build());
        subcategories.add(Subcategory.builder().name("Mantas").category(living).build());

        // 2. Dormitorio
        Category dormitorio = categoryDataLoader.getCategoryByName("Dormitorio");
        subcategories.add(Subcategory.builder().name("Ropa de cama").category(dormitorio).build());
        subcategories.add(Subcategory.builder().name("Cubrecamas & acolchados").category(dormitorio).build());
        subcategories.add(Subcategory.builder().name("Mantas").category(dormitorio).build());
        subcategories.add(Subcategory.builder().name("Pie de cama").category(dormitorio).build());
        subcategories.add(Subcategory.builder().name("Textiles decorativos").category(dormitorio).build());

        // 3. Cocina & Mesa
        Category cocina = categoryDataLoader.getCategoryByName("Cocina & Mesa");
        subcategories.add(Subcategory.builder().name("Mantelería").category(cocina).build());
        subcategories.add(Subcategory.builder().name("Textil de Cocina").category(cocina).build());
        subcategories.add(Subcategory.builder().name("Mesa").category(cocina).build());
        subcategories.add(Subcategory.builder().name("Organización de Cocina").category(cocina).build());
        subcategories.add(Subcategory.builder().name("Accesorios de mesa").category(cocina).build());

        // 4. Baño
        Category bano = categoryDataLoader.getCategoryByName("Baño");
        subcategories.add(Subcategory.builder().name("Toallas").category(bano).build());
        subcategories.add(Subcategory.builder().name("Alfombras de baño").category(bano).build());
        subcategories.add(Subcategory.builder().name("Cortinas de baño").category(bano).build());
        subcategories.add(Subcategory.builder().name("Batas").category(bano).build());
        subcategories.add(Subcategory.builder().name("Accesorios de baño").category(bano).build());

        // 5. Decoración
        Category decoracion = categoryDataLoader.getCategoryByName("Decoración");
        subcategories.add(Subcategory.builder().name("Velas & Aromas").category(decoracion).build());
        subcategories.add(Subcategory.builder().name("Objetos decorativos").category(decoracion).build());
        subcategories.add(Subcategory.builder().name("Jarrones").category(decoracion).build());
        subcategories.add(Subcategory.builder().name("Cuadros & Láminas").category(decoracion).build());
        subcategories.add(Subcategory.builder().name("Decoración de pared").category(decoracion).build());
        subcategories.add(Subcategory.builder().name("Bandejas decorativas").category(decoracion).build());
        subcategories.add(Subcategory.builder().name("Centros de mesa").category(decoracion).build());
        subcategories.add(Subcategory.builder().name("Iluminación").category(decoracion).build());
        subcategories.add(Subcategory.builder().name("Espejos").category(decoracion).build());
        subcategories.add(Subcategory.builder().name("Relojes").category(decoracion).build());

        // 6. Personalizados
        Category personalizados = categoryDataLoader.getCategoryByName("Personalizados");
        subcategories.add(Subcategory.builder().name("Textil personalizado").category(personalizados).build());
        subcategories.add(Subcategory.builder().name("Regalos personalizados").category(personalizados).build());
        subcategories.add(Subcategory.builder().name("Pedidos a medida").category(personalizados).build());
        subcategories.add(Subcategory.builder().name("Bordados").category(personalizados).build());
        subcategories.add(Subcategory.builder().name("Grabados").category(personalizados).build());
        subcategories.add(Subcategory.builder().name("Monogramas").category(personalizados).build());

        subcategoryRepository.saveAll(subcategories);

        // Mostrar resumen
        System.out.println("✅ Subcategorías creadas: " + subcategories.size());
        printResumen();
    }

    @Transactional
    public void loadEssential() {
        if (subcategoryRepository.count() > 0) {
            return;
        }

        if (!categoryRepositoryExists()) {
            categoryDataLoader.loadEssential();
        }

        List<Subcategory> subcategories = new ArrayList<>();

        // Solo las más esenciales
        Category living = categoryDataLoader.getCategoryByName("Living");
        subcategories.add(Subcategory.builder().name("Alfombras").category(living).build());
        subcategories.add(Subcategory.builder().name("Almohadones").category(living).build());

        Category dormitorio = categoryDataLoader.getCategoryByName("Dormitorio");
        subcategories.add(Subcategory.builder().name("Ropa de cama").category(dormitorio).build());

        Category decoracion = categoryDataLoader.getCategoryByName("Decoración");
        subcategories.add(Subcategory.builder().name("Velas & Aromas").category(decoracion).build());
        subcategories.add(Subcategory.builder().name("Cuadros & Láminas").category(decoracion).build());

        subcategoryRepository.saveAll(subcategories);
        System.out.println("✅ Subcategorías esenciales creadas: " + subcategories.size());
    }

    private boolean categoryRepositoryExists() {
        try {
            return categoryDataLoader.getCategoryByName("Living") != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void printResumen() {
        System.out.println("\n📊 RESUMEN DE SUBCATEGORÍAS:");
        System.out.println("================================");

        // Group by category
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        List<Subcategory> all = subcategoryRepository.findAll();

        for (Subcategory sub : all) {
            String catName = sub.getCategory().getName();
            grouped.computeIfAbsent(catName, k -> new ArrayList<>())
                    .add(sub.getName());
        }

        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            System.out.println("🏷️  " + entry.getKey() + ":");
            for (String subName : entry.getValue()) {
                System.out.println("    └─ " + subName);
            }
            System.out.println();
        }

        System.out.println("================================");
    }

    // MÉTODO QUE FALTABA - AÑADIR ESTO
    public Subcategory getSubcategoryByNameAndCategory(String subcategoryName, String categoryName) {
        return subcategoryRepository.findByNameAndCategoryName(subcategoryName, categoryName)
                .orElseThrow(() -> new RuntimeException(
                        "Subcategoría no encontrada: " + subcategoryName + " en categoría " + categoryName));
    }

    // MÉTODO QUE FALTABA - AÑADIR ESTO
    public List<Subcategory> getAllSubcategories() {
        return subcategoryRepository.findAll();
    }

    // MÉTODO QUE FALTABA - AÑADIR ESTO
    public Map<String, List<String>> getSubcategoriesGroupedByCategory() {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        List<Subcategory> all = subcategoryRepository.findAll();

        for (Subcategory sub : all) {
            String catName = sub.getCategory().getName();
            grouped.computeIfAbsent(catName, k -> new ArrayList<>())
                    .add(sub.getName());
        }

        return grouped;
    }
}