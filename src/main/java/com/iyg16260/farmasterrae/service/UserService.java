package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.mapper.UserMapper;
import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserMapper userMapper;

    private final int PAGE_SIZE_ADMIN = 50;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws ResponseStatusException {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario o email no encontrado"));
    }


    /**
     * @param user
     * @return
     * @throws ResponseStatusException
     */
    public User createUser(User user) throws ResponseStatusException {

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public void changePassword(long id, String uncryptedPassword) throws ResponseStatusException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(uncryptedPassword));
        userRepository.save(user);
    }

    // Obtener usuario por ID
    public UserDTO getUserById(long id) throws ResponseStatusException {
        return userMapper.userToUserDTO(
                userRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Usuario no encontrado"))
        );
    }

    public UserDTO getUserByUsername(String username) throws ResponseStatusException {
        return userMapper.userToUserDTO(
                userRepository.findByUsername(username)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Usuario no encontrado"))
        );
    }

    // Obtener todos los usuarios
    public Page<UserDTO> getAllUsers(int page) {
        return userRepository.findAll(Pageable.ofSize(PAGE_SIZE_ADMIN).withPage(page))
                .map(user -> userMapper.userToUserDTO(user));
    }

    // Actualizar usuario
    public User updateUserById(long id, UserDTO userDetails) throws ResponseStatusException {
        User userDB = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return userRepository.save(
                validateUserDTOAndReturnUserModified(userDB, userDetails)
        );
    }

    public User updateUserByUsername(UserDTO userDetails, String oldUsername) throws ResponseStatusException {
        User userDB = userRepository.findByUsername(oldUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return userRepository.save(
                validateUserDTOAndReturnUserModified(userDB, userDetails)
        );
    }

    private User validateUserDTOAndReturnUserModified(User user, UserDTO userDetails) throws ResponseStatusException {

        if (!Objects.equals(user.getUsername(), userDetails.getUsername())
                && userRepository.existsByUsername(userDetails.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nombre de usuario ya existente en la BBDD");
        }
        if (!Objects.equals(user.getEmail(), userDetails.getEmail())
                && userRepository.existsByEmail(userDetails.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ya existente en la BBDD");
        }
        userMapper.updateUserFromUserDTO(userDetails, user);
        user.setProfile(profileRepository.findByType(userDetails.getType()));
        return user;
    }

    // Eliminar usuario por ID
    public void deleteUserById(long id) throws ResponseStatusException {
        var userOpt = userRepository.findById(id);
        userRepository.save(validateUserForDelete(userOpt));
    }

    public void deleteUserByUsername(String username) throws ResponseStatusException {
        var userOpt = userRepository.findByUsername(username);
        userRepository.save(validateUserForDelete(userOpt));
    }

    private User validateUserForDelete(Optional<User> userOpt) throws ResponseStatusException {
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String userProfileType = user.getProfile().getType();

            if (Objects.equals(userProfileType, "ADMIN")
                    && userRepository.findAllByProfileType(userProfileType).size() <= 1)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tiene que existir al menos un administrador");

            return anonymize(user);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
    }

    public User anonymize(User u) {
        u.setUsername("deleted_" + u.getId());
        u.setEmail("deleted_" + u.getId() + "@example.com");
        u.setPhone(null);
        u.setAddress(null);
        u.setDeletedAt(LocalDateTime.now());
        return u;
    }

    @Transactional
    public User getUser(long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        // Forzamos la inicialización de la colección mientras la sesión está abierta
        user.getOrderList().size();
        return user;
    }

    public List<Profile> getProfiles() {
        return profileRepository.findAll();
    }

}


