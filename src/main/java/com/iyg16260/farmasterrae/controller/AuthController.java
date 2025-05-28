package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.auth.PasswordRecoveryDTO;
import com.iyg16260.farmasterrae.dto.auth.RegisterFormDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.AuthService;
import com.iyg16260.farmasterrae.utils.SuccessMessageUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping ("/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @GetMapping ("/forgotPassword")
    public ModelAndView getPasswordRecovery() {
        return new ModelAndView("auth/forgot-password")
                .addObject("passwordRecovery", new PasswordRecoveryDTO());
    }

    @PostMapping ("/forgotPassword")
    public ModelAndView setPasswordRecovery(@ModelAttribute PasswordRecoveryDTO passwordRequest) {
        return new ModelAndView("auth/forgot-password")
                .addObject("passwordRecovery", authService.setPassword(passwordRequest));
    }

    @GetMapping ("/register")
    public ModelAndView getRegisterForm() {
        return new ModelAndView("auth/register")
                .addObject("registerForm", new RegisterFormDTO());
    }

    @PostMapping ("/register")
    public ModelAndView setRegisterForm(@Valid @ModelAttribute RegisterFormDTO registerFormDTO, RedirectAttributes ra) {
        authService.setRegister(registerFormDTO);
        SuccessMessageUtils.buildSuccessMessage(ra, EntityType.USERS, Operation.POST);
        return new ModelAndView("redirect:/auth");
    }

    @GetMapping ("/changePassword")
    public ModelAndView getChangePasswordForm() {
        return new ModelAndView("auth/new-password")
                .addObject("passwordRecovery", new PasswordRecoveryDTO());
    }

    @PostMapping ("/changePassword")
    public ModelAndView setChangePasswordForm(@ModelAttribute PasswordRecoveryDTO passwordRequest,
                                              @AuthenticationPrincipal User user) {
        return new ModelAndView("auth/new-password")
                .addObject("passwordRecovery", authService.setPasswordAuthenticated(passwordRequest, user));
    }
}
