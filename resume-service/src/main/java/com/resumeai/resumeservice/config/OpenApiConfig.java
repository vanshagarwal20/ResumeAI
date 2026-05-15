package com.resumeai.resumeservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI configuration for resume-service.
 *
 * This adds:
 * - project metadata
 * - JWT Bearer support in Swagger UI
 * - Authorize button for protected APIs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI resumeServiceOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("ResumeAI Resume Service API")
                        .description("Resume creation, update, duplication, publishing, ATS score, and gallery APIs")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ResumeAI Team")
                                .email("support@resumeai.local"))
                        .license(new License()
                                .name("Internal Project Use")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
