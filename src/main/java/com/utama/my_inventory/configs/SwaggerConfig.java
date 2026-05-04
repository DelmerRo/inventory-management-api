package com.utama.my_inventory.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${app.environment:local}")
    private String environment;

    @Value("${server.port:9092}")
    private String serverPort;

    @Bean
    @Profile("!prod")  // ✅ Swagger solo disponible en perfiles que NO son de producción
    OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Documentation: Inventory Management API")
                        .version("1.0")
                        .description("""
                            ## 📦 Inventory Management API
                            
                            API para la gestión completa de inventario, productos, pedidos de compra, proveedores y categorías.
                            
                            ### 🔐 Autenticación
                            Esta API utiliza autenticación JWT. Para probar los endpoints:
                            1. Usa el endpoint `/api/auth/login` para obtener un token
                            2. Haz clic en el botón **Authorize** e ingresa: `Bearer {tu_token}`
                            
                            ### 🌐 Entornos
                            - **Desarrollo**: `http://localhost:9092`
                            - **Producción**: `https://inventory-management-api-xbpp.onrender.com`
                            
                            ### 📚 Recursos
                            - 🔗 [Repositorio Backend](https://github.com/DelmerRo/inventory-management-api)
                            - 🎨 [Frontend Vercel](https://inventory-management-frontend-utama.vercel.app)
                            """)
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("Utama-Deco")
                                .email("contacto@utama.com")
                                .url("https://utama.com"))
                )
                .servers(getServers())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .name("Bearer Authentication")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                            Ingresa el token JWT obtenido del login.
                                            
                                            Formato: `Bearer {tu_token_jwt}`
                                            """)
                        )
                );
    }

    private List<Server> getServers() {
        List<Server> servers = new ArrayList<>();

        // Servidor de desarrollo local
        Server devServer = new Server();
        devServer.setUrl("http://localhost:" + serverPort);
        devServer.setDescription("🌐 Servidor de Desarrollo Local");
        servers.add(devServer);

        // Servidor de producción en Render
        Server prodServer = new Server();
        prodServer.setUrl("https://inventory-management-api-xbpp.onrender.com");
        prodServer.setDescription("🚀 Servidor de Producción (Render)");
        servers.add(prodServer);

        // Si es entorno local, mostrar también el servidor alternativo
        if ("local".equals(environment)) {
            Server altServer = new Server();
            altServer.setUrl("http://localhost:8080");
            altServer.setDescription("🖥️ Servidor Alternativo (puerto 8080)");
            servers.add(altServer);
        }

        return servers;
    }
}