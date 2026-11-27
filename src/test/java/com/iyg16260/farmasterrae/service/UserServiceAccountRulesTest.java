package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.user.AdminUserUpdateDTO;
import com.iyg16260.farmasterrae.dto.user.SelfProfileUpdateDTO;
import com.iyg16260.farmasterrae.mapper.UserMapperImpl;
import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.PasswordResetTokenRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Account rules of {@link UserService}, with the real mapper and mocked persistence. */
class UserServiceAccountRulesTest {

    private static final String REGISTRATION_PATTERN = "^[a-zA-Z0-9_]{3,20}$";

    private UserRepository users;
    private ProfileRepository profiles;
    private PasswordResetTokenRepository resetTokens;
    private SessionRevocationService sessions;
    private UserService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        profiles = mock(ProfileRepository.class);
        resetTokens = mock(PasswordResetTokenRepository.class);
        sessions = mock(SessionRevocationService.class);
        service = new UserService();
        service.userRepository = users;
        service.profileRepository = profiles;
        service.passwordResetTokenRepository = resetTokens;
        service.sessionRevocationService = sessions;
        service.userMapper = new UserMapperImpl();
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profiles.findByType("ADMIN")).thenReturn(profile(1L, "ADMIN"));
        when(profiles.findByType("USER")).thenReturn(profile(2L, "USER"));
    }

    @Test
    void lastAdminCheckReliesOnTheLockedReadTakenBeforeLoadingTheTarget() {
        User admin = user(1L, "admin_a", "ADMIN");
        // The other admin was deleted by a transaction that committed while this one waited for the lock
        when(users.findAllByProfileTypeForUpdate("ADMIN")).thenReturn(List.of(admin));
        when(users.findByUsernameForUpdate("admin_a")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.deleteUserByUsername("admin_a"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        InOrder order = inOrder(users);
        order.verify(users).findAllByProfileTypeForUpdate("ADMIN");
        order.verify(users).findByUsernameForUpdate("admin_a");
        verify(users, never()).findAllByProfileType(anyString());
        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(resetTokens, sessions);
        assertThat(admin.getDeletedAt()).isNull();
    }

    @Test
    void deletingAnAdminWhileAnotherRemainsScrubsTheAccountAndRevokesItsAccess() {
        User admin = user(1L, "admin_a", "ADMIN");
        when(users.findAllByProfileTypeForUpdate("ADMIN")).thenReturn(List.of(admin, user(2L, "admin_b", "ADMIN")));
        when(users.findByUsernameForUpdate("admin_a")).thenReturn(Optional.of(admin));

        service.deleteUserByUsername(" Admin_A ");

        InOrder order = inOrder(users, resetTokens, sessions);
        order.verify(users).findAllByProfileTypeForUpdate("ADMIN");
        order.verify(users).findByUsernameForUpdate("admin_a");
        order.verify(resetTokens).deleteAllByUserId(1L);
        order.verify(users).saveAndFlush(admin);
        order.verify(sessions).revoke(admin, "admin_a");
        assertThat(admin.getDeletedAt()).isNotNull();
        assertThat(admin.getUsername()).startsWith("#del-");
    }

    @Test
    void lastAdminCannotBeDemoted() {
        User admin = user(1L, "admin_a", "ADMIN");
        when(users.findAllByProfileTypeForUpdate("ADMIN")).thenReturn(List.of(admin));
        when(users.findByUsernameForUpdate("admin_a")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.updateUserByUsername(adminForm("admin_a", "USER"), "admin_a"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).isEqualTo("Tiene que existir al menos un administrador");
                });

        assertThat(admin.getProfile().getType()).isEqualTo("ADMIN");
        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(sessions);
    }

    @Test
    void lastAdminCanStillEditTheirOwnData() {
        User admin = user(1L, "admin_a", "ADMIN");
        when(users.findAllByProfileTypeForUpdate("ADMIN")).thenReturn(List.of(admin));
        when(users.findByUsernameForUpdate("admin_a")).thenReturn(Optional.of(admin));
        AdminUserUpdateDTO form = adminForm("admin_a", "ADMIN");
        form.setPhone("985123456");

        service.updateUserByUsername(form, "admin_a");

        assertThat(admin.getProfile().getType()).isEqualTo("ADMIN");
        assertThat(admin.getPhone()).isEqualTo("985123456");
        verifyNoInteractions(sessions);
    }

    @Test
    void unknownProfileNeverLeavesTheAccountWithoutAProfile() {
        User member = user(5L, "member", "USER");
        when(users.findByUsernameForUpdate("member")).thenReturn(Optional.of(member));
        when(profiles.findByType("SUPERADMIN")).thenReturn(null);

        assertThatThrownBy(() -> service.updateUserByUsername(adminForm("member", " superadmin "), "member"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).isEqualTo("Tipo de usuario inválido");
                });

        verify(profiles).findByType("SUPERADMIN");
        assertThat(member.getProfile()).isNotNull();
        assertThat(member.getProfile().getType()).isEqualTo("USER");
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void roleChangeRevokesEverySessionOfTheAccount() {
        User member = user(5L, "member", "USER");
        when(users.findByUsernameForUpdate("member")).thenReturn(Optional.of(member));

        service.updateUserByUsername(adminForm("member", "ADMIN"), "member");

        assertThat(member.getProfile().getType()).isEqualTo("ADMIN");
        verify(sessions).revoke(member, "member");
    }

    @Test
    void uniqueConstraintRaceOnUpdateIsAConflict() {
        User member = user(5L, "member", "USER");
        when(users.findByIdForUpdate(5L)).thenReturn(Optional.of(member));
        when(users.saveAndFlush(member)).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.updateOwnProfile(5L, selfForm("member", "taken@example.com"), "current"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verifyNoInteractions(sessions);
    }

    @Test
    void ownRenameRevokesTheOtherSessionsAndTheOldNameButKeepsTheCurrentSession() {
        User member = user(5L, "member", "USER");
        when(users.findByIdForUpdate(5L)).thenReturn(Optional.of(member));

        User updated = service.updateOwnProfile(5L, selfForm("Member_Two", "member@example.com"), "current-session");

        assertThat(updated.getUsername()).isEqualTo("member_two");
        assertThat(updated.getProfile().getType()).isEqualTo("USER");
        verify(sessions).revokeOthers(member, "member", "current-session");
        verify(sessions, never()).revoke(any(), any());
    }

    @Test
    void ownEmailChangeAlsoRevokesTheOtherSessions() {
        User member = user(5L, "member", "USER");
        when(users.findByIdForUpdate(5L)).thenReturn(Optional.of(member));

        service.updateOwnProfile(5L, selfForm("member", "New.Address@Example.com"), "current-session");

        assertThat(member.getEmail()).isEqualTo("new.address@example.com");
        verify(sessions).revokeOthers(member, "member", "current-session");
    }

    @Test
    void ownContactChangeWithoutNewUsernameOrEmailKeepsEverySession() {
        User member = user(5L, "member", "USER");
        when(users.findByIdForUpdate(5L)).thenReturn(Optional.of(member));
        SelfProfileUpdateDTO form = selfForm("member", "member@example.com");
        form.setPhone("600123456");

        service.updateOwnProfile(5L, form, "current-session");

        assertThat(member.getPhone()).isEqualTo("600123456");
        verifyNoInteractions(sessions);
    }

    @Test
    void ownProfileUpdateOfAMissingOrDeletedAccountIsNotFound() {
        when(users.findByIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateOwnProfile(5L, selfForm("member", "member@example.com"), "current"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void anonymizeLeavesNothingThatIdentifiesOrAuthenticatesTheAccount() {
        User first = user(5L, "member", "USER");
        first.setFullName("Ana Pérez");
        first.setPhone("+34 600 123 456");
        first.setAddress("Calle Mayor 1");
        first.setPassword(new BCryptPasswordEncoder().encode("CurrentPassword9"));
        User second = user(6L, "other", "USER");

        service.anonymize(first);
        service.anonymize(second);

        assertThat(first.getUsername()).startsWith("#del-").hasSizeLessThanOrEqualTo(255).doesNotMatch(REGISTRATION_PATTERN);
        assertThat(first.getEmail()).startsWith("#del-").endsWith("@deleted.invalid").hasSizeLessThanOrEqualTo(255);
        assertThat(first.getFullName()).isNull();
        assertThat(first.getPhone()).isNull();
        assertThat(first.getAddress()).isNull();
        assertThat(first.getPassword()).startsWith("!deleted!");
        assertThat(new BCryptPasswordEncoder().matches("CurrentPassword9", first.getPassword())).isFalse();
        assertThat(first.getDeletedAt()).isNotNull();
        assertThat(first.isEnabled()).isFalse();
        // Tombstones are random, so they never collide with each other or with an id-based guess
        assertThat(second.getUsername()).isNotEqualTo(first.getUsername()).isNotEqualTo("deleted_6");
        assertThat(second.getEmail()).isNotEqualTo(first.getEmail());
        assertThat(second.getPassword()).isNotEqualTo(first.getPassword());
    }

    private static AdminUserUpdateDTO adminForm(String username, String type) {
        AdminUserUpdateDTO form = new AdminUserUpdateDTO();
        form.setUsername(username);
        form.setEmail(username + "@example.com");
        form.setType(type);
        return form;
    }

    private static SelfProfileUpdateDTO selfForm(String username, String email) {
        SelfProfileUpdateDTO form = new SelfProfileUpdateDTO();
        form.setUsername(username);
        form.setEmail(email);
        return form;
    }

    private static User user(Long id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setProfile(profile("ADMIN".equals(role) ? 1L : 2L, role));
        return user;
    }

    private static Profile profile(Long id, String type) {
        Profile profile = new Profile();
        profile.setId(id);
        profile.setType(type);
        return profile;
    }
}
