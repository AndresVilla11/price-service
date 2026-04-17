package com.inditex.price.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI priceServiceOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("https://api-dev.inditex.com").description("Development"),
                        new Server().url("https://api.inditex.com").description("Production")
                ))
                .components(new Components()
                        .addSchemas("ProblemDetail", problemDetailSchema()))
                .addTagsItem(new Tag()
                        .name("Prices")
                        .description("Applicable price resolution for products across brands"));
    }

    private Info apiInfo() {
        return new Info()
                .title("Price Service API")
                .description("""
                        Resolves the applicable price for a product given a brand and application date.
                        
                        When multiple prices overlap in time, the one with the highest **priority** 
                        value is returned. All prices are returned in the currency defined per brand.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Inditex Platform Team")
                        .email("platform@inditex.com")
                        .url("https://inditex.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://inditex.com/legal"));
    }

    private Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .description("RFC 7807 Problem Detail")
                .addProperty("type", new StringSchema().example("https://api.inditex.com/errors/price-not-found"))
                .addProperty("title", new StringSchema().example("Price Not Found"))
                .addProperty("status", new IntegerSchema().example(404))
                .addProperty("detail", new StringSchema().example("No applicable price found for productId=35455"))
                .addProperty("timestamp", new StringSchema().format("date-time"))
                .addProperty("path", new StringSchema().example("/api/v1/prices"));
    }
}
