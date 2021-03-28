package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(5, 7);
    }

    @Bean
    SecurityWebFilterChain authorization(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(Customizer.withDefaults())
                .authorizeExchange(ae -> ae.anyExchange().authenticated())
                .build();
    }

    @Bean
    MapReactiveUserDetailsService authentication() {
        return new MapReactiveUserDetailsService(User.withDefaultPasswordEncoder().username("jlong").roles("USER").password("pw").build());
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return
                rlb
                        .routes()
                        .route(rs -> rs
                                .path("/proxy")
                                .filters(fs -> fs
                                        .setPath("/")
                                        .requestRateLimiter(rlc -> rlc
                                                .setRateLimiter(redisRateLimiter())
                                        )
                                )
                                .uri("https://start.spring.io")
                        )
                        .build();
    }


}
