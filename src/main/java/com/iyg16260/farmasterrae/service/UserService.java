package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.user.AdminUserUpdateDTO;
import com.iyg16260.farmasterrae.dto.user.SelfProfileUpdateDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.mapper.UserMapper;
import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.PasswordResetTokenRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.spec.UserSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    static final String ADMIN_PROFILE = "ADMIN";

    /**
     * Prefijo del nombre de usuario y del email de una cuenta eliminada. {@code #} y {@code -} no caben en el
     * patrón de registro ({@code ^[a-zA-Z0-9_]{3,20}$}) y el resto es un UUID aleatorio, así que nadie puede
     * registrar de antemano el valor que recibirá una cuenta al eliminarse y bloquear su borrado.
     */
    static final String DELETED_PREFIX = "#del-";

    /** Dominio reservado (RFC 2606): un email anonimizado nunca puede recibir correo. */
    static final String DELETED_EMAIL_DOMAIN = "@deleted.invalid";

    /** Sustituye al hash de la contraseña al eliminar una cuenta: no es BCrypt, así que ninguna contraseña coincide. */
    static final String DELETED_PASSWORD_PREFIX = "!deleted!";

    private static final String LAST_ADMIN_MESSAGE = "Tiene que existir al menos un administrador";

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

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
     * Obtiene los datos que el propio usuario puede editar, para el formulario de su panel
     *
     * @param userId id del usuario autenticado
     * @return datos editables del usuario
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el usuario
     */
    public SelfProfileUpdateDTO getOwnProfile(long userId) throws ResponseStatusException {
        return userMapper.userToSelfProfileUpdateDTO(
                userRepository.findById(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"))
        );
    }

    /**
     * Actualiza los datos del propio usuario. Nunca cambia su rol, su contraseña ni su estado de borrado, y un
     * campo opcional {@code null} conserva el valor guardado. Si cambia el nombre de usuario o el email, cierra
     * el resto de sesiones y borra los remember-me del nombre anterior y del nuevo, sin cerrar la sesión actual.
     *
     * @param userId           id del usuario autenticado (nunca un dato de la petición)
     * @param profileDetails   nuevos datos, ya validados
     * @param currentSessionId sesión que hace el cambio y que se mantiene abierta; {@code null} las cierra todas
     * @return usuario actualizado
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el usuario, CONFLICT si el username o el email
     *                                 ya existen, BAD_REQUEST si el nombre de usuario no es válido
     */
    @Transactional
    public User updateOwnProfile(long userId, SelfProfileUpdateDTO profileDetails, String currentSessionId)
            throws ResponseStatusException {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        String previousUsername = user.getUsername();
        String previousEmail = user.getEmail();

        profileDetails.setUsername(normalizeIdentifier(profileDetails.getUsername(), "El usuario no puede estar vacío"));
        profileDetails.setEmail(normalizeIdentifier(profileDetails.getEmail(), "El correo no puede estar vacío"));
        ensureIdentifiersAreFree(user, profileDetails.getUsername(), profileDetails.getEmail());
        userMapper.updateUserFromSelfProfileDTO(profileDetails, user);

        User updated = saveOrConflict(user);
        if (!Objects.equals(previousUsername, updated.getUsername())
                || !Objects.equals(previousEmail, updated.getEmail())) {
            sessionRevocationService.revokeOthers(updated, previousUsername, currentSessionId);
        }
        return updated;
    }

    /**
     * Actualiza la información de un usuario por su nombre de usuario (panel de administración)
     *
     * @param userDetails DTO con los datos actualizados del usuario
     * @param oldUsername nombre de usuario actual
     * @return usuario actualizado
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el usuario, BAD_REQUEST si el tipo no existe o
     *                                 se degrada al último administrador, CONFLICT si el username o el email ya existen
     */
    @PreAuthorize ("hasRole('ADMIN')")
    @Transactional
    public User updateUserByUsername(AdminUserUpdateDTO userDetails, String oldUsername) throws ResponseStatusException {
        // Siempre se bloquean primero los administradores y después la cuenta: el mismo orden en todas las
        // operaciones que pueden quitar un administrador evita interbloqueos entre ellas
        List<User> lockedAdmins = userRepository.findAllByProfileTypeForUpdate(ADMIN_PROFILE);
        User userDB = findByUsernameForUpdate(oldUsername);
        String previousRole = userDB.getProfile().getType();
        String previousUsername = userDB.getUsername();
        String previousEmail = userDB.getEmail();

        Profile targetProfile = resolveProfile(userDetails.getType(), userDB.getProfile());
        if (isAdmin(userDB) && !ADMIN_PROFILE.equals(targetProfile.getType())) {
            ensureAnotherAdminRemains(userDB, lockedAdmins);
        }

        userDetails.setUsername(normalizeIdentifier(userDetails.getUsername(), "El usuario no puede estar vacío"));
        userDetails.setEmail(normalizeIdentifier(userDetails.getEmail(), "El correo no puede estar vacío"));
        ensureIdentifiersAreFree(userDB, userDetails.getUsername(), userDetails.getEmail());
        userMapper.updateUserFromAdminDTO(userDetails, userDB);
        userDB.setProfile(targetProfile);

        User updated = saveOrConflict(userDB);
        if (!Objects.equals(previousRole, updated.getProfile().getType())
                || !Objects.equals(previousUsername, updated.getUsername())
                || !Objects.equals(previousEmail, updated.getEmail())) {
            sessionRevocationService.revoke(updated, previousUsername);
        }
        return updated;
    }

    /**
     * Elimina un usuario por su id mediante anonimización
     *
     * @param id id del usuario a eliminar
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el usuario, BAD_REQUEST si es el último admin
     */
    @PreAuthorize ("hasRole('ADMIN')")
    @Transactional
    public void deleteUserById(long id) throws ResponseStatusException {
        List<User> lockedAdmins = userRepository.findAllByProfileTypeForUpdate(ADMIN_PROFILE);
        User user = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        softDelete(user, lockedAdmins);
    }

    /**
     * Elimina un usuario por su nombre de usuario mediante anonimización
     *
     * @param username nombre de usuario del usuario a eliminar
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el usuario, BAD_REQUEST si es el último admin
     */
    @PreAuthorize ("hasRole('ADMIN')")
    @Transactional
    public void deleteUserByUsername(String username) throws ResponseStatusException {
        List<User> lockedAdmins = userRepository.findAllByProfileTypeForUpdate(ADMIN_PROFILE);
        softDelete(findByUsernameForUpdate(username), lockedAdmins);
    }

    /**
     * Anonimiza y desactiva una cuenta ya bloqueada, y revoca todo lo que permitía usarla: sesiones,
     * remember-me y enlaces de restablecimiento de contraseña
     *
     * @param user         usuario a eliminar
     * @param lockedAdmins administradores activos, leídos con bloqueo en esta transacción
     * @throws ResponseStatusException BAD_REQUEST si es el último admin
     */
    private void softDelete(User user, List<User> lockedAdmins) throws ResponseStatusException {
        if (isAdmin(user)) {
            ensureAnotherAdminRemains(user, lockedAdmins);
        }
        String previousUsername = user.getUsername();
        passwordResetTokenRepository.deleteAllByUserId(user.getId());
        User deleted = saveOrConflict(anonymize(user));
        sessionRevocationService.revoke(deleted, previousUsername);
    }

    /**
     * Anonimiza los datos de un usuario para cumplir con políticas de eliminación: sustituye su nombre de
     * usuario y su email por valores que no se pueden registrar, borra sus datos personales e invalida
     * su contraseña
     *
     * @param u usuario a anonimizar
     * @return usuario con datos anonimizados
     */
    public User anonymize(User u) {
        u.setUsername(DELETED_PREFIX + UUID.randomUUID());
        u.setEmail(DELETED_PREFIX + UUID.randomUUID() + DELETED_EMAIL_DOMAIN);
        u.setFullName(null);
        u.setPhone(null);
        u.setAddress(null);
        u.setPassword(DELETED_PASSWORD_PREFIX + UUID.randomUUID());
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

    private User findByUsernameForUpdate(String username) throws ResponseStatusException {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByUsernameForUpdate(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    /**
     * Resuelve el perfil pedido. Sin tipo se conserva el actual; un tipo que no existe nunca deja la cuenta
     * sin perfil
     *
     * @throws ResponseStatusException BAD_REQUEST si el tipo no corresponde a ningún perfil
     */
    private Profile resolveProfile(String type, Profile currentProfile) throws ResponseStatusException {
        if (type == null) {
            return currentProfile;
        }
        String normalizedType = type.trim().toUpperCase(Locale.ROOT);
        Profile targetProfile = normalizedType.isEmpty() ? null : profileRepository.findByType(normalizedType);
        if (targetProfile == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de usuario inválido");
        }
        return targetProfile;
    }

    private static boolean isAdmin(User user) {
        return user.getProfile() != null && ADMIN_PROFILE.equals(user.getProfile().getType());
    }

    /**
     * Comprueba que quede otro administrador activo si {@code target} deja de serlo
     *
     * @param lockedAdmins administradores activos, leídos con bloqueo en esta transacción (no una lectura previa,
     *                     que dos borrados simultáneos podrían ver igual)
     * @throws ResponseStatusException BAD_REQUEST si no queda ningún otro
     */
    private static void ensureAnotherAdminRemains(User target, List<User> lockedAdmins) throws ResponseStatusException {
        boolean anotherAdmin = lockedAdmins.stream()
                .anyMatch(admin -> !Objects.equals(admin.getId(), target.getId()));
        if (!anotherAdmin) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, LAST_ADMIN_MESSAGE);
        }
    }

    /**
     * Comprueba que el nuevo username y el nuevo email no pertenezcan a otra cuenta
     *
     * @throws ResponseStatusException CONFLICT si el username o el email ya existen
     */
    private void ensureIdentifiersAreFree(User user, String username, String email) throws ResponseStatusException {
        if (username != null && !username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nombre de usuario ya existente en la BBDD");
        }
        if (email != null && !email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ya existente en la BBDD");
        }
    }

    /**
     * Guarda y sincroniza con la BBDD dentro de la transacción, para que una restricción única (p. ej. dos
     * peticiones simultáneas con el mismo email) acabe en CONFLICT y no en un error interno
     */
    private User saveOrConflict(User user) throws ResponseStatusException {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nombre de usuario o email ya existente en la BBDD");
        }
    }

    /**
     * Normaliza un username o email. {@code null} significa "no cambiar"
     *
     * @throws ResponseStatusException BAD_REQUEST si llega en blanco
     */
    private static String normalizeIdentifier(String value, String blankMessage) throws ResponseStatusException {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, blankMessage);
        }
        return normalized;
    }
}
