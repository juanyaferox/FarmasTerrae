package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.auth.PasswordRecoveryDTO;
import com.iyg16260.farmasterrae.dto.auth.RegisterFormDTO;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.utils.EncryptionUtils;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.lang.module.ResolutionException;

import static com.iyg16260.farmasterrae.utils.EmailUtils.*;
import static com.iyg16260.farmasterrae.utils.GeneratorUtils.*;

@Service
@Slf4j
public class AuthService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;


    public void setRegister(RegisterFormDTO registerForm) throws ResponseStatusException {
        if (userRepository.findByUsername(registerForm.getUsername()).isPresent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already registered");

        if (userRepository.findByEmail(registerForm.getEmail()).isPresent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");

        User user = new User();
        user.setUsername(registerForm.getUsername());
        user.setEmail(registerForm.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(registerForm.getPassword()));
        user.setPhone(registerForm.getPhone());
    }

    public PasswordRecoveryDTO setPassword(PasswordRecoveryDTO passwordRequest) {
        try {
            if (!recoverPassword(passwordRequest.getEmail())) {
                passwordRequest.setMessage("No existe el email en los registros del sistema.");
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
        return passwordRequest;
    }

    /**
     *
     * @param email correo a recuperar
     * @throws MessagingException excepcion en caso de que el correo no pudiera ser enviado
     * @return false caso el correo no este asignado a un usuario, true si se realizó con éxito
     */
    private boolean recoverPassword(String email) throws MessagingException {
        var optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty())
            return false;

        User user = optUser.get();
        String newPassword = generateRandomCode();

        sendPasswordRecoveryEmail(email, newPassword);

        user.setPassword(EncryptionUtils.hashPassword(newPassword));
        userRepository.save(user);
        return true;
    }

}
