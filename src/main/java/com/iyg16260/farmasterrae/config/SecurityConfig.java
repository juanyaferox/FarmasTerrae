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
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    @Value ("${app.env:dev}") // por defecto desarollo
    private String appEnv;

    @Value ("${app.security}")
    private String securityKey;

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
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/auth/changePassword/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/order/**").authenticated()
                        .anyRequest().permitAll()
                );
        // Configuración para H2
        if ("dev".equals(appEnv)) {
            http.csrf(csrf ->
                            csrf.ignoringRequestMatchers("/h2-console/**"))
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

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/auth").setViewName("auth/login");
    }


}
