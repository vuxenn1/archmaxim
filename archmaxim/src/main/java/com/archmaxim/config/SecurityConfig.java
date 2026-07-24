package com.archmaxim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/files/search", "/files/*/download").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/files/upload").hasRole("LECTURER")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/faculties").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/departments").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/courses").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/courses/*/aliases").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/lecturers").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/students").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}