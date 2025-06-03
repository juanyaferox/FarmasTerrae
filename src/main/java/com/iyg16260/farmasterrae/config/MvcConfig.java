package com.iyg16260.farmasterrae.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Clase de configuración para definir enlaces entre vista y template sin crear un controlador
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/common/error").setViewName("common/error");
        registry.addViewController("/auth").setViewName("auth/login");
    }
}