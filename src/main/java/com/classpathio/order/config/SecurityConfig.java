package com.classpathio.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import com.classpathio.order.security.JwtAuthConverter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthConverter jwtAuthConverter = new JwtAuthConverter();

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                // path-level + scopes + roles
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/**")
                    .hasAuthority("SCOPE_orders.reader")

                .requestMatchers(HttpMethod.POST, "/api/v1/orders/**")
                    .hasAuthority("SCOPE_orders.create")

                .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/**")
                    .hasAuthority("SCOPE_orders.delete")                
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt( jwt -> jwt.jwtAuthenticationConverter(getJwtAuthConverter())));

        return http.build();
    }

    // Custom converter to read "cognito:groups" + custom scopes
    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> getJwtAuthConverter() {
        return new JwtAuthConverter()::convert;
    }
}
