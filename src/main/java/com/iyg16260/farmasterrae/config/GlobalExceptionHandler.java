package com.iyg16260.farmasterrae.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler (Exception.class) // Captura cualquier excepción
    public ModelAndView handleException(Exception ex, Model model) {
        ModelAndView modelAndView = new ModelAndView("common/error");

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        // Si es un 404, puedes configurar un HTML específico
        if (ex instanceof NoHandlerFoundException) {
            status = HttpStatus.NOT_FOUND; // error 404
        } else if (ex instanceof AccessDeniedException) {
            status = HttpStatus.FORBIDDEN; // error 403
        }

        // Pasar el código de estado y el mensaje al modelo
        modelAndView.addObject("errorCode", status.value());
        modelAndView.addObject("errorMessage", status.getReasonPhrase());

        return modelAndView;
    }
}
