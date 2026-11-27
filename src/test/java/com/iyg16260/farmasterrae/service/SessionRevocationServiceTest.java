package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.dto.user.AdminUserUpdateDTO;
import com.iyg16260.farmasterrae.mapper.UserMapper;
import com.iyg16260.farmasterrae.repository.PasswordResetTokenRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class SessionRevocationServiceTest {
    @Test
    void userServiceRevokesSessionsOnRoleChangeAndSoftDeletion() {
        SessionRegistryImpl registry = new SessionRegistryImpl();
        PersistentTokenRepository tokens = mock(PersistentTokenRepository.class);
        SessionRevocationService revocation = new SessionRevocationService(registry, tokens);
        UserRepository users = mock(UserRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        UserMapper mapper = mock(UserMapper.class);
        PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
        UserService service = new UserService(); service.userRepository = users; service.profileRepository = profiles;
        service.userMapper = mapper; service.sessionRevocationService = revocation;
        service.passwordResetTokenRepository = resetTokens;
        User user = user(9L, "member", "USER");
        registry.registerNewSession("role-session", user);
        when(users.findByUsernameForUpdate("member")).thenReturn(java.util.Optional.of(user));
        when(profiles.findByType("ADMIN")).thenReturn(profile("ADMIN"));
        when(users.saveAndFlush(user)).thenReturn(user);
        AdminUserUpdateDTO update = new AdminUserUpdateDTO(); update.setType("ADMIN"); update.setUsername("member"); update.setEmail("member@example.com");

        service.updateUserByUsername(update, "member");
        assertThat(registry.getSessionInformation("role-session").isExpired()).isTrue();
        verify(tokens).removeUserTokens("member");

        User deletedUser = user(10L, "removed", "USER");
        registry.registerNewSession("delete-session", deletedUser);
        when(users.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(deletedUser));
        when(users.saveAndFlush(deletedUser)).thenReturn(deletedUser);
        service.deleteUserById(10L);
        assertThat(registry.getSessionInformation("delete-session").isExpired()).isTrue();
        assertThat(deletedUser.getDeletedAt()).isNotNull();
        verify(tokens).removeUserTokens("removed");
        verify(resetTokens).deleteAllByUserId(10L);
    }

    @Test
    void revokingTheNewOwnerOfARenamedUsernameLeavesTheRenamedAccountAlone() {
        SessionRegistryImpl registry = new SessionRegistryImpl();
        PersistentTokenRepository tokens = mock(PersistentTokenRepository.class);
        // Alice (id 3) logged in as "alice" and then renamed herself: her session still holds that name
        User aliceAtLogin = user(3L, "alice", "USER");
        registry.registerNewSession("alice-session", aliceAtLogin);
        // Bob (id 4) later registered the freed "alice" username and logged in
        User bob = user(4L, "alice", "USER");
        registry.registerNewSession("bob-session", bob);

        new SessionRevocationService(registry, tokens).revoke(bob);

        assertThat(registry.getSessionInformation("bob-session").isExpired()).isTrue();
        assertThat(registry.getSessionInformation("alice-session").isExpired()).isFalse();
    }

    @Test
    void renameKeepsTheCurrentSessionAndDropsRememberMeTokensOfBothUsernames() {
        SessionRegistryImpl registry = new SessionRegistryImpl();
        PersistentTokenRepository tokens = mock(PersistentTokenRepository.class);
        User atLogin = user(3L, "alice", "USER");
        registry.registerNewSession("current", atLogin);
        registry.registerNewSession("other", atLogin);
        User renamed = user(3L, "alice_new", "USER");

        new SessionRevocationService(registry, tokens).revokeOthers(renamed, "alice", "current");

        assertThat(registry.getSessionInformation("current").isExpired()).isFalse();
        assertThat(registry.getSessionInformation("other").isExpired()).isTrue();
        verify(tokens).removeUserTokens("alice");
        verify(tokens).removeUserTokens("alice_new");
    }

    @Test
    void revokeExpiresEveryActiveSessionAndRememberMeToken() {
        SessionRegistryImpl registry = new SessionRegistryImpl();
        PersistentTokenRepository tokens = mock(PersistentTokenRepository.class);
        User user = user(3L, "alice", "USER");
        registry.registerNewSession("session-one", user);
        registry.registerNewSession("session-two", user);
        SessionInformation one = registry.getSessionInformation("session-one");
        SessionInformation two = registry.getSessionInformation("session-two");

        new SessionRevocationService(registry, tokens).revoke(user);

        assertThat(one.isExpired()).isTrue(); assertThat(two.isExpired()).isTrue();
        verify(tokens).removeUserTokens("alice");
    }

    @Test
    void passwordChangeKeepsCurrentSessionAndExpiresOtherSessions() {
        SessionRegistryImpl registry = new SessionRegistryImpl();
        PersistentTokenRepository tokens = mock(PersistentTokenRepository.class);
        User user = user(3L, "alice", "USER");
        registry.registerNewSession("current", user);
        registry.registerNewSession("other", user);

        new SessionRevocationService(registry, tokens).revokeOthers(user, "current");

        assertThat(registry.getSessionInformation("current").isExpired()).isFalse();
        assertThat(registry.getSessionInformation("other").isExpired()).isTrue();
        verify(tokens).removeUserTokens("alice");
    }

    private static User user(Long id, String username, String role) {
        User user = new User(); user.setId(id); user.setUsername(username);
        Profile profile = new Profile(); profile.setType(role); user.setProfile(profile); return user;
    }
    private static Profile profile(String type) { Profile p = new Profile(); p.setType(type); return p; }
}
