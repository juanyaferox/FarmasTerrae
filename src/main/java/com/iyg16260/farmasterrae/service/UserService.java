package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.mapper.user.UserMapper;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.utils.EncryptionUtils;
import com.iyg16260.farmasterrae.validation.ObjectValidator;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Autowired
    UserMapper userMapper;

    private final int PAGE_SIZE_ADMIN = 50;

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

    public void changePassword(long id, String uncryptedPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(uncryptedPassword));
        userRepository.save(user);
    }

    // Obtener usuario por ID
    public UserDTO getUserById(long id) {
        return userMapper.userToUserDTO(
                userRepository.findById(id).orElseThrow(RuntimeException::new)
        );
    }

    // Obtener todos los usuarios
    public Page<UserDTO> getAllUsers(int page) {
        return userRepository.findAll(Pageable.ofSize(PAGE_SIZE_ADMIN).withPage(page))
                .map(user -> userMapper.userToUserDTO(user));
    }

    // Actualizar usuario
    public User updateUser(long id, UserDTO userDetails) throws RuntimeException {
        return userRepository.findById(id).map(user -> {
            if (userDetails.getUsername() != null) user.setUsername(userDetails.getUsername());
            if (userDetails.getEmail() != null) user.setEmail(userDetails.getEmail());
            if (userDetails.getPhone() != null) user.setPhone(userDetails.getPhone());
            if (userDetails.getAddress() != null) user.setAddress(userDetails.getAddress());

            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // Eliminar usuario por ID
    public void deleteUser(long id) throws RuntimeException {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        } else {
            throw new RuntimeException("Usuario no encontrado");
        }
    }

    @Transactional
    public User getUser(long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        // Forzamos la inicialización de la colección mientras la sesión está abierta
        user.getOrderList().size();
        return user;
    }

}


