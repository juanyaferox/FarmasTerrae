package com.iyg16260.farmasterrae.config;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.AuthenticationException;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.net.URI;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Optional;
import java.util.UUID;

@ControllerAdvice
@Order (1)
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Captura cualquier tipo de excepción
     * Si existe Referer: añade flashAttribute y redirige allí.
     * Si no existe Referer: muestra una pantalla genérica con el código y mensaje.
     */
    @ExceptionHandler (Exception.class)
    public ModelAndView handleAllExceptions(Exception ex,
                                            RedirectAttributes redirectAttributes,
                                            HttpServletRequest request) {


        if (ex instanceof AccessDeniedException denied) throw denied;
        if (ex instanceof AuthenticationException authentication) throw authentication;
        ErrorInfo errorInfo = getErrorInfo(ex);

        String messageError;
        // Solo logear errores 5xx del servidor
        if (shouldLogException(errorInfo)) {
            String errorId = Optional.ofNullable((String) request.getAttribute("errorId"))
                    .orElse(UUID.randomUUID().toString());
            messageError = errorInfo.message() + " " + errorId;
            request.setAttribute("errorId", errorId);
            logException(errorId, ex, collectAuditInfo(request));
        } else {
            messageError = errorInfo.message();
        }

        // Se determina si se debería redirigir
        if (determineIfShouldRedirect(ex, request)) {
            String referer = request.getHeader("Referer");
            if (ex instanceof BindException) {
                List<String> errors = processBindingErrors((BindException) ex);
                redirectAttributes.addFlashAttribute("errorMessages", errors);
                return handleRedirect(redirectAttributes, referer, "Errores de validación:", request);
            }

            return handleRedirect(redirectAttributes, referer, messageError, request);
        }

        // Caso no tenga dirección, se devuelve una pantalla genérica
        return createErrorModelAndView(errorInfo.status(), errorInfo.message(), request);
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

            case BindException ignored -> new ErrorInfo(HttpStatus.BAD_REQUEST, "Errores de validación en los datos enviados.");
            case MaxUploadSizeExceededException ignored ->
                    new ErrorInfo(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido.");
            case MultipartException ignored ->
                    new ErrorInfo(HttpStatus.BAD_REQUEST, "Solicitud multipart inválida.");
            case MethodArgumentTypeMismatchException ignored -> new ErrorInfo(HttpStatus.BAD_REQUEST, "Parámetro de solicitud inválido.");
            case MissingServletRequestParameterException ignored -> new ErrorInfo(HttpStatus.BAD_REQUEST, "Falta un parámetro obligatorio.");
            case HttpMediaTypeNotSupportedException ignored -> new ErrorInfo(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado.");
            case TransactionSystemException ignored ->
                    new ErrorInfo(HttpStatus.BAD_REQUEST, "Error en la transacción. Verifique los datos.");
            case OptimisticLockingFailureException ignored ->
                    new ErrorInfo(HttpStatus.CONFLICT, "El recurso fue modificado por otro usuario. Recarga e inténtalo de nuevo.");
            case OptimisticLockException ignored ->
                    new ErrorInfo(HttpStatus.CONFLICT, "El recurso fue modificado por otro usuario. Recarga e inténtalo de nuevo.");

            case HttpRequestMethodNotSupportedException methodEx ->
                    new ErrorInfo(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido: " + methodEx.getMethod());

            case NoHandlerFoundException ignored -> new ErrorInfo(HttpStatus.NOT_FOUND, "Recurso no encontrado");

            case NoResourceFoundException ignored -> new ErrorInfo(HttpStatus.NOT_FOUND, "Recurso no encontrado");

            case null, default -> new ErrorInfo(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
        };
    }

    private List<String> processBindingErrors(BindException be) {
        return be.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.isBindingFailure()
                        ? "Valor inválido para el campo " + fe.getField()
                        : fe.getDefaultMessage())
                .toList();
    }

    private boolean shouldLogException(ErrorInfo errorInfo) {
        return errorInfo.status().is5xxServerError();
    }

    boolean shouldLogException(Exception ex) {
        return shouldLogException(getErrorInfo(ex));
    }

    /**
     * Determina si debería devolver un redirect en base al tipo de error y la cabecera
     */
    private boolean determineIfShouldRedirect(Exception ex, HttpServletRequest request) {
        boolean haveReferer = request.getHeader("Referer") != null;
        return switch (ex) {
            case ResponseStatusException ignore -> haveReferer;
            case TransactionSystemException ignore -> haveReferer;
            case OptimisticLockingFailureException ignore -> haveReferer;
            case OptimisticLockException ignore -> haveReferer;
            case BindException ignore -> haveReferer;
            default -> false;
        };
    }

    /**
     * Se redirige añadiendo el error en el redirect
     */
    private ModelAndView handleRedirect(RedirectAttributes redirectAttributes,
                                        String referer, String message, HttpServletRequest requestForRedirect) {

        redirectAttributes.addFlashAttribute("errorMessage", message);
        return new ModelAndView("redirect:" + safeRefererPath(referer, requestForRedirect));
    }

    private String safeRefererPath(String referer, HttpServletRequest request) {
        if (referer == null || referer.indexOf('\r') >= 0 || referer.indexOf('\n') >= 0 || referer.indexOf('\\') >= 0) return "/";
        try {
            URI uri = URI.create(referer);
            if (uri.getRawUserInfo() != null) return "/";
            String path = uri.getRawPath();
            if (uri.getHost() == null) {
                if (uri.isAbsolute() || uri.getRawAuthority() != null) return "/";
            } else {
                String scheme = uri.getScheme();
                String expectedScheme = request.getScheme();
                if (scheme == null
                        || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                        || (expectedScheme != null && !scheme.equalsIgnoreCase(expectedScheme))
                        || !uri.getHost().equalsIgnoreCase(request.getServerName())) return "/";
                int port = uri.getPort() >= 0 ? uri.getPort() : ("https".equalsIgnoreCase(scheme) ? 443 : 80);
                int requestPort = request.getServerPort();
                if (requestPort < 0) requestPort = request.isSecure() ? 443 : 80;
                if (port != requestPort) return "/";
            }
            if (path == null || path.isEmpty()) path = "/";
            if (!path.startsWith("/") || path.startsWith("//")) return "/";
            return path + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }

    /**
     * Se devuelve una pantalla genérica con el error
     */
    private ModelAndView createErrorModelAndView(HttpStatusCode status, String message, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("common/error");
        mav.setStatus(status);
        mav.addObject("errorCode", status.value());
        mav.addObject("errorMessageLabel", message);
        mav.addObject("errorId", request.getAttribute("errorId"));
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
        String userAgent = sanitize(request.getHeader("User-Agent"));
        String requestUri = sanitize(request.getRequestURI());
        String method = sanitize(request.getMethod());
        String sessionId = request.getSession(false) != null ?
                hashSessionId(request.getSession(false).getId()) : "none";

        return new AuditInfo(username, remoteAddress, userAgent, requestUri, method, sessionId);
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace("\r", "").replace("\n", "");
    }

    private String hashSessionId(String sessionId) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 8);
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private String getCurrentUsername() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("anonymous");
    }

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /**
     * Construye el mensaje de error de manera detallada con la información del usuario
     */
    private void logException(String errorId, Exception ex, AuditInfo auditInfo) {
        String baseMsg = String.format("Excepción [%s] | Usuario: %s | IP: %s | Agente: %s | URI: %s | Método: %s | Sesión: %s |Tipo: %s",
                errorId,
                auditInfo.username(),
                auditInfo.remoteAddress(),
                auditInfo.userAgent(),
                auditInfo.requestUri(),
                auditInfo.method(),
                auditInfo.sessionId(),
                ex.getClass().getSimpleName());

        log.error(baseMsg, ex);
    }
}
