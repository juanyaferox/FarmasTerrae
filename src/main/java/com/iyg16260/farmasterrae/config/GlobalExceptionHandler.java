package com.iyg16260.farmasterrae.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@Order (1)
public class GlobalExceptionHandler {

    /**
     * Captura ResponseStatusException (400/404/500, etc.).
     * Si existe Referer: añade flashAttribute y redirige allí.
     * Si no existe Referer: muestra common/error con código y mensaje.
     */
    @ExceptionHandler ({ResponseStatusException.class, TransactionSystemException.class})
    public ModelAndView handleResponseStatus(ResponseStatusException ex,
                                             RedirectAttributes redirectAttributes,
                                             HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            // 1) si venimos de otra página, usamos flash + redirect
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
            return new ModelAndView("redirect:" + referer);
        } else {
            // 2) si no hay referer, mostramos la vista de error genérico
            HttpStatusCode status = ex.getStatusCode();
            ModelAndView mav = new ModelAndView("common/error");
            mav.setStatus(status);
            mav.addObject("errorCode", status.value());
            mav.addObject("errorMessage", ex.getReason());
            return mav;
        }
    }

    /**
     * Captura cualquier otra excepción no contemplada explícitamente.
     * Siempre muestra common/error con su código y mensaje.
     */
    @ExceptionHandler (Exception.class)
    public ModelAndView handleException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex instanceof NoHandlerFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
        }

        ModelAndView mav = new ModelAndView("common/error");
        mav.setStatus(status);
        mav.addObject("errorCode", status.value());
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }
}

