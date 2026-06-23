package com.innowise.apigateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.PathContainer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private final SecurityProperties properties;
    private final List<PathPattern> compiledPublicPathPatterns;
    private final PathPatternParser parser = new PathPatternParser();

    public SecurityConfig(SecurityProperties properties){
        this.properties = properties;
        this.compiledPublicPathPatterns = properties.publicPaths().stream().map(parser::parse).toList();
    }


    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                  ServerAuthenticationConverter jwtBearerTokenConverter){
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOriginPatterns(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    return config;
                }))
                .authorizeExchange(authorizeExchangeSpec ->
                    authorizeExchangeSpec
                            .pathMatchers(properties.publicPaths().toArray(String[]::new)).permitAll()
                            .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2
                                .jwt(Customizer.withDefaults())
                                .bearerTokenConverter(jwtBearerTokenConverter)
                )
                .build();
    }

    /**
     * returns null on public paths making the JwtFilter, provided by oauth2ResourceServer, think that there were no Bearer token
     * @return
     */
    @Bean
    ServerAuthenticationConverter skipPublicPathsConverter(){
        ServerAuthenticationConverter delegate = new ServerBearerTokenAuthenticationConverter();
        return exchange -> {
            boolean isPublic = compiledPublicPathPatterns.stream()
                    .anyMatch(p ->
                            p.matches(PathContainer.parsePath(exchange.getRequest().getPath().value()))
                    );
            return isPublic ? Mono.empty() : delegate.convert(exchange);
        };
    }
}
