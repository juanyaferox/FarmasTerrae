package com.iyg16260.farmasterrae.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
@Order (1)
public class GlobalExceptionHandler {

    /**
     * Captura ResponseStatusException (400/404/500, etc.).
     * Si existe Referer: añade flashAttribute y redirige allí.
     * Si no existe Referer: muestra common/error con código y mensaje.
     */
    @ExceptionHandler (Exception.class)
    public ModelAndView handleAllExceptions(Exception ex,
                                            RedirectAttributes redirectAttributes,
                                            HttpServletRequest request) {
        log.error("Excepción capturada", ex);

        // Determinamos código y mensaje
        HttpStatusCode status;
        String message;
        boolean isFlashRedirect = false;

        if (ex instanceof ResponseStatusException rse) {
            status = rse.getStatusCode();
            message = rse.getReason();
            isFlashRedirect = request.getHeader("Referer") != null;

        } else if (ex instanceof TransactionSystemException) {
            status = HttpStatus.BAD_REQUEST;
            message = "Error en la transacción. Verifique los datos.";
            isFlashRedirect = request.getHeader("Referer") != null;

        } else if (ex instanceof HttpRequestMethodNotSupportedException methodEx) {
            status = HttpStatus.METHOD_NOT_ALLOWED;
            message = "Método no permitido: " + methodEx.getMethod();

        } else if (ex instanceof NoHandlerFoundException) {
            status = HttpStatus.NOT_FOUND;
            message = "Recurso no encontrado";

        } else if (ex instanceof AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
            message = "No tienes permisos para acceder a este recurso";

        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error interno del servidor";
        }

        String referer = request.getHeader("Referer");
        if (isFlashRedirect && referer != null) {
            redirectAttributes.addFlashAttribute("errorMessage", message);
            return new ModelAndView("redirect:" + referer);
        }

        ModelAndView mav = new ModelAndView("common/error");
        mav.setStatus(status);
        mav.addObject("errorCode", status.value());
        mav.addObject("errorMessageLabel", message);
        return mav;
    }
}

