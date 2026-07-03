package com.innowise.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "application.security")
public record SecurityProperties(
        List<String> publicPaths
) {
}
