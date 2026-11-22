package com.iyg16260.farmasterrae.config;

import com.iyg16260.farmasterrae.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
@Order (1)
public class GlobalExceptionHandler implements Filter {

    /**
     * Captura las excepciones no controladas de la cadena de filtros. Si es BAD_REQUEST o BindException no genera archivo de log
     *
     * @param req   The request to process
     * @param res   The response associated with the request
     * @param chain Provides access to the next filter in the chain for this filter to pass the request and response
     *              to for further processing
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;

        try {
            chain.doFilter(req, res);
        } catch (Exception ex) {
            String errorId = UUID.randomUUID().toString();
            request.setAttribute("errorId", errorId);
            logException(errorId, ex, collectAuditInfo(request));
            throw ex;
        }
    }

    /**
     * Captura cualquier tipo de excepción y la devuelve como JSON con el código HTTP correspondiente.
     * Si la excepción se registra en el log se incluye su identificador (errorId) en la respuesta.
     */
    @ExceptionHandler (Exception.class)
    @ApiResponse (responseCode = "500", description = "Error interno del servidor",
            content = @Content (schema = @Schema (implementation = ErrorResponseDTO.class)))
    public ResponseEntity<ErrorResponseDTO> handleAllExceptions(Exception ex,
                                                                HttpServletRequest request) {

        // Construcción mensaje del error
        ErrorInfo errorInfo = getErrorInfo(ex);

        // Log con la información excepto si es un error de validación o un 400
        String errorId = null;
        if (shouldLogException(ex)) {
            errorId = Optional.ofNullable((String) request.getAttribute("errorId"))
                    .orElse(UUID.randomUUID().toString());
            request.setAttribute("errorId", errorId);
            logException(errorId, ex, collectAuditInfo(request));
        }

        // Errores de validación de los campos
        List<String> errors = ex instanceof BindException be ? processBindingErrors(be) : null;

        return ResponseEntity.status(errorInfo.status())
                .body(ErrorResponseDTO.of(errorInfo.status(), errorInfo.message(), errors, errorId));
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

            case BindException ignored -> new ErrorInfo(HttpStatus.BAD_REQUEST, "Errores de validación");

            case HttpMessageNotReadableException ignored ->
                    new ErrorInfo(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido");

            case MissingServletRequestParameterException paramEx ->
                    new ErrorInfo(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + paramEx.getParameterName());

            case MissingServletRequestPartException partEx ->
                    new ErrorInfo(HttpStatus.BAD_REQUEST, "Falta la parte obligatoria: " + partEx.getRequestPartName());

            case MethodArgumentTypeMismatchException typeEx ->
                    new ErrorInfo(HttpStatus.BAD_REQUEST, "Valor inválido para el parámetro: " + typeEx.getName());

            case HttpMediaTypeNotSupportedException mediaEx ->
                    new ErrorInfo(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado: " + mediaEx.getContentType());

            case MaxUploadSizeExceededException ignored ->
                    new ErrorInfo(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido");

            case TransactionSystemException ignored ->
                    new ErrorInfo(HttpStatus.BAD_GATEWAY, "Error en la transacción. Verifique los datos.");

            case HttpRequestMethodNotSupportedException methodEx ->
                    new ErrorInfo(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido: " + methodEx.getMethod());

            case NoHandlerFoundException ignored -> new ErrorInfo(HttpStatus.NOT_FOUND, "Recurso no encontrado");

            case NoResourceFoundException ignored -> new ErrorInfo(HttpStatus.NOT_FOUND, "Recurso no encontrado");

            case AccessDeniedException ignored ->
                    new ErrorInfo(HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este recurso");

            case null, default -> new ErrorInfo(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
        };
    }

    /**
     * Listado de errores de validación con el formato "campo: mensaje"
     */
    private List<String> processBindingErrors(BindException be) {
        return be.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
    }

    /**
     * Solo genera un log si no se trata de un error de validación un 400
     *
     * @param ex excepción
     * @return si se deberia logear o no
     */
    private boolean shouldLogException(Exception ex) {

        if (ex instanceof BindException) {
            return false;
        }

        // Errores en los datos de la petición enviados por el cliente
        if (ex instanceof HttpMessageNotReadableException
                || ex instanceof MissingServletRequestParameterException
                || ex instanceof MissingServletRequestPartException
                || ex instanceof MethodArgumentTypeMismatchException
                || ex instanceof HttpMediaTypeNotSupportedException
                || ex instanceof MaxUploadSizeExceededException) {
            return false;
        }

        if (ex instanceof ResponseStatusException rse) {
            return !HttpStatus.BAD_REQUEST.equals(rse.getStatusCode()) || !HttpStatus.FORBIDDEN.equals(rse.getStatusCode());
        }

        return true;
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
        String baseMsg = String.format("Excepción [%s] | Usuario: %s | IP: %s | Agente: %s | URI: %s | Método: %s | Sesión: %s |Tipo: %s",
                errorId,
                auditInfo.username(),
                auditInfo.remoteAddress(),
                auditInfo.userAgent(),
                auditInfo.requestUri(),
                auditInfo.method(),
                auditInfo.sessionId(),
                ex.getClass().getSimpleName());

        // En consola
        log.trace(baseMsg);

        // En el achivo
        Logger fileLogger = LoggerFactory.getLogger("errorLogger");
        fileLogger.error(baseMsg, ex);
    }
}