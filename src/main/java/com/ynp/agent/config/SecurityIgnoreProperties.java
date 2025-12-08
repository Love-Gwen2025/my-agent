package com.ynp.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@RefreshScope
@Component
@ConfigurationProperties(prefix = "security.ignore")
public class SecurityIgnoreProperties {

    /**
     * Additional white-list paths that should bypass authentication.
     */
    private List<String> whites = new ArrayList<>();
}

