package com.couple.agent.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableKnife4j
public class OpenApiConfig {

    @Bean
    public OpenAPI ynpAgentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ynp-Agent API")
                        .description("LangChain4j powered endpoints documented via Knife4j.")
                        .version("1.0.0"));
    }
}
