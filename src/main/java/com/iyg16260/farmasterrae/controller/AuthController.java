package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.auth.PasswordRecoveryDTO;
import com.iyg16260.farmasterrae.dto.auth.RegisterFormDTO;
import com.iyg16260.farmasterrae.service.AuthService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping ("/login")
public class AuthController {

    @Autowired
    AuthService authService;

    @GetMapping("/forgotPassword")
    public ModelAndView getPasswordRecovery(){
        return new ModelAndView("auth/forgot-password")
                .addObject("password-recovery", new PasswordRecoveryDTO());
    }

    @PostMapping("/forgotPassword")
    public ModelAndView setPasswordRecovery(@ModelAttribute PasswordRecoveryDTO passwordRequest) {
        return new ModelAndView("auth/forgot-password")
                .addObject("password-recovery", authService.setPassword(passwordRequest));
    }

    @GetMapping("/register")
    public ModelAndView getRegisterForm() {
        return new ModelAndView("auth/register")
                .addObject("registerForm", new RegisterFormDTO());
    }

    @PostMapping("/register")
    public ModelAndView setRegisterForm(@Valid @ModelAttribute RegisterFormDTO registerFormDTO,
                                        BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            return new ModelAndView("auth/register");
        try {
            authService.setRegister(registerFormDTO);
            return new ModelAndView("redirect:/login");
        } catch (ResponseStatusException e) {
            return new ModelAndView("auth/register")
                    .addObject("registerForm", registerFormDTO)
                    .addObject("error", e.getMessage());
        }
    }
}
