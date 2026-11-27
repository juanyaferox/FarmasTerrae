package com.iyg16260.farmasterrae.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Only the dev profile enables the H2 console (H2 is not even on the production classpath). */
    @Value ("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Value ("${spring.h2.console.path:/h2-console}")
    private String h2ConsolePath;

    @Value ("${app.security}")
    private String securityKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UserDetailsService userDetailsService) throws Exception {
        String h2ConsolePattern = StringUtils.trimTrailingCharacter(h2ConsolePath, '/') + "/**";
        http
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Never public, whichever profile enables the console
                        .requestMatchers(h2ConsolePattern).hasRole("ADMIN")
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/auth/changePassword/**").authenticated()
                        .requestMatchers("/order/**").authenticated()
                        .anyRequest().permitAll()
                );
        // Configuración para H2: the console posts forms without CSRF tokens and renders in frames
        if (h2ConsoleEnabled) {
            http.csrf(csrf ->
                            csrf.ignoringRequestMatchers(h2ConsolePattern))
                    .headers(headers ->
                            headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        }
        http.formLogin(form -> form
                        .loginPage("/auth")  // Configuramos la URL de la página de login personalizada
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/auth?logout")
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
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
                        .maximumSessions(1).expiredUrl("/auth?expired")  // Expira sesiones viejas
                )
                .exceptionHandling(ex -> ex
                        // para 401:
                        .authenticationEntryPoint((req, res, authEx) -> {
                            req.setAttribute("errorCode", 401);
                            req.setAttribute("errorMessageLabel", "Debes iniciar sesión");
                            req.getRequestDispatcher("/common/error").forward(req, res);
                        })
                        // para 403:
                        .accessDeniedHandler((req, res, deniedEx) -> {
                            req.setAttribute("errorCode", 403);
                            req.setAttribute("errorMessageLabel", "Acceso denegado");
                            req.getRequestDispatcher("/common/error").forward(req, res);
                        })
                );
        return http.build();
    }
}
