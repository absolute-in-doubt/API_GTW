package com.innowise.apigateway.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "spring.security")
public record SecurityProperties(
        List<String> publicPaths
) {
}
