package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.utils.EncryptionUtils;
import com.iyg16260.farmasterrae.validation.ObjectValidator;
import com.iyg16260.farmasterrae.validation.PasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegisterService {

    @Autowired
    UserRepository ur;

    public void registerUser(User user) {
        for (boolean value : isValidUser(user)) {
           if (!value)
               throw new IllegalArgumentException("Usuario inválido");
        }
        encryptPassword(user);
        ur.save(user);
    }

    /**
     * @param user usuario con campos a validar
     * @return array con todas las validaciones
     */
    public boolean[] isValidUser(User user){
        final int NUMBER_VALIDATIONS = 3;
        boolean[] array = new boolean[NUMBER_VALIDATIONS];

        array[0] = ObjectValidator.isValid(user);
        array[1] = PasswordValidator.isValidPassword(user.getPassword());
        array[2] = isValidUsername(user);

        return array;
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
