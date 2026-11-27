package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.user.AdminUserUpdateDTO;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * {@code @PreAuthorize("hasRole('ADMIN')")} on the admin user mutations holds even when a caller gets past the
 * URL rules, and the resulting denial is a clean 403. Every test is rolled back.
 */
@SpringBootTest (properties = "spring.datasource.url=jdbc:h2:mem:usermethodsecuritydb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@Import (UserServiceMethodSecurityTest.MethodSecurityProbe.class)
@Transactional
class UserServiceMethodSecurityTest {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private MockMvc mockMvc;

    private User target;

    @BeforeEach
    void createTarget() {
        User user = new User();
        user.setUsername("method_target");
        user.setEmail("method.target@example.com");
        user.setPassword("unused");
        user.setProfile(profileRepository.findByType("USER"));
        target = userRepository.save(user);
    }

    private AdminUserUpdateDTO promotion() {
        AdminUserUpdateDTO form = new AdminUserUpdateDTO();
        form.setUsername(target.getUsername());
        form.setEmail(target.getEmail());
        form.setType("ADMIN");
        return form;
    }

    private void assertTargetUntouched() {
        User stored = userRepository.findByUsername("method_target").orElseThrow();
        assertThat(stored.getProfile().getType()).isEqualTo("USER");
        assertThat(stored.getDeletedAt()).isNull();
    }

    @Test
    @WithMockUser (roles = "USER")
    void regularUsersCannotCallAdminUserMutations() {
        assertThatThrownBy(() -> userService.updateUserByUsername(promotion(), target.getUsername()))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> userService.deleteUserByUsername(target.getUsername()))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> userService.deleteUserById(target.getId()))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertTargetUntouched();
    }

    @Test
    void unauthenticatedCallersCannotCallAdminUserMutations() {
        assertThatThrownBy(() -> userService.deleteUserByUsername(target.getUsername()))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        assertTargetUntouched();
    }

    @Test
    @WithMockUser (roles = "ADMIN")
    void adminsCanCallAdminUserMutations() {
        userService.updateUserByUsername(promotion(), target.getUsername());
        assertThat(userRepository.findByUsername("method_target").orElseThrow().getProfile().getType()).isEqualTo("ADMIN");

        userService.deleteUserById(target.getId());
        assertThat(target.getDeletedAt()).isNotNull();
        assertThat(userRepository.findByUsername("method_target")).isEmpty();
    }

    @Test
    void methodSecurityDenialReachedThroughMvcIsACleanForbidden() throws Exception {
        mockMvc.perform(post("/user/method-security-probe")
                        .with(user("probe_user").roles("USER")).with(csrf())
                        .param("username", target.getUsername()))
                .andExpect(status().isForbidden())
                .andExpect(view().name("common/error"))
                .andExpect(model().attribute("errorCode", 403));

        assertTargetUntouched();
    }

    /** A deliberately unsafe endpoint: only {@code @PreAuthorize} stands between a USER and the admin method. */
    @Controller
    static class MethodSecurityProbe {
        private final UserService userService;

        MethodSecurityProbe(UserService userService) {
            this.userService = userService;
        }

        @PostMapping ("/user/method-security-probe")
        String deleteAnyUser(@RequestParam String username) {
            userService.deleteUserByUsername(username);
            return "redirect:/";
        }
    }
}
