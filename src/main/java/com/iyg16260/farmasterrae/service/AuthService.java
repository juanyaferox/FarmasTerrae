package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.auth.PasswordRecoveryDTO;
import com.iyg16260.farmasterrae.dto.auth.RegisterFormDTO;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    JavaMailSender javaMailSender;

    @Autowired
    BCryptPasswordEncoder encoder;

    /**
     * Registra el usuario a partir del formulario en la BBDD
     *
     * @param registerForm DTO que representa el formulario de registro
     * @throws ResponseStatusException BAD_REQUEST caso haya campos coincidentes
     */
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

    /**
     * Cambia la contraseña por una contraseña autogenerada y manda un correo
     *
     * @param passwordRequest DTO que contiene el email
     * @return mismo DTO con success/error
     */
    public PasswordRecoveryDTO setPassword(PasswordRecoveryDTO passwordRequest) {
        try {
            if (!recoverPassword(passwordRequest.getEmail())) {
                passwordRequest.setMessage("No existe el email en los registros del sistema.");
                passwordRequest.setMessageType("error");
            } else {
                passwordRequest.setMessage("Correo enviado correctamente. Revisa tu bandeja de entrada.");
                passwordRequest.setMessageType("success");
            }
        } catch (Exception e) {
            log.error("Error al mandar el mensaje a la dirección de correo: {}", passwordRequest.getEmail(), e);
            passwordRequest.setMessage("Hubo un error al enviar el correo. Inténtalo de nuevo.");
            passwordRequest.setMessageType("error");
        }
        return passwordRequest;
    }

    /**
     * Cambia la contraseña por la recibida através del DTO
     *
     * @param passwordRequest DTO con la contraseña
     * @param user            usuario a cambiar
     * @return mismo DTO con success/error
     */
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

    /**
     * Verifica que la contraseña sea correcta, si está en blanco y si la confirmación es correcta
     *
     * @param passwordRequest DTO con la contraseña y su confirmación
     * @return null si es correcta la validacion, error si no
     */
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

    @Value ("${spring.mail.username}")
    String fromAddress;

    /**
     * @param email correo a recuperar
     * @return false caso el correo no este asignado a un usuario, true si se realizó con éxito
     * @throws ResponseStatusException excepcion en caso de que el correo no pudiera ser enviado
     */
    private boolean recoverPassword(String email) throws ResponseStatusException {
        var optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty())
            return false;

        User user = optUser.get();
        String newPassword = generateRandomCode();

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("Recuperación de Contraseña - FarmasTerrae");

            String content = String.format(
                    "Estimado %s,%n%n" +
                            "Su contraseña ha sido restablecida. Su nueva contraseña es:%n%n%s%n%n" +
                            "Por favor, cámbiela después de iniciar sesión.",
                    user.getUsername(), newPassword
            );
            helper.setText(content);

            javaMailSender.send(message);

        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error enviando correo de recuperación");
        }

        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

}
