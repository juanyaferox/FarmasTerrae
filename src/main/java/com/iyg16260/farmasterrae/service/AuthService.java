package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.auth.PasswordRecoveryDTO;
import com.iyg16260.farmasterrae.dto.auth.RegisterFormDTO;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static com.iyg16260.farmasterrae.utils.EmailUtils.sendPasswordRecoveryEmail;
import static com.iyg16260.farmasterrae.utils.GeneratorUtils.generateRandomCode;
import static com.iyg16260.farmasterrae.utils.PasswordUtils.isValidPassword;

@Service
@Slf4j
public class AuthService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    BCryptPasswordEncoder encoder;


    public void setRegister(RegisterFormDTO registerForm) throws ResponseStatusException {

        if (userRepository.findByUsername(registerForm.getUsername()).isPresent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario ya registrado");

        if (userRepository.findByEmail(registerForm.getEmail()).isPresent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email ya registrado");

        User user = new User();
        user.setUsername(registerForm.getUsername());
        user.setEmail(registerForm.getEmail());
        user.setFullName(registerForm.getFullName());
        user.setPassword(passwordEncoder.encode(registerForm.getPassword()));
        user.setPhone(registerForm.getPhone());
        user.setProfile(profileRepository.findByType("USER"));
        userRepository.save(user);
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

    public PasswordRecoveryDTO setPasswordAuthenticated(PasswordRecoveryDTO passwordRequest, User user) {

        var validationResult = validatePassword(passwordRequest);
        if (validationResult != null)
            return validationResult;

        user.setPassword(
                encoder.encode(passwordRequest.getNewPassword())
        );
        userRepository.save(user);
        passwordRequest.setMessage("Contraseña cambiada correctamente.");
        passwordRequest.setMessageType("success");
        return passwordRequest;
    }

    private PasswordRecoveryDTO validatePassword(PasswordRecoveryDTO passwordRequest) {
        String newPassword = passwordRequest.getNewPassword();

        if (newPassword.isBlank()) {
            passwordRequest.setMessage("La contraseña no puede estar vacía.");
            passwordRequest.setMessageType("error");
            return passwordRequest;
        }

        if (!newPassword.equals(passwordRequest.getConfirmPassword())) {
            passwordRequest.setMessage("No coinciden las contraseñas");
            passwordRequest.setMessageType("error");
            return passwordRequest;
        }

        if (!isValidPassword(newPassword)) {
            passwordRequest.setMessage("La contraseña debe contener al menos 1 mayúscula, 1 número y 6 digitos. " +
                    "No puede superar los 20 carácteres de longitud.");
            passwordRequest.setMessageType("error");
            return passwordRequest;
        }

        return null;
    }

    /**
     * @param email correo a recuperar
     * @return false caso el correo no este asignado a un usuario, true si se realizó con éxito
     * @throws MessagingException excepcion en caso de que el correo no pudiera ser enviado
     */
    private boolean recoverPassword(String email) throws MessagingException {
        var optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty())
            return false;

        User user = optUser.get();
        String newPassword = generateRandomCode();

        sendPasswordRecoveryEmail(email, newPassword);

        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

}
