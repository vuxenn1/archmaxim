package com.archmaxim.config;

import com.archmaxim.security.JwtFilter;
import com.archmaxim.security.JwtService;
import com.archmaxim.security.SecurityProblemHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Configuration
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String LECTURER = "LECTURER";
    private static final String STUDENT = "STUDENT";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtService jwtService,
                                    SecurityProblemHandler problemHandler,
                                    ArchmaximProperties properties) throws Exception {
        boolean swaggerEnabled = properties.swagger().enabled();
        List<String> allowedOrigins = properties.cors().allowedOrigins();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .addFilterBefore(new JwtFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/error").permitAll();
                    auth.requestMatchers(POST, "/auth/login").permitAll();
                    if (swaggerEnabled) {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    }
                    auth.requestMatchers(GET, "/auth/me").authenticated();
                    auth.requestMatchers(POST, "/auth/change-password").authenticated();

                    auth.requestMatchers(GET, "/courses/mine").hasAnyRole(LECTURER, STUDENT);
                    auth.requestMatchers(GET, "/faculties", "/departments", "/courses").authenticated();
                    auth.requestMatchers(POST, "/faculties", "/departments", "/courses", "/courses/*/aliases")
                            .hasRole(ADMIN);
                    auth.requestMatchers(PUT, "/faculties/*", "/departments/*", "/courses/*").hasRole(ADMIN);
                    auth.requestMatchers(DELETE, "/courses/*/aliases/*").hasRole(ADMIN);
                    auth.requestMatchers("/lecturers/**", "/students/**", "/course-lecturers/**", "/enrollments/**")
                            .hasRole(ADMIN);

                    auth.requestMatchers(GET, "/files/search", "/files/*/download").authenticated();
                    auth.requestMatchers(POST, "/files/upload").hasRole(LECTURER);
                    auth.requestMatchers(GET, "/files", "/files/index/status").hasRole(ADMIN);
                    auth.requestMatchers(POST, "/files/index").hasRole(ADMIN);
                    // Lecturers may only remove their own uploads; FileService checks ownership.
                    auth.requestMatchers(DELETE, "/files/*").hasAnyRole(ADMIN, LECTURER);

                    auth.anyRequest().denyAll();
                });

        if (allowedOrigins.isEmpty()) {
            http.cors(AbstractHttpConfigurer::disable);
        } else {
            http.cors(cors -> cors.configurationSource(corsSource(allowedOrigins)));
        }
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static UrlBasedCorsConfigurationSource corsSource(List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        config.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION, HttpHeaders.RETRY_AFTER));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
