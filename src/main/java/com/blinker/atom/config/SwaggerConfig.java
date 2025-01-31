package com.blinker.atom.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(
            title = "Blinker API Docs",
            description = "Blinker API Docs",
            version = "v1"
    ), servers = {@Server(url = "/", description = "Default Server URL")}
)
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String securityJwtName = "access-token";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securityJwtName);
        Components components = new Components()
                .addSecuritySchemes(securityJwtName, new SecurityScheme()
                        .name(securityJwtName)
                        .type(Type.APIKEY)
                        .in(In.HEADER));

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
