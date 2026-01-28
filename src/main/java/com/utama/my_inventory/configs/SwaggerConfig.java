package com.utama.my_inventory.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Documentation: Inventory Management Api")
                        .version("1.0")
                        .description("""
                            Documentación de la API de Inventory Management Api.

                            🔗 Repositorio en GitHub: [Inventory Management Api Backend](https://github.com/DelmerRo/inventory-management-api))
                            """)
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("Utama-Deco"))
                )
                .addServersItem(new Server().url("https://inventory-management.onrender.com").description("Servidor de Producción"))
                .addServersItem(new Server().url("http://localhost:9092").description("Servidor de Desarrollo"))
                .addSecurityItem(new SecurityRequirement().addList("TOKEN"))
                .components(new Components()
                        .addSecuritySchemes("TOKEN",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
}