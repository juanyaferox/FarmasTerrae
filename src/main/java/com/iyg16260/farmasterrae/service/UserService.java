package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.utils.EncryptionUtils;
import com.iyg16260.farmasterrae.validation.ObjectValidator;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    // Crear usuario
    public User createUser(User user) throws IllegalArgumentException {
        if (!ObjectValidator.isValid(user)) {
            throw new IllegalArgumentException("Usuario inválido");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public void changePassword(Long id, String uncryptedPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(uncryptedPassword));
        userRepository.save(user);
    }

    // Obtener usuario por ID
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    // Obtener todos los usuarios
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Actualizar usuario
    public User updateUser(Long id, UserDTO userDetails) throws RuntimeException {
        return userRepository.findById(id).map(user -> {
            if (userDetails.getUsername() != null) user.setUsername(userDetails.getUsername());
            if (userDetails.getEmail() != null) user.setEmail(userDetails.getEmail());
            if (userDetails.getPhone() != null) user.setPhone(userDetails.getPhone());
            if (userDetails.getAddress() != null) user.setAddress(userDetails.getAddress());
            System.out.println("Se ha llegado a guardar");
            System.out.println("User: "+user);
            System.out.println("UserDTO: "+userDetails);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // Eliminar usuario por ID
    public void deleteUser(Long id) throws RuntimeException {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        } else {
            throw new RuntimeException("Usuario no encontrado");
        }
    }

    @Transactional
    public User getUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        // Forzamos la inicialización de la colección mientras la sesión está abierta
        user.getOrderList().size();
        return user;
    }

}


