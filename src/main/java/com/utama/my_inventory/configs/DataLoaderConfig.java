package com.utama.my_inventory.configs;

import com.utama.my_inventory.configs.data.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
public class DataLoaderConfig {

    private final Environment environment;

    @Bean
    public CommandLineRunner loadData(
            CategoryDataLoader categoryDataLoader,
            SubcategoryDataLoader subcategoryDataLoader,
            SupplierDataLoader supplierDataLoader) {

        return args -> {
            boolean isProduction = isProductionEnvironment();

            if (isProduction) {
                System.out.println("✅ Modo producción - No se cargan datos automáticos");
                // Solo cargar si la tabla está vacía
                categoryDataLoader.loadIfEmpty();
                subcategoryDataLoader.loadIfEmpty();
                supplierDataLoader.loadIfEmpty();
            } else {
                System.out.println("🚧 Desarrollo - Cargando datos de prueba...");
                categoryDataLoader.load();
                subcategoryDataLoader.load();
                supplierDataLoader.load();
                System.out.println("✅ Carga de datos completada");
            }
        };
    }

    private boolean isProductionEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}