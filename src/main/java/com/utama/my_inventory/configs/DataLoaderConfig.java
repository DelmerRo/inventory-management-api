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
            SupplierDataLoader supplierDataLoader,
            ProductDataLoader productDataLoader,
            InventoryMovementDataLoader inventoryMovementDataLoader,
            MultimediaFileDataLoader multimediaFileDataLoader) {

        return args -> {
            boolean isProduction = isProductionEnvironment();

            if (isProduction) {
                System.out.println("🔄 Cargando datos esenciales para producción...");
                categoryDataLoader.loadEssential();
                subcategoryDataLoader.loadEssential();
                supplierDataLoader.loadEssential();
                // No cargamos productos ni archivos en producción
            } else {
                System.out.println("🚧 Cargando datos completos para desarrollo...");
                System.out.println("=".repeat(50));

                // Orden importante: dependencias primero
                categoryDataLoader.load();
                subcategoryDataLoader.load();
                supplierDataLoader.load();
                productDataLoader.load();
                inventoryMovementDataLoader.load();
                multimediaFileDataLoader.load();

                System.out.println("=".repeat(50));
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