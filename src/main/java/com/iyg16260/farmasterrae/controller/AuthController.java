package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.auth.PasswordRecoveryDTO;
import com.iyg16260.farmasterrae.dto.auth.RegisterFormDTO;
import com.iyg16260.farmasterrae.dto.response.AuthenticatedUserDTO;
import com.iyg16260.farmasterrae.dto.response.CsrfTokenDTO;
import com.iyg16260.farmasterrae.dto.response.MessageResponseDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.AuthService;
import com.iyg16260.farmasterrae.utils.SuccessMessageUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

/**
 * El login (POST /api/auth/login) y el logout (POST /api/auth/logout) los gestiona Spring Security,
 * ver {@link com.iyg16260.farmasterrae.config.SecurityConfig}
 */
@Slf4j
@RestController
@RequestMapping ("/api/auth")
public class AuthController {

    private static final String MESSAGE_TYPE_ERROR = "error";

    @Autowired
    AuthService authService;

    /**
     * Token CSRF necesario para las peticiones POST, PUT y DELETE
     */
    @GetMapping ("/csrf")
    public CsrfTokenDTO getCsrfToken(CsrfToken csrfToken) {
        return new CsrfTokenDTO(csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken());
    }

    /**
     * Usuario autenticado actualmente
     */
    @GetMapping ("/me")
    public AuthenticatedUserDTO getAuthenticatedUser(Authentication authentication) {
        return AuthenticatedUserDTO.from(authentication);
    }

    @PostMapping ("/forgot-password")
    public ResponseEntity<PasswordRecoveryDTO> setPasswordRecovery(@RequestBody PasswordRecoveryDTO passwordRequest) {
        return buildPasswordResponse(authService.setPassword(passwordRequest));
    }

    @PostMapping ("/register")
    @ResponseStatus (HttpStatus.CREATED)
    public MessageResponseDTO setRegisterForm(@Valid @RequestBody RegisterFormDTO registerFormDTO) {
        authService.setRegister(registerFormDTO);
        return SuccessMessageUtils.buildSuccessMessage(EntityType.USERS, Operation.POST);
    }

    @PostMapping ("/change-password")
    public ResponseEntity<PasswordRecoveryDTO> setChangePasswordForm(@RequestBody PasswordRecoveryDTO passwordRequest,
                                                                     @AuthenticationPrincipal User user) {
        return buildPasswordResponse(authService.setPasswordAuthenticated(passwordRequest, user));
    }

    /**
     * Devuelve el resultado de la operación con 400 si el servicio lo marcó como error, 200 en otro caso
     */
    private ResponseEntity<PasswordRecoveryDTO> buildPasswordResponse(PasswordRecoveryDTO result) {
        HttpStatus status = MESSAGE_TYPE_ERROR.equals(result.getMessageType())
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(result);
    }
}
