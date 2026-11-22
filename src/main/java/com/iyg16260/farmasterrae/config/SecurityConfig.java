package com.iyg16260.farmasterrae.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyg16260.farmasterrae.dto.response.AuthenticatedUserDTO;
import com.iyg16260.farmasterrae.dto.response.ErrorResponseDTO;
import com.iyg16260.farmasterrae.dto.response.MessageResponseDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value ("${app.env:dev}") // por defecto desarollo
    private String appEnv;

    @Value ("${app.security}")
    private String securityKey;

    /**
     * Orígenes permitidos para CORS separados por comas. Vacío = CORS deshabilitado (mismo origen)
     */
    @Value ("${app.cors.allowed-origins:}")
    private List<String> corsAllowedOrigins;

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UserDetailsService userDetailsService) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/auth/change-password", "/api/auth/me").authenticated()
                        .requestMatchers("/api/order/**").authenticated()
                        .anyRequest().permitAll()
                );

        // CSRF: el token se envía en la cookie XSRF-TOKEN y se devuelve en la cabecera X-XSRF-TOKEN
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName(null); // carga el token en cada petición para que la cookie esté siempre disponible
        http.csrf(csrf -> {
            csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(csrfRequestHandler);
            // Configuración para H2
            if ("dev".equals(appEnv))
                csrf.ignoringRequestMatchers("/h2-console/**");
        });
        if ("dev".equals(appEnv)) {
            http.headers(headers ->
                    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        }

        // CORS solo si se han configurado orígenes permitidos
        if (!corsAllowedOrigins.isEmpty()) {
            http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        }

        http.formLogin(form -> form
                        .loginPage("/api/auth/login") // Evita la página de login HTML que genera Spring por defecto
                        .loginProcessingUrl("/api/auth/login") // POST con los parámetros username y password
                        .successHandler((req, res, auth) ->
                                writeJson(res, HttpStatus.OK, AuthenticatedUserDTO.from(auth)))
                        .failureHandler((req, res, authEx) ->
                                writeJson(res, HttpStatus.UNAUTHORIZED,
                                        ErrorResponseDTO.of(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos")))
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) ->
                                writeJson(res, HttpStatus.OK, new MessageResponseDTO("Sesión cerrada correctamente")))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key(securityKey)  // Clave para cifrar las cookies de remember-me
                        .tokenValiditySeconds(604800)  // 7 días
                        .userDetailsService(userDetailsService)
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()  // Migra a la nueva sesión después de autenticación
                        .maximumSessions(1)  // Expira sesiones viejas
                        .expiredSessionStrategy(event ->
                                writeJson(event.getResponse(), HttpStatus.UNAUTHORIZED,
                                        ErrorResponseDTO.of(HttpStatus.UNAUTHORIZED,
                                                "La sesión ha expirado, vuelve a iniciar sesión")))
                )
                .exceptionHandling(ex -> ex
                        // para 401:
                        .authenticationEntryPoint((req, res, authEx) ->
                                writeJson(res, HttpStatus.UNAUTHORIZED,
                                        ErrorResponseDTO.of(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión")))
                        // para 403:
                        .accessDeniedHandler((req, res, deniedEx) ->
                                writeJson(res, HttpStatus.FORBIDDEN,
                                        ErrorResponseDTO.of(HttpStatus.FORBIDDEN, deniedEx instanceof CsrfException
                                                ? "Token CSRF inválido o ausente"
                                                : "Acceso denegado")))
                );
        return http.build();
    }

    /**
     * Configuración CORS para los orígenes permitidos, con credenciales (cookies de sesión)
     */
    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /**
     * Escribe una respuesta JSON desde los manejadores de Spring Security
     */
    private void writeJson(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
