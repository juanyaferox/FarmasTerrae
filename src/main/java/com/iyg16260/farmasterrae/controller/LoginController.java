package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.auth.LoginDTO;
import com.iyg16260.farmasterrae.dto.auth.LoginResponseDTO;
import com.iyg16260.farmasterrae.dto.auth.PasswordRecoveryDTO;
import com.iyg16260.farmasterrae.service.LoginService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequestMapping ("/login")
public class LoginController {

    @Autowired
    LoginService ls;

    @GetMapping("/forgotPassword")
    public ModelAndView getPassword(){
        return new ModelAndView("auth/forgotPassword");
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<PasswordRecoveryDTO> setPassword(@RequestBody PasswordRecoveryDTO passwordRequest) {

        try {
            String email = passwordRequest.getEmail();
            if (!ls.recoverPassword(email)) {
                passwordRequest.setMessage("No existe el email en la base de datos.");
                passwordRequest.setMessageType("error");
            } else {
                passwordRequest.setMessage("Correo enviado correctamente. Revisa tu bandeja de entrada.");
                passwordRequest.setMessageType("success");
            }
        } catch (MessagingException e) {
            log.error("Error al mandar el mensaje a la dirección de correo: {}", passwordRequest.getEmail(), e);
            passwordRequest.setMessage("Hubo un error al enviar el correo. Inténtalo de nuevo.");
            passwordRequest.setMessageType("error");
        }
        return ResponseEntity.ok(passwordRequest);
    }

}
