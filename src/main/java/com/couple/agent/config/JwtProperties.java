package com.couple.agent.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

@Validated
@RefreshScope
@ConfigurationProperties(prefix = "security.jwt")
@Data
public class JwtProperties {

    @NotBlank

    private String secret;

    @Positive
    private long expirationMinutes = 60;

}
