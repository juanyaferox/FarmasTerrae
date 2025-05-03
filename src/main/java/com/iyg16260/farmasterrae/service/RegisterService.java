package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.utils.EncryptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegisterService {

    @Autowired
    UserRepository ur;

    public void registerUser(User user) {
        encryptPassword(user);
        ur.save(user);
    }

    /**
     * @param user usuario a encriptar contraseña
     */
    private void encryptPassword(User user) {
        String password = user.getPassword();
        password = EncryptionUtils.hashPassword(password);
        user.setPassword(password);
    }

    /**
     * @param user usuario a validar username
     * @return false si el nombre de usuario ya existe en la BB.DD.
     */
    private boolean isValidUsername(User user) {
        String username = user.getUsername();
        boolean isValid = true;
        for (User value : ur.findAll()) {
            if (value.getUsername().equals(username)) {
                isValid = false;
                break;
            }
        }
        return isValid;
    }


}
