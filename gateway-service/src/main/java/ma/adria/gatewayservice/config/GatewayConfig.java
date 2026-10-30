package ma.adria.gatewayservice.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // REST
                .route("account-service-rest", r -> r.path("/api/accounts/**")
                        .uri("lb://account-service"))
                .route("transaction-service-rest", r -> r.path("/api/transactions/**")
                        .uri("lb://transaction-service"))
                // GraphQL + Swagger + autres endpoints via prefixe
                .route("account-service", r -> r.path("/account-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://account-service"))
                .route("transaction-service", r -> r.path("/transaction-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://transaction-service"))
                // Agent IA (sera cree a la Partie E)
                .route("bank-agent", r -> r.path("/api/chat/**")
                        .uri("lb://bank-agent"))
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}