package com.utama.my_inventory.configs.data;

import com.utama.my_inventory.entities.Category;
import com.utama.my_inventory.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryDataLoader {

    private final CategoryRepository categoryRepository;

    @Transactional
    public void load() {
        if (categoryRepository.count() > 0) {
            return;
        }

        List<Category> categories = Arrays.asList(
                Category.builder().name("Living").active(true).build(),
                Category.builder().name("Dormitorio").active(true).build(),
                Category.builder().name("Cocina & Mesa").active(true).build(),
                Category.builder().name("Baño").active(true).build(),
                Category.builder().name("Decoración").active(true).build(),
                Category.builder().name("Personalizados").active(true).build()
        );

        categoryRepository.saveAll(categories);
        System.out.println("✅ Categorías creadas: " + categories.size());
    }

    @Transactional
    public void loadEssential() {
        if (categoryRepository.count() > 0) {
            return;
        }

        List<Category> categories = Arrays.asList(
                Category.builder().name("Living").active(true).build(),
                Category.builder().name("Dormitorio").active(true).build(),
                Category.builder().name("Decoración").active(true).build()
        );

        categoryRepository.saveAll(categories);
        System.out.println("✅ Categorías esenciales creadas: " + categories.size());
    }

    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + name));
    }
}