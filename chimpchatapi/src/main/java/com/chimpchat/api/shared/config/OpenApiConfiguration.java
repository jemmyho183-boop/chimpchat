package com.chimpchat.api.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI chimpChatOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ChimpChat API")
                        .version("v1")
                        .description("API for managing ChimpChat authentication and connected channels."));
    }
}
