package com.vokabelnetz.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Vokabelnetz API}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("Vokabelnetz API")
                .version("1.0.0")
                .description("""
                    REST API for Vokabelnetz - German vocabulary learning platform.

                    ## Features
                    - User authentication with JWT
                    - Spaced repetition learning (SM-2 algorithm)
                    - Elo rating system for adaptive difficulty
                    - Progress tracking and statistics
                    - Multi-language support (TR/EN)

                    ## Authentication
                    Most endpoints require JWT Bearer token authentication.
                    Obtain tokens via `/auth/login` or `/auth/register`.
                    """)
                .contact(new Contact()
                    .name("Vokabelnetz Team")
                    .email("support@vokabelnetz.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080/api")
                    .description("Development Server"),
                new Server()
                    .url("https://api.vokabelnetz.com/api")
                    .description("Production Server")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter your JWT access token")));
    }
}
