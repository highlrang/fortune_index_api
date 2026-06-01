package com.hwcompany.fortune_index.common

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("fortune_index_api")
                    .description("Fortune Index API 문서")
                    .version("v1")
            )
            .servers(listOf(Server().url("/")))
            .components(
                Components()
                    .addSecuritySchemes(
                        AUTHORIZATION_SCHEME,
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .name("Authorization")
                    )
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList(AUTHORIZATION_SCHEME)
            )

    @Bean
    fun mobileApiGroup(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("mobile")
            .pathsToMatch("/api/**")
            .pathsToExclude("/api/admin/**")
            .build()

    @Bean
    fun adminApiGroup(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("/api/admin/**")
            .build()

    companion object {
        private const val AUTHORIZATION_SCHEME = "Authorization"
    }
}
