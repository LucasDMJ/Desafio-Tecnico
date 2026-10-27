package com.desafio.customers.config;

import com.desafio.customers.security.RestAccessDeniedHandler;
import com.desafio.customers.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API stateless com Basic Auth
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Consultas: USER e ADMIN
                        .requestMatchers(HttpMethod.GET, "/customers", "/customers/**").hasAnyRole("USER", "ADMIN")
                        // Escrita: somente ADMIN
                        .requestMatchers(HttpMethod.POST, "/customers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/customers/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/customers/*").hasRole("ADMIN")
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AppSecurityProperties properties, PasswordEncoder encoder) {
        List<UserDetails> users = properties.users().stream()
                .map(u -> User.withUsername(u.username())
                        .password(encoder.encode(u.password()))
                        .roles(u.role())
                        .build())
                .toList();
        return new InMemoryUserDetailsManager(users);
    }
}
