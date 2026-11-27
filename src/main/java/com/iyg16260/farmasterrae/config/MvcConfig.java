package com.iyg16260.farmasterrae.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Clase de configuración para definir enlaces entre vista y template sin crear un controlador
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/auth").setViewName("auth/login");
    }

    /**
     * Resuelve el token CSRF diferido antes de renderizar vistas Thymeleaf (excluyendo recursos estáticos)
     * para que los formularios POST situados tras el búfer inicial de respuesta puedan crear sesión en Tomcat.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!(handler instanceof ResourceHttpRequestHandler)
                        && ("GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod()))) {
                    Object csrfAttr = request.getAttribute(CsrfToken.class.getName());
                    if (csrfAttr instanceof CsrfToken token) {
                        token.getToken();
                    }
                }
                return true;
            }
        });
    }
}
