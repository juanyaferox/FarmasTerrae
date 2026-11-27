package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.user.SelfProfileUpdateDTO;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.PersistentLoginRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Self-service profile form ({@code /user/dashboard/info-user}): a user can only change their own contact data,
 * never their role, credentials or deletion state, and omitted fields never wipe stored values.
 */
@SpringBootTest (properties = "spring.datasource.url=jdbc:h2:mem:selfprofileupdatedb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class UserSelfProfileUpdateTest {

    private static final String PASSWORD = "CurrentPassword9";
    private static final String FULL_NAME = "Ana Pérez";
    private static final String PHONE = "+34 600 123 456";
    private static final String ADDRESS = "Calle Mayor 1, Oviedo";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private PersistentLoginRepository persistentLoginRepository;
    @Autowired private SessionRegistry sessionRegistry;
    @Autowired private PasswordEncoder passwordEncoder;

    /** A stored USER with every optional field filled in; usernames and emails never contain each other. */
    private User newUser(String prefix) {
        int n = SEQUENCE.incrementAndGet();
        User user = new User();
        user.setUsername(prefix + "_" + n);
        user.setEmail("owner" + n + "@example.com");
        user.setFullName(FULL_NAME);
        user.setPhone(PHONE);
        user.setAddress(ADDRESS);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setProfile(profileRepository.findByType("USER"));
        return userRepository.save(user);
    }

    private User reload(User user) {
        return userRepository.findById(user.getId()).orElseThrow();
    }

    @Test
    void extraRoleAndAccountParametersCannotEscalatePrivileges() throws Exception {
        User account = newUser("escalate");
        Long adminProfileId = profileRepository.findByType("ADMIN").getId();

        mockMvc.perform(post("/user/dashboard/info-user").with(user(account)).with(csrf())
                        .param("username", account.getUsername())
                        .param("email", account.getEmail())
                        .param("type", "ADMIN")
                        .param("profile", String.valueOf(adminProfileId))
                        .param("profile.id", String.valueOf(adminProfileId))
                        .param("profile.type", "ADMIN")
                        .param("id", "1")
                        .param("password", "Hijacked123456")
                        .param("deletedAt", "2020-01-01T00:00:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dashboard/info-user"));

        User stored = reload(account);
        assertThat(stored.getProfile().getType()).isEqualTo("USER");
        assertThat(stored.getPassword()).isEqualTo(account.getPassword());
        assertThat(stored.getDeletedAt()).isNull();
    }

    @Test
    void successfulUpdateSaysUpdatedInsteadOfAdded() throws Exception {
        User account = newUser("toast");

        MvcResult result = mockMvc.perform(post("/user/dashboard/info-user").with(user(account)).with(csrf())
                        .param("username", account.getUsername())
                        .param("email", account.getEmail())
                        .param("fullName", "Ana María Pérez"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Usuario actualizado con éxito."))
                .andReturn();

        mockMvc.perform(get("/user/dashboard/info-user").with(user(account)).flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuario actualizado con éxito.")))
                .andExpect(content().string(not(containsString("añadido"))));
        assertThat(reload(account).getFullName()).isEqualTo("Ana María Pérez");
    }

    @Test
    void invalidEmailIsRejectedAndShownWithoutTouchingTheAccount() throws Exception {
        User account = newUser("bademail");

        mockMvc.perform(post("/user/dashboard/info-user").with(user(account)).with(csrf())
                        .param("username", account.getUsername())
                        .param("email", "not-an-email")
                        .param("phone", "999 999 999"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("user/dashboard"))
                .andExpect(model().attributeHasFieldErrors("userView", "email"))
                .andExpect(content().string(containsString("Debe ser un correo")))
                .andExpect(content().string(containsString("value=\"not-an-email\"")));

        User stored = reload(account);
        assertThat(stored.getEmail()).isEqualTo(account.getEmail());
        assertThat(stored.getPhone()).isEqualTo(PHONE);
    }

    @ParameterizedTest
    @ValueSource (strings = {"", "ab", "bad user", "has-dash", "#del-squatter", "this_username_is_too_long"})
    void invalidUsernameIsRejectedWithoutTouchingTheAccount(String username) throws Exception {
        User account = newUser("baduser");

        mockMvc.perform(post("/user/dashboard/info-user").with(user(account)).with(csrf())
                        .param("username", username)
                        .param("email", account.getEmail()))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("userView", "username"));

        assertThat(reload(account).getUsername()).isEqualTo(account.getUsername());
    }

    @Test
    void omittedOptionalFieldsKeepTheirStoredValues() throws Exception {
        User account = newUser("partial");

        mockMvc.perform(post("/user/dashboard/info-user").with(user(account)).with(csrf())
                        .param("username", account.getUsername())
                        .param("email", "changed." + account.getEmail()))
                .andExpect(status().is3xxRedirection());

        User stored = reload(account);
        assertThat(stored.getEmail()).isEqualTo("changed." + account.getEmail());
        assertThat(stored.getFullName()).isEqualTo(FULL_NAME);
        assertThat(stored.getPhone()).isEqualTo(PHONE);
        assertThat(stored.getAddress()).isEqualTo(ADDRESS);
    }

    @Test
    void optionalFieldsSubmittedEmptyAreCleared() throws Exception {
        User account = newUser("clear");

        mockMvc.perform(post("/user/dashboard/info-user").with(user(account)).with(csrf())
                        .param("username", account.getUsername())
                        .param("email", account.getEmail())
                        .param("fullName", "")
                        .param("phone", "   ")
                        .param("address", ""))
                .andExpect(status().is3xxRedirection());

        User stored = reload(account);
        assertThat(stored.getFullName()).isNull();
        assertThat(stored.getPhone()).isNull();
        assertThat(stored.getAddress()).isNull();
    }

    @Test
    void usernameAndEmailAreStoredNormalized() throws Exception {
        User account = newUser("normalize");
        String newUsername = "Mixed_Case_" + account.getId();

        mockMvc.perform(post("/user/dashboard/info-user").with(user(account)).with(csrf())
                        .param("username", newUsername)
                        .param("email", "Mixed." + account.getEmail().toUpperCase(Locale.ROOT))
                        .param("phone", "  600123456  "))
                .andExpect(status().is3xxRedirection());

        User stored = reload(account);
        assertThat(stored.getUsername()).isEqualTo(newUsername.toLowerCase(Locale.ROOT));
        assertThat(stored.getEmail()).isEqualTo("mixed." + account.getEmail());
        assertThat(stored.getPhone()).isEqualTo("600123456");
    }

    @Test
    void usernameOrEmailOfAnotherAccountIsAConflict() throws Exception {
        User account = newUser("dupa");
        User other = newUser("dupb");

        mockMvc.perform(post("/user/dashboard/info-user").with(user(account)).with(csrf())
                        .param("username", other.getUsername().toUpperCase(Locale.ROOT))
                        .param("email", account.getEmail()))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/user/dashboard/info-user").with(user(account)).with(csrf())
                        .param("username", account.getUsername())
                        .param("email", other.getEmail().toUpperCase(Locale.ROOT)))
                .andExpect(status().isConflict());

        User stored = reload(account);
        assertThat(stored.getUsername()).isEqualTo(account.getUsername());
        assertThat(stored.getEmail()).isEqualTo(account.getEmail());
    }

    @Test
    void renameKeepsTheCurrentSessionWithTheNewNameAndRevokesEverythingElse() throws Exception {
        User account = newUser("rename");
        String oldUsername = account.getUsername();
        String newUsername = "renamed_" + account.getId();

        MvcResult login = mockMvc.perform(post("/auth").with(csrf())
                        .param("username", oldUsername)
                        .param("password", PASSWORD)
                        .param("remember-me", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        Cookie rememberMe = login.getResponse().getCookie("remember-me");
        assertThat(session).isNotNull();
        assertThat(rememberMe).isNotNull();
        assertThat(persistentLoginRepository.findByUsername(oldUsername)).isPresent();
        // The same account is also signed in on another device
        String otherSessionId = "other-device-" + account.getId();
        sessionRegistry.registerNewSession(otherSessionId, reload(account));

        mockMvc.perform(post("/user/dashboard/info-user").session(session).with(csrf())
                        .param("username", newUsername)
                        .param("email", account.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dashboard/info-user"));

        // The session that made the change keeps working and already carries the new name
        mockMvc.perform(get("/user/dashboard/info-user").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(newUsername)))
                .andExpect(content().string(not(containsString(oldUsername))));
        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(((User) context.getAuthentication().getPrincipal()).getUsername()).isEqualTo(newUsername);
        assertThat(context.getAuthentication().getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(sessionRegistry.getSessionInformation(session.getId()).isExpired()).isFalse();

        // Every other session and every remember-me token issued for the old name are revoked
        assertThat(sessionRegistry.getSessionInformation(otherSessionId).isExpired()).isTrue();
        assertThat(persistentLoginRepository.findByUsername(oldUsername)).isEmpty();
        mockMvc.perform(get("/user/dashboard").cookie(rememberMe))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Location", org.hamcrest.Matchers.endsWith("/auth")));
    }

    @Test
    void infoUserFormIsBackedByTheSelfProfileFields() throws Exception {
        User account = newUser("view");

        mockMvc.perform(get("/user/dashboard/info-user").with(user(account)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("userView", instanceOf(SelfProfileUpdateDTO.class)))
                .andExpect(model().attribute("userView", hasProperty("phone", is(PHONE))))
                .andExpect(content().string(containsString("value=\"" + account.getUsername() + "\"")))
                .andExpect(content().string(containsString("value=\"" + ADDRESS + "\"")));
    }

    @Test
    @WithMockUser (roles = "USER")
    void unknownUserDashboardSectionIsNotFound() throws Exception {
        mockMvc.perform(get("/user/dashboard/bogus"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("common/error"))
                .andExpect(model().attribute("errorCode", 404));

        mockMvc.perform(get("/user/dashboard/bogus").header("Referer", "/user/dashboard/info-user"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("common/error"))
                .andExpect(model().attribute("errorCode", 404));
    }

    @ParameterizedTest
    @ValueSource (strings = {"/user/dashboard", "/user/dashboard/info-user", "/user/dashboard/orders", "/user/dashboard/reviews"})
    void knownUserDashboardSectionsStillRender(String path) throws Exception {
        User account = newUser("sections");

        mockMvc.perform(get(path).with(user(account)))
                .andExpect(status().isOk())
                .andExpect(view().name("user/dashboard"));
    }
}
