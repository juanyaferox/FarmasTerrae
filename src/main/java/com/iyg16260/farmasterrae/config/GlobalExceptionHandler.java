package com.iyg16260.farmasterrae.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@Order (1)
public class GlobalExceptionHandler {

    /**
     * Captura cualquier tipo de excepción
     * Si existe Referer: añade flashAttribute y redirige allí.
     * Si no existe Referer: muestra common/error con código y mensaje.
     */
    @ExceptionHandler (Exception.class)
    public ModelAndView handleAllExceptions(Exception ex,
                                            RedirectAttributes redirectAttributes,
                                            HttpServletRequest request) {

        // Construcción mensaje del error
        ErrorInfo errorInfo = getErrorInfo(ex);
        String errorId = "(ID: " + UUID.randomUUID() + ")";
        String messageWithId = errorInfo.message() + " " + errorId;

        // Log con la información
        logException(errorId, ex, collectAuditInfo(request));

        // Se determina si se debería redirigir
        if (determineIfShouldRedirect(ex, request)) {
            String referer = request.getHeader("Referer");
            return handleRedirect(redirectAttributes, referer, messageWithId);
        }

        // Caso no tenga dirección, se devuelve una pantalla génerica
        return createErrorModelAndView(errorInfo.status(), errorInfo.message());
    }

    /**
     * Record para construir los mensajes
     *
     * @param status  Código HTTP
     * @param message
     */
    private record ErrorInfo(HttpStatusCode status, String message) {
    }

    /**
     * Construye el mensaje de error en base a la excepción a través del record ErrorInfo
     */
    private ErrorInfo getErrorInfo(Exception ex) {
        return switch (ex) {
            case ResponseStatusException rse -> new ErrorInfo(rse.getStatusCode(), rse.getReason());

            case TransactionSystemException ignored ->
                    new ErrorInfo(HttpStatus.BAD_REQUEST, "Error en la transacción. Verifique los datos.");

            case HttpRequestMethodNotSupportedException methodEx ->
                    new ErrorInfo(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido: " + methodEx.getMethod());

            case NoHandlerFoundException ignored -> new ErrorInfo(HttpStatus.NOT_FOUND, "Recurso no encontrado");

            case AccessDeniedException ignored ->
                    new ErrorInfo(HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este recurso");

            case BindException be -> {
                String errors = processBindingErrors(be);
                yield new ErrorInfo(HttpStatus.BAD_REQUEST, "Errores de validación: " + errors);
            }

            case null, default -> new ErrorInfo(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
        };
    }

    private String processBindingErrors(BindException be) {
        return be.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    /**
     * Determina si debería devolver un redirect en base al tipo de error y la cabecera
     */
    private boolean determineIfShouldRedirect(Exception ex, HttpServletRequest request) {
        return switch (ex) {
            case ResponseStatusException ignore -> request.getHeader("Referer") != null;
            case TransactionSystemException ignore -> request.getHeader("Referer") != null;
            default -> false;
        };
    }

    /**
     * Se redirige añadiendo el error en el redirect
     */
    private ModelAndView handleRedirect(RedirectAttributes redirectAttributes,
                                        String referer, String message) {
        redirectAttributes.addFlashAttribute("errorMessage", message);
        return new ModelAndView("redirect:" + referer);
    }

    /**
     * Se devuelve una pantalla genérica con el error
     */
    private ModelAndView createErrorModelAndView(HttpStatusCode status, String message) {
        ModelAndView mav = new ModelAndView("common/error");
        mav.setStatus(status);
        mav.addObject("errorCode", status.value());
        mav.addObject("errorMessageLabel", message);
        return mav;
    }

    /**
     * Record para construir la auditoria con la información del usuario
     */
    private record AuditInfo(
            String username,
            String remoteAddress,
            String userAgent,
            String requestUri,
            String method,
            String sessionId
    ) {
    }

    /**
     * Construye la auditoría con la información del usuario
     */
    AuditInfo collectAuditInfo(HttpServletRequest request) {
        String username = getCurrentUsername();
        String remoteAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String sessionId = request.getSession(false) != null ?
                request.getSession().getId() : "No session";

        return new AuditInfo(username, remoteAddress, userAgent, requestUri, method, sessionId);

    }

    private String getCurrentUsername() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("anonymous");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Construye el mensaje de error de manera detallada con la información del usuario
     */
    private void logException(String errorId, Exception ex, AuditInfo auditInfo) {
        log.error("Excepción [{}] | Usuario: {} | IP: {} | URI: {} | Método: {} | Sesión: {} | " +
                        "Mensaje: {} | Tipo: {}",
                errorId,
                auditInfo.username(),
                auditInfo.remoteAddress(),
                auditInfo.requestUri(),
                auditInfo.method(),
                auditInfo.sessionId(),
                ex.getMessage(),
                ex.getClass().getSimpleName());
    }
}