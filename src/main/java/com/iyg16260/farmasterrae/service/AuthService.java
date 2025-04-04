package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.utils.EncryptionUtils;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.iyg16260.farmasterrae.utils.EmailUtils.*;
import static com.iyg16260.farmasterrae.utils.GeneratorUtils.*;

@Service
public class AuthService {

    @Autowired
    UserRepository ur;

    /**
     *
     * @param email correo a recuperar
     * @throws MessagingException excepcion en caso de que el correo no pudiera ser enviado
     * @return false caso el correo no este asignado a un usuario, true si se realizó con éxito
     */
    public boolean recoverPassword(String email) throws MessagingException {
        var optUser = ur.findByEmail(email);
        if (optUser.isEmpty())
            return false;

        User user = optUser.get();
        String newPassword = generateRandomCode();

        sendPasswordRecoveryEmail(email, newPassword);

        user.setPassword(EncryptionUtils.hashPassword(newPassword));
        ur.save(user);
        return true;
    }

}
