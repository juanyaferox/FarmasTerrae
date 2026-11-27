package com.iyg16260.farmasterrae.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorViewController {
    @RequestMapping("/common/error")
    public ModelAndView error(HttpServletRequest request, HttpServletResponse response) {
        Object codeValue = request.getAttribute("errorCode");
        int code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (codeValue instanceof Number number) code = number.intValue();
        else if (codeValue instanceof String value) {
            try { code = Integer.parseInt(value); } catch (NumberFormatException ignored) { /* use default status */ }
        }
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = status.value();
        }
        response.setStatus(status.value());
        ModelAndView model = new ModelAndView("common/error");
        model.setStatus(status);
        model.addObject("errorCode", code);
        model.addObject("errorMessageLabel", request.getAttribute("errorMessageLabel"));
        model.addObject("errorId", request.getAttribute("errorId"));
        return model;
    }
}
