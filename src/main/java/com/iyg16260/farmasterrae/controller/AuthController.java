package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.auth.*;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.AuthService;
import com.iyg16260.farmasterrae.utils.SuccessMessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @GetMapping("/forgotPassword")
    public ModelAndView getPasswordRecovery() { return new ModelAndView("auth/forgot-password").addObject("forgotPasswordRequest", new ForgotPasswordRequestDTO()); }

    @PostMapping("/forgotPassword")
    public ModelAndView setPasswordRecovery(@Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequestDTO request, BindingResult result) {
        if (result.hasErrors()) return new ModelAndView("auth/forgot-password");
        return new ModelAndView("auth/forgot-password").addObject("forgotPasswordRequest", authService.setPassword(request));
    }

    @GetMapping("/resetPassword")
    public ModelAndView getResetPassword(@RequestParam(required = false) String token, HttpServletResponse response) {
        response.setHeader("Referrer-Policy", "no-referrer");
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setToken(token);
        if (token != null && !token.isBlank() && !authService.isResetTokenValid(token)) {
            request.setMessage("El enlace no es válido o ha caducado.");
            request.setMessageType("error");
        }
        return new ModelAndView("auth/reset-password").addObject("resetPasswordRequest", request);
    }

    @PostMapping("/resetPassword")
    public ModelAndView resetPassword(@Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequestDTO request, BindingResult result) {
        if (result.hasErrors()) { request.setMessage("Comprueba el enlace y los requisitos de la contraseña."); request.setMessageType("error"); }
        else authService.resetPassword(request);
        request.setNewPassword(null); request.setConfirmPassword(null);
        return new ModelAndView("auth/reset-password").addObject("resetPasswordRequest", request);
    }

    @GetMapping("/register")
    public ModelAndView getRegisterForm() { return new ModelAndView("auth/register").addObject("registerForm", new RegisterFormDTO()); }

    @PostMapping("/register")
    public ModelAndView setRegisterForm(@Valid @ModelAttribute("registerForm") RegisterFormDTO dto, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) return registerError(dto, result);
        try { authService.setRegister(dto); }
        catch (org.springframework.web.server.ResponseStatusException e) {
            result.reject("registration.conflict", e.getReason());
            ModelAndView view = registerError(dto, result);
            view.setStatus(e.getStatusCode());
            return view;
        }
        SuccessMessageUtils.buildSuccessMessage(ra, com.iyg16260.farmasterrae.enums.EntityType.USERS, com.iyg16260.farmasterrae.enums.Operation.POST);
        return new ModelAndView("redirect:/auth");
    }

    private ModelAndView registerError(RegisterFormDTO dto, BindingResult result) {
        dto.setPassword(null); dto.setConfirmPassword(null);
        ModelAndView view = new ModelAndView("auth/register").addObject("registerForm", dto);
        if (result.hasGlobalErrors()) view.addObject("error", result.getGlobalError().getDefaultMessage());
        else view.addObject("error", "Revisa los campos indicados.");
        return view;
    }

    @GetMapping("/changePassword")
    public ModelAndView getChangePasswordForm() { return new ModelAndView("auth/new-password").addObject("changePasswordRequest", new ChangePasswordRequestDTO()); }

    @PostMapping("/changePassword")
    public ModelAndView setChangePasswordForm(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequestDTO request,
                                              BindingResult result, @AuthenticationPrincipal User user,
                                              HttpServletRequest httpRequest) {
        if (result.hasErrors()) { request.setMessage("Comprueba los campos y los requisitos de la contraseña."); request.setMessageType("error"); }
        else authService.setPasswordAuthenticated(request, user, httpRequest.getSession(false) == null ? null : httpRequest.getSession(false).getId());
        request.setCurrentPassword(null); request.setNewPassword(null); request.setConfirmPassword(null);
        return new ModelAndView("auth/new-password").addObject("changePasswordRequest", request);
    }
}
