package com.utama.my_inventory.configs.data;

import com.utama.my_inventory.entities.Category;
import com.utama.my_inventory.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CategoryDataLoader {

    private final CategoryRepository categoryRepository;

    @Transactional
    public void load() {

        saveIfNotExists("Living");
        saveIfNotExists("Dormitorio");
        saveIfNotExists("Cocina & Mesa");
        saveIfNotExists("Baño");
        saveIfNotExists("Decoración");
        saveIfNotExists("Personalizados");
        saveIfNotExists("Cuidado del Hogar");

        System.out.println("✅ Categorías sincronizadas");
    }

    @Transactional
    public void loadEssential() {

        saveIfNotExists("Living");
        saveIfNotExists("Dormitorio");
        saveIfNotExists("Decoración");

        System.out.println("✅ Categorías esenciales sincronizadas");
    }

    private void saveIfNotExists(String name) {
        if (!categoryRepository.existsByName(name)) {
            categoryRepository.save(
                    Category.builder()
                            .name(name)
                            .active(true)
                            .build()
            );
        }
    }

    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + name));
    }

    public void loadIfEmpty() {
        // Solo carga si no hay categorías
        if (categoryRepository.count() == 0) {
            System.out.println("📂 Cargando categorías esenciales...");
            load();
        } else {
            System.out.println("✅ Las categorías ya existen, omitiendo carga.");
        }
    }
}