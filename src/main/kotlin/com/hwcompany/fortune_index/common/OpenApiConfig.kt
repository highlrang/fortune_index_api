package com.hwcompany.fortune_index.common

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
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
                    .addSecuritySchemes(
                        API_KEY_SCHEME,
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name("API_Key")
                    )
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList(AUTHORIZATION_SCHEME)
                    .addList(API_KEY_SCHEME)
            )

    companion object {
        private const val AUTHORIZATION_SCHEME = "Authorization"
        private const val API_KEY_SCHEME = "API_Key"
    }
}
