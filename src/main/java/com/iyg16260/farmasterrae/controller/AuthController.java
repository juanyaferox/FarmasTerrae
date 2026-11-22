package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.auth.PasswordRecoveryDTO;
import com.iyg16260.farmasterrae.dto.auth.RegisterFormDTO;
import com.iyg16260.farmasterrae.dto.response.AuthenticatedUserDTO;
import com.iyg16260.farmasterrae.dto.response.CsrfTokenDTO;
import com.iyg16260.farmasterrae.dto.response.ErrorResponseDTO;
import com.iyg16260.farmasterrae.dto.response.MessageResponseDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.AuthService;
import com.iyg16260.farmasterrae.utils.SuccessMessageUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import static com.iyg16260.farmasterrae.config.OpenApiConfig.*;

/**
 * El login (POST /api/auth/login) y el logout (POST /api/auth/logout) los gestiona Spring Security,
 * ver {@link com.iyg16260.farmasterrae.config.SecurityConfig}. Su documentación OpenAPI se añade en
 * {@link com.iyg16260.farmasterrae.config.OpenApiConfig}
 */
@Slf4j
@Tag (name = TAG_AUTH)
@RestController
@RequestMapping ("/api/auth")
public class AuthController {

    private static final String MESSAGE_TYPE_ERROR = "error";

    @Autowired
    AuthService authService;

    /**
     * Token CSRF necesario para las peticiones POST, PUT y DELETE
     */
    @io.swagger.v3.oas.annotations.Operation (summary = "Obtener el token CSRF",
            description = "Token que hay que enviar en la cabecera X-XSRF-TOKEN en las peticiones " +
                    "POST, PUT y DELETE. También se envía en la cookie XSRF-TOKEN")
    @ApiResponse (responseCode = "200", description = "Token CSRF")
    @GetMapping ("/csrf")
    public CsrfTokenDTO getCsrfToken(@Parameter (hidden = true) CsrfToken csrfToken) {
        return new CsrfTokenDTO(csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken());
    }

    /**
     * Usuario autenticado actualmente
     */
    @io.swagger.v3.oas.annotations.Operation (summary = "Usuario autenticado",
            description = "Nombre, nombre completo y roles del usuario con la sesión iniciada")
    @SecurityRequirement (name = SESSION_COOKIE)
    @ApiResponse (responseCode = "200", description = "Usuario autenticado")
    @ApiResponse (responseCode = "401", ref = UNAUTHORIZED)
    @GetMapping ("/me")
    public AuthenticatedUserDTO getAuthenticatedUser(@Parameter (hidden = true) Authentication authentication) {
        return AuthenticatedUserDTO.from(authentication);
    }

    @io.swagger.v3.oas.annotations.Operation (summary = "Recuperar la contraseña",
            description = "Genera una contraseña nueva y la envía al email indicado. Solo se usa el campo email")
    @ApiResponse (responseCode = "200", description = "Correo enviado (messageType = success)")
    @ApiResponse (responseCode = "400", description = "El email no existe o no se pudo enviar el correo (messageType = error)",
            content = @Content (schema = @Schema (implementation = PasswordRecoveryDTO.class)))
    @ApiResponse (responseCode = "403", ref = FORBIDDEN)
    @PostMapping ("/forgot-password")
    public ResponseEntity<PasswordRecoveryDTO> setPasswordRecovery(@RequestBody PasswordRecoveryDTO passwordRequest) {
        return buildPasswordResponse(authService.setPassword(passwordRequest));
    }

    @io.swagger.v3.oas.annotations.Operation (summary = "Registrar un usuario",
            description = "Crea un usuario con el perfil USER")
    @ApiResponse (responseCode = "201", description = "Usuario registrado")
    @ApiResponse (responseCode = "400", description = "Errores de validación o usuario/email ya registrado",
            content = @Content (schema = @Schema (implementation = ErrorResponseDTO.class)))
    @ApiResponse (responseCode = "403", ref = FORBIDDEN)
    @PostMapping ("/register")
    @ResponseStatus (HttpStatus.CREATED)
    public MessageResponseDTO setRegisterForm(@Valid @RequestBody RegisterFormDTO registerFormDTO) {
        authService.setRegister(registerFormDTO);
        return SuccessMessageUtils.buildSuccessMessage(EntityType.USERS, Operation.POST);
    }

    @io.swagger.v3.oas.annotations.Operation (summary = "Cambiar la contraseña",
            description = "Cambia la contraseña del usuario autenticado. Solo se usan los campos " +
                    "newPassword y confirmPassword")
    @SecurityRequirement (name = SESSION_COOKIE)
    @ApiResponse (responseCode = "200", description = "Contraseña cambiada (messageType = success)")
    @ApiResponse (responseCode = "400", description = "Contraseña vacía, no coincide o no cumple el patrón (messageType = error)",
            content = @Content (schema = @Schema (implementation = PasswordRecoveryDTO.class)))
    @ApiResponse (responseCode = "401", ref = UNAUTHORIZED)
    @ApiResponse (responseCode = "403", ref = FORBIDDEN)
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
