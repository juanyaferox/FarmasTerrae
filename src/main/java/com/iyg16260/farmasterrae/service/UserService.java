package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.utils.EncryptionUtils;
import com.iyg16260.farmasterrae.validation.ObjectValidator;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository ur;

    // Crear usuario
    public User createUser(User user) {
        if (!ObjectValidator.isValid(user)) {
            throw new IllegalArgumentException("Usuario inválido");
        }
        return ur.save(user);
    }

    // Obtener usuario por ID
    public Optional<User> getUserById(Long id) {
        return ur.findById(id);
    }

    // Obtener todos los usuarios
    public List<User> getAllUsers() {
        return ur.findAll();
    }

    // Actualizar usuario
    public User updateUser(Long id, User userDetails) {
        return ur.findById(id).map(user -> {
            if (userDetails.getUsername() != null) user.setUsername(userDetails.getUsername());
            if (userDetails.getEmail() != null) user.setEmail(userDetails.getEmail());
            if (userDetails.getPassword() != null) user.setPassword(EncryptionUtils.hashPassword(userDetails.getPassword()));
            if (userDetails.getPhone() != null) user.setPhone(userDetails.getPhone());
            if (userDetails.getAddress() != null) user.setAddress(userDetails.getAddress());
            if (userDetails.getProfile() != null) user.setProfile(userDetails.getProfile());

            return ur.save(user);
        }).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // Eliminar usuario por ID
    public void deleteUser(Long id) {
        if (ur.existsById(id)) {
            ur.deleteById(id);
        } else {
            throw new RuntimeException("Usuario no encontrado");
        }
    }
}
