package com.utama.my_inventory.configs.data;

import com.utama.my_inventory.entities.Category;
import com.utama.my_inventory.entities.Subcategory;
import com.utama.my_inventory.repositories.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SubcategoryDataLoader {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryDataLoader categoryDataLoader;

    @Transactional
    public void load() {

        // Asegurar categorías
        categoryDataLoader.load();

        // Living
        Category living = categoryDataLoader.getCategoryByName("Living");
        saveIfNotExists("Alfombras", living);
        saveIfNotExists("Almohadones", living);
        saveIfNotExists("Cortinas", living);
        saveIfNotExists("Mantas", living);

        // Dormitorio
        Category dormitorio = categoryDataLoader.getCategoryByName("Dormitorio");
        saveIfNotExists("Ropa de cama", dormitorio);
        saveIfNotExists("Cubrecamas & acolchados", dormitorio);
        saveIfNotExists("Mantas", dormitorio);
        saveIfNotExists("Pie de cama", dormitorio);
        saveIfNotExists("Textiles decorativos", dormitorio);
        saveIfNotExists("Organización de Ropa y Calzado", dormitorio);

        // Cocina
        Category cocina = categoryDataLoader.getCategoryByName("Cocina & Mesa");
        saveIfNotExists("Mantelería", cocina);
        saveIfNotExists("Textil de Cocina", cocina);
        saveIfNotExists("Mesa", cocina);
        saveIfNotExists("Organización de Cocina", cocina);
        saveIfNotExists("Accesorios de mesa", cocina);
        saveIfNotExists("Vajilla & Servicio de Mesa", cocina);
        saveIfNotExists("Accesorios de cocina", cocina);

        // Baño
        Category bano = categoryDataLoader.getCategoryByName("Baño");
        saveIfNotExists("Toallas", bano);
        saveIfNotExists("Alfombras de baño", bano);
        saveIfNotExists("Cortinas de baño", bano);
        saveIfNotExists("Batas", bano);
        saveIfNotExists("Accesorios de baño", bano);

        // Decoración
        Category decoracion = categoryDataLoader.getCategoryByName("Decoración");
        saveIfNotExists("Velas & Aromas", decoracion);
        saveIfNotExists("Objetos decorativos", decoracion);
        saveIfNotExists("Jarrones", decoracion);
        saveIfNotExists("Cuadros & Láminas", decoracion);
        saveIfNotExists("Decoración de pared", decoracion);
        saveIfNotExists("Bandejas decorativas", decoracion);
        saveIfNotExists("Centros de mesa", decoracion);
        saveIfNotExists("Iluminación", decoracion);
        saveIfNotExists("Espejos", decoracion);
        saveIfNotExists("Relojes", decoracion);

        // Personalizados
        Category personalizados = categoryDataLoader.getCategoryByName("Personalizados");
        saveIfNotExists("Textil personalizado", personalizados);
        saveIfNotExists("Regalos personalizados", personalizados);
        saveIfNotExists("Pedidos a medida", personalizados);
        saveIfNotExists("Bordados", personalizados);
        saveIfNotExists("Grabados", personalizados);
        saveIfNotExists("Monogramas", personalizados);

        // Cuidado del Hogar
        Category cuidado = categoryDataLoader.getCategoryByName("Cuidado del Hogar");
        saveIfNotExists("Lavandería", cuidado);
        saveIfNotExists("Organización", cuidado);
        saveIfNotExists("Guardado", cuidado);
        saveIfNotExists("Limpieza", cuidado);
        saveIfNotExists("Planchado", cuidado);

        System.out.println("✅ Subcategorías sincronizadas");
    }

    @Transactional
    public void loadEssential() {

        categoryDataLoader.loadEssential();

        Category living = categoryDataLoader.getCategoryByName("Living");
        saveIfNotExists("Alfombras", living);
        saveIfNotExists("Almohadones", living);

        Category dormitorio = categoryDataLoader.getCategoryByName("Dormitorio");
        saveIfNotExists("Ropa de cama", dormitorio);

        Category decoracion = categoryDataLoader.getCategoryByName("Decoración");
        saveIfNotExists("Velas & Aromas", decoracion);
        saveIfNotExists("Cuadros & Láminas", decoracion);

        System.out.println("✅ Subcategorías esenciales sincronizadas");
    }

    private void saveIfNotExists(String name, Category category) {
        if (!subcategoryRepository.existsByNameAndCategoryName(name, category.getName())) {
            subcategoryRepository.save(
                    Subcategory.builder()
                            .name(name)
                            .category(category)
                            .build()
            );
        }
    }

    public Subcategory getSubcategoryByNameAndCategory(String subcategoryName, String categoryName) {
        return subcategoryRepository
                .findByNameAndCategoryName(subcategoryName, categoryName)
                .orElseThrow(() -> new RuntimeException(
                        "Subcategoría no encontrada: " + subcategoryName +
                                " en categoría " + categoryName
                ));
    }

    public List<Subcategory> getAllSubcategories() {
        return subcategoryRepository.findAll();
    }
}