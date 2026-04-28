package com.utama.my_inventory.configs;

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
    public CommandLineRunner loadData() {
        return args -> {
            System.out.println("✅ Inicialización completada - Sin precarga de datos");
        };
    }
}