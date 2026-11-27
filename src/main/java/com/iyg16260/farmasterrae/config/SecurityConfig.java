package com.iyg16260.farmasterrae.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.util.StringUtils;

// @EnableMethodSecurity: @PreAuthorize guards the admin-only service methods (e.g. UserService) even if a
// controller or URL rule ever exposes them by mistake. Denials throw AuthorizationDeniedException, an
// AccessDeniedException, which GlobalExceptionHandler already turns into a 403.
@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfig {
    @Value("${spring.h2.console.enabled:false}") private boolean h2ConsoleEnabled;
    @Value("${spring.h2.console.path:/h2-console}") private String h2ConsolePath;
    @Value("${app.security}") private String securityKey;

    @PostConstruct
    void validateSecurityKey() {
        if (!StringUtils.hasText(securityKey) || "uniqueAndSecret".equals(securityKey) || securityKey.length() < 32)
            throw new IllegalStateException("app.security must be a non-default secret of at least 32 characters");
    }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }
    @Bean public HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }

    @Bean
    public RememberMeServices rememberMeServices(UserDetailsService users, PersistentTokenRepository tokenRepository) {
        StrictPersistentRememberMeServices services = new StrictPersistentRememberMeServices(securityKey, users, tokenRepository);
        services.setTokenValiditySeconds(604800);
        return services;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService users,
                                                   RememberMeServices rememberMeServices, SessionRegistry sessionRegistry) throws Exception {
        String h2Pattern = StringUtils.trimTrailingCharacter(h2ConsolePath, '/') + "/**";
        http.userDetailsService(users)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(h2Pattern).hasRole("ADMIN")
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/auth/changePassword", "/auth/changePassword/**", "/order/**").authenticated()
                        .requestMatchers("/", "/products/**", "/cart/**", "/auth", "/auth/register", "/auth/forgotPassword", "/auth/resetPassword", "/css/**", "/assets/**", "/js/**", "/favicon.ico", "/common/error", "/error").permitAll()
                        .anyRequest().authenticated());
        if (h2ConsoleEnabled) http.csrf(csrf -> csrf.ignoringRequestMatchers(h2Pattern))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-eval' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: blob: https: http://localhost:9000; frame-ancestors 'self'; object-src 'none'; base-uri 'self'; form-action 'self'"))
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(policy -> policy.policy("geolocation=(), microphone=(), camera=()")));
        http.formLogin(form -> form.loginPage("/auth").defaultSuccessUrl("/", false).permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/auth?logout")
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID", "remember-me").permitAll())
                .rememberMe(remember -> remember.rememberMeServices(rememberMeServices))
                .sessionManagement(session -> session.sessionFixation().changeSessionId()
                        .maximumSessions(1).sessionRegistry(sessionRegistry).expiredUrl("/auth?expired"));
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/auth"))
                .accessDeniedHandler((req, res, deniedEx) -> {
                    if (res.isCommitted()) return;
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    req.setAttribute("errorCode", 403);
                    req.setAttribute("errorMessageLabel", "No tienes permisos para acceder a este recurso.");
                    req.getRequestDispatcher("/common/error").forward(req, res);
                }));
        return http.build();
    }
}
