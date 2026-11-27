package com.iyg16260.farmasterrae.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.HexFormat;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestAuditFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            String errorId = (String) request.getAttribute("errorId");
            if (errorId == null) {
                errorId = UUID.randomUUID().toString();
                request.setAttribute("errorId", errorId);
            }
            Principal principal = request.getUserPrincipal();
            String username = principal != null ? clean(principal.getName()) : "anonymous";
            log.error("Excepción [{}] | Usuario: {} | IP: {} | Agente: {} | URI: {} | Método: {} | Sesión: {} | Tipo: {}",
                    errorId, username, request.getRemoteAddr(), clean(request.getHeader("User-Agent")),
                    clean(request.getRequestURI()), clean(request.getMethod()), sessionHash(request),
                    ex.getClass().getSimpleName(), ex);
            throw ex;
        }
    }

    private String sessionHash(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) return "none";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(session.getId().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 8);
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.replace("\r", "").replace("\n", "");
    }
}
