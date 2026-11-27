package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.mapper.UserMapper;
import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.spec.UserSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    UserMapper userMapper;

    @Autowired
    SessionRevocationService sessionRevocationService;

    private final int PAGE_SIZE_ADMIN = 9;

    /**
     * Obtiene todos los perfiles disponibles en el sistema
     *
     * @return lista de perfiles
     */
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String normalized = usernameOrEmail == null ? "" : usernameOrEmail.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByUsername(normalized)
                .or(() -> userRepository.findByEmail(normalized))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    /**
     * Obtiene un usuario por su id
     *
     * @param id id del usuario a buscar
     * @return usuario DTO encontrado
     * @throws ResponseStatusException CONFLICT si no se encuentra el usuario
     */
    public UserDTO getUserById(long id) throws ResponseStatusException {
        return userMapper.userToUserDTO(
                userRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Usuario no encontrado"))
        );
    }

    /**
     * Obtiene un usuario por su nombre de usuario
     *
     * @param username nombre de usuario a buscar
     * @return usuario DTO encontrado
     * @throws ResponseStatusException CONFLICT si no se encuentra el usuario
     */
    public UserDTO getUserByUsername(String username) throws ResponseStatusException {
        return userMapper.userToUserDTO(
                userRepository.findByUsername(username)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Usuario no encontrado"))
        );
    }

    /**
     * Obtiene una página de todos los usuarios del sistema
     *
     * @param page número de página
     * @return página de usuarios DTO
     */
    public Page<UserDTO> getAllUsers(int page, String keyword, String sort, String dir) {
        Sort sortOrder = Sort.unsorted();
        if (sort != null) {
            Sort.Direction direction = "desc".equals(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sortOrder = Sort.by(direction, sort);
        }

        Specification<User> spec = new UserSpecification()
                .searchLike(keyword);

        return userRepository.findAll(spec, PageRequest.of(page, PAGE_SIZE_ADMIN, sortOrder))
                .map(user -> userMapper.userToUserDTO(user));
    }

    /**
     * Actualiza la información de un usuario por su id
     *
     * @param id          id del usuario a actualizar
     * @param userDetails DTO con los datos actualizados del usuario
     * @return usuario actualizado
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el usuario
     */
    public User updateUserById(long id, UserDTO userDetails) throws ResponseStatusException {
        User userDB = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        String oldRole = userDB.getProfile().getType();
        String oldUsername = userDB.getUsername();
        if (userDetails.getType() == null)
            userDetails.setType(userDB.getProfile().getType());
        User updated = userRepository.save(validateUserDTOAndReturnUserModified(userDB, userDetails));
        if (!Objects.equals(oldRole, updated.getProfile().getType())
                || !Objects.equals(oldUsername, updated.getUsername())) {
            sessionRevocationService.revoke(updated, oldUsername);
        }
        return updated;
    }

    /**
     * Actualiza la información de un usuario por su nombre de usuario
     *
     * @param userDetails DTO con los datos actualizados del usuario
     * @param oldUsername nombre de usuario actual
     * @return usuario actualizado
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el usuario
     */
    public User updateUserByUsername(UserDTO userDetails, String oldUsername) throws ResponseStatusException {
        User userDB = userRepository.findByUsername(oldUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        String oldRole = userDB.getProfile().getType();
        String actualOldUsername = userDB.getUsername();
        User updated = userRepository.save(validateUserDTOAndReturnUserModified(userDB, userDetails));
        if (!Objects.equals(oldRole, updated.getProfile().getType())
                || !Objects.equals(actualOldUsername, updated.getUsername())) {
            sessionRevocationService.revoke(updated, actualOldUsername);
        }
        return updated;
    }

    /**
     * Valida los datos del DTO y actualiza el usuario correspondiente
     *
     * @param user        usuario existente a modificar
     * @param userDetails DTO con los nuevos datos
     * @return usuario modificado y validado
     * @throws ResponseStatusException CONFLICT si el username o email ya existen
     */
    private User validateUserDTOAndReturnUserModified(User user, UserDTO userDetails) throws ResponseStatusException {

        if (userDetails.getUsername() != null) userDetails.setUsername(userDetails.getUsername().trim().toLowerCase(Locale.ROOT));
        if (userDetails.getEmail() != null) userDetails.setEmail(userDetails.getEmail().trim().toLowerCase(Locale.ROOT));

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

    /**
     * Elimina un usuario por su id mediante anonimización
     *
     * @param id id del usuario a eliminar
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el usuario
     */
    public void deleteUserById(long id) throws ResponseStatusException {
        var userOpt = userRepository.findById(id);
        User user = userOpt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        String oldUsername = user.getUsername();
        User deleted = validateUserForDelete(userOpt);
        sessionRevocationService.revoke(deleted, oldUsername);
        userRepository.save(deleted);
    }

    /**
     * Elimina un usuario por su nombre de usuario mediante anonimización
     *
     * @param username nombre de usuario del usuario a eliminar
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el usuario
     */
    public void deleteUserByUsername(String username) throws ResponseStatusException {
        var userOpt = userRepository.findByUsername(username);
        User user = userOpt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        String oldUsername = user.getUsername();
        User deleted = validateUserForDelete(userOpt);
        sessionRevocationService.revoke(deleted, oldUsername);
        userRepository.save(deleted);
    }

    /**
     * Valida si un usuario puede ser eliminado y lo prepara para anonimización
     *
     * @param userOpt usuario opcional a validar
     * @return usuario anonimizado
     * @throws ResponseStatusException BAD_REQUEST si es el último admin, NOT_FOUND si no existe
     */
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

    /**
     * Anonimiza los datos de un usuario para cumplir con políticas de eliminación
     *
     * @param u usuario a anonimizar
     * @return usuario con datos anonimizados
     */
    public User anonymize(User u) {
        u.setUsername("deleted_" + u.getId());
        u.setEmail("deleted_" + u.getId() + "@example.com");
        u.setPhone(null);
        u.setAddress(null);
        u.setDeletedAt(LocalDateTime.now());
        return u;
    }

    /**
     * Obtiene un usuario por id inicializando sus relaciones lazy
     *
     * @param userId id del usuario
     * @return usuario con relaciones inicializadas
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el usuario
     */
    @Transactional
    public User getUser(long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        // Forzamos la inicialización de la colección mientras la sesión está abierta
        user.getOrderList().size();
        return user;
    }

    /**
     * Obtiene todos los perfiles disponibles en el sistema
     *
     * @return lista de perfiles
     */
    public List<Profile> getProfiles() {
        return profileRepository.findAll();
    }

}


