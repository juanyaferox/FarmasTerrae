package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.user.AdminUserUpdateDTO;
import com.iyg16260.farmasterrae.model.PasswordResetToken;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.PasswordResetTokenRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Admin user management: role changes are validated, the last ADMIN can neither be demoted nor deleted, and
 * deleting an account anonymizes it with tombstones nobody can pre-register. Every test is rolled back.
 */
@SpringBootTest (properties = "spring.datasource.url=jdbc:h2:mem:adminusermanagementdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@Transactional
@WithMockUser (roles = "ADMIN")
class AdminUserManagementTest {

    private static final String PASSWORD = "VictimPassword9";
    private static final String LAST_ADMIN = "Tiene que existir al menos un administrador";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private PersistentTokenRepository persistentTokenRepository;
    @Autowired private SessionRegistry sessionRegistry;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private UserService userService;

    private User newUser(String username, String profileType) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username.replace('_', '.') + "@example.com");
        user.setFullName("Ana Pérez");
        user.setPhone("+34 600 123 456");
        user.setAddress("Calle Mayor 1, Oviedo");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setProfile(profileRepository.findByType(profileType));
        return userRepository.save(user);
    }

    /** Creates an ADMIN and demotes every other one, so it is the last ADMIN for this (rolled back) test. */
    private User soleAdmin(String username) {
        User admin = newUser(username, "ADMIN");
        jdbcTemplate.update("update users set profileId = ? where profileId = ? and id <> ?",
                profileRepository.findByType("USER").getId(), profileRepository.findByType("ADMIN").getId(), admin.getId());
        return admin;
    }

    /** The stored row, bypassing both the persistence context and the soft-delete filter. */
    private Map<String, Object> row(long id) {
        entityManager.flush();
        return jdbcTemplate.queryForMap("select u.username, u.email, u.fullName, u.phone, u.address, u.password, "
                + "u.deletedAt, p.type from users u join profiles p on p.id = u.profileId where u.id = ?", id);
    }

    private static AdminUserUpdateDTO adminForm(User user, String type) {
        AdminUserUpdateDTO form = new AdminUserUpdateDTO();
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setType(type);
        return form;
    }

    @Test
    void lastAdminCannotBeDemoted() throws Exception {
        User admin = soleAdmin("last_admin");

        mockMvc.perform(put("/admin/dashboard/users").with(csrf())
                        .param("oldUsername", admin.getUsername())
                        .param("username", admin.getUsername())
                        .param("email", admin.getEmail())
                        .param("type", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute("errorMessageLabel", LAST_ADMIN));

        assertThat(row(admin.getId()).get("type")).isEqualTo("ADMIN");
    }

    @Test
    void lastAdminCannotBeDeleted() throws Exception {
        User admin = soleAdmin("last_admin");

        mockMvc.perform(delete("/admin/dashboard/users").with(csrf()).param("username", admin.getUsername()))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute("errorMessageLabel", LAST_ADMIN));

        Map<String, Object> stored = row(admin.getId());
        assertThat(stored.get("deletedAt")).isNull();
        assertThat(stored.get("username")).isEqualTo("last_admin");
    }

    @Test
    void anAdminCanBeDemotedWhileAnotherAdminRemainsAndLosesTheirSessions() throws Exception {
        User keeper = soleAdmin("keeper_admin");
        User demoted = newUser("demoted_admin", "ADMIN");
        String sessionId = "demoted-session-" + demoted.getId();
        sessionRegistry.registerNewSession(sessionId, demoted);

        mockMvc.perform(put("/admin/dashboard/users").with(csrf())
                        .param("oldUsername", demoted.getUsername())
                        .param("username", demoted.getUsername())
                        .param("email", demoted.getEmail())
                        .param("type", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Usuario actualizado con éxito."));

        assertThat(row(demoted.getId()).get("type")).isEqualTo("USER");
        assertThat(row(keeper.getId()).get("type")).isEqualTo("ADMIN");
        assertThat(sessionRegistry.getSessionInformation(sessionId).isExpired()).isTrue();
    }

    @Test
    void unknownProfileTypeIsABadRequestAndKeepsTheRole() throws Exception {
        User target = newUser("plain_user", "USER");

        mockMvc.perform(put("/admin/dashboard/users").with(csrf())
                        .param("oldUsername", target.getUsername())
                        .param("username", target.getUsername())
                        .param("email", target.getEmail())
                        .param("type", "SUPERADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("common/error"));

        assertThat(row(target.getId()).get("type")).isEqualTo("USER");
    }

    @Test
    void serviceRefusesProfilesThatDoNotExistInsteadOfClearingTheRole() {
        User target = newUser("plain_user", "USER");

        assertThatThrownBy(() -> userService.updateUserByUsername(adminForm(target, "SUPERADMIN"), target.getUsername()))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).isEqualTo("Tipo de usuario inválido");
                });

        assertThat(row(target.getId()).get("type")).isEqualTo("USER");
    }

    @Test
    void adminEditKeepsOmittedOptionalFields() throws Exception {
        User target = newUser("kept_fields", "USER");
        String updatedEmail = "new." + target.getEmail();

        mockMvc.perform(put("/admin/dashboard/users").with(csrf())
                        .param("oldUsername", target.getUsername())
                        .param("username", target.getUsername())
                        .param("email", updatedEmail)
                        .param("type", "USER"))
                .andExpect(status().is3xxRedirection());

        Map<String, Object> stored = row(target.getId());
        assertThat(stored.get("email")).isEqualTo(updatedEmail);
        assertThat(stored.get("fullName")).isEqualTo("Ana Pérez");
        assertThat(stored.get("phone")).isEqualTo("+34 600 123 456");
        assertThat(stored.get("address")).isEqualTo("Calle Mayor 1, Oviedo");
    }

    @Test
    void preRegisteredDeletedPrefixCannotBlockDeletionAndDeletionScrubsTheAccount() throws Exception {
        User victim = newUser("victim", "USER");
        long id = victim.getId();
        // Anyone can register the username the old anonymization ("deleted_<id>") would have given this account
        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("username", "deleted_" + id)
                        .param("email", "squatter" + id + "@example.com")
                        .param("password", "Squatter123456")
                        .param("confirmPassword", "Squatter123456"))
                .andExpect(status().is3xxRedirection());
        assertThat(userRepository.existsByUsername("deleted_" + id)).isTrue();
        // Everything that could still let someone use the account
        PasswordResetToken pending = resetToken(victim, null);
        PasswordResetToken used = resetToken(victim, Instant.now());
        String series = "victim-series-" + id;
        persistentTokenRepository.createNewToken(new PersistentRememberMeToken(victim.getUsername(), series, "token-value", new Date()));
        String sessionId = "victim-session-" + id;
        sessionRegistry.registerNewSession(sessionId, victim);

        mockMvc.perform(delete("/admin/dashboard/users").with(csrf()).param("username", victim.getUsername()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Usuario eliminado con éxito."));

        Map<String, Object> stored = row(id);
        String tombstone = (String) stored.get("username");
        assertThat(tombstone).startsWith("#del-").doesNotMatch("^[a-zA-Z0-9_]{3,20}$");
        assertThat((String) stored.get("email")).startsWith("#del-").endsWith("@deleted.invalid");
        assertThat(stored.get("fullName")).isNull();
        assertThat(stored.get("phone")).isNull();
        assertThat(stored.get("address")).isNull();
        assertThat(stored.get("deletedAt")).isNotNull();
        String storedPassword = (String) stored.get("password");
        assertThat(storedPassword).isNotBlank().doesNotStartWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, storedPassword)).isFalse();
        assertThat(jdbcTemplate.queryForObject("select count(*) from passwordResetTokens where id in (?, ?)",
                Integer.class, pending.getId(), used.getId())).isZero();
        assertThat(persistentTokenRepository.getTokenForSeries(series)).isNull();
        assertThat(sessionRegistry.getSessionInformation(sessionId).isExpired()).isTrue();
        assertThat(userRepository.findByUsername(victim.getUsername())).isEmpty();
        assertThat(userRepository.findByUsername("deleted_" + id)).isPresent();

        // Nor can anyone take the tombstone itself
        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("username", tombstone)
                        .param("email", "late" + id + "@example.com")
                        .param("password", "Squatter123456")
                        .param("confirmPassword", "Squatter123456"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registerForm", "username"));
    }

    private PasswordResetToken resetToken(User user, Instant usedAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash((UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", ""));
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(1800));
        token.setUsedAt(usedAt);
        return passwordResetTokenRepository.save(token);
    }

    @Test
    void unknownAdminDashboardSectionIsNotFound() throws Exception {
        mockMvc.perform(get("/admin/dashboard/bogus"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("common/error"))
                .andExpect(model().attribute("errorCode", 404));

        mockMvc.perform(get("/admin/dashboard/bogus").header("Referer", "/admin/dashboard/users"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("common/error"))
                .andExpect(model().attribute("errorCode", 404));
    }

    @ParameterizedTest
    @ValueSource (strings = {"/admin/dashboard", "/admin/dashboard/products", "/admin/dashboard/orders", "/admin/dashboard/users"})
    void knownAdminDashboardSectionsStillRender(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }
}
