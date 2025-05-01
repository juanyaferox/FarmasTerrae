package com.iyg16260.farmasterrae.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {


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
                        // Permitimos el acceso a la página de login y a los recursos estáticos (styles y js)
                        .requestMatchers("/login",
                                "/styles/**",
                                "/js/**",
                                "/dist/**",
                                "/cart/**",
                                "/products/**",
                                "/",
                                "/h2-console/**",
                                "/common/error"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/order/**").authenticated()
                )
                // Configuración para H2
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .formLogin(form -> form
                        .loginPage("/login")  // Configuramos la URL de la página de login personalizada
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("uniqueAndSecret")  // Clave para cifrar las cookies de remember-me
                        .tokenValiditySeconds(604800)  // 7 días
                        .userDetailsService(userDetailsService)
                )
                .sessionManagement(session -> session
                        .sessionFixation().newSession()  // Crea una nueva sesión después de autenticación
                        .maximumSessions(1).expiredUrl("/login?expired")  // Expira sesiones viejas
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
        registry.addViewController("/login").setViewName("auth/login");
    }


}
