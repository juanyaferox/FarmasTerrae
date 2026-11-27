package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.auth.ChangePasswordRequestDTO;
import com.iyg16260.farmasterrae.dto.auth.ForgotPasswordRequestDTO;
import com.iyg16260.farmasterrae.dto.auth.ResetPasswordRequestDTO;
import com.iyg16260.farmasterrae.dto.auth.RegisterFormDTO;
import com.iyg16260.farmasterrae.model.PasswordResetToken;
import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.PasswordResetTokenRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;

class AuthServicePasswordLifecycleTest {
    private UserRepository users;
    private ProfileRepository profiles;
    private PasswordResetTokenRepository tokens;
    private JavaMailSender mail;
    private SessionRevocationService sessions;
    private BCryptPasswordEncoder encoder;
    private AuthService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class); profiles = mock(ProfileRepository.class);
        tokens = mock(PasswordResetTokenRepository.class); mail = mock(JavaMailSender.class);
        sessions = mock(SessionRevocationService.class); encoder = new BCryptPasswordEncoder();
        service = new AuthService(users, profiles, tokens, encoder, mail, sessions, "noreply@example.com", "https://farmas.example");
        user = user(42L, "alice", "alice.db@example.com", encoder.encode("CurrentPassword8"));
    }

    @Test
    void forgotPasswordReturnsSameNeutralMessageForExistingAndMissingEmails() throws Exception {
        when(users.findByEmail("alice.db@example.com")).thenReturn(Optional.of(user));
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mail.createMimeMessage()).thenReturn(message);
        ForgotPasswordRequestDTO existing = forgot(" ALICE.DB@example.com ");
        ForgotPasswordRequestDTO missing = forgot("missing@example.com");

        service.setPassword(existing);
        service.setPassword(missing);

        assertThat(existing.getMessage()).isEqualTo(missing.getMessage())
                .isEqualTo("Si el correo está registrado, recibirás un enlace para restablecer tu contraseña.");
        assertThat(existing.getMessageType()).isEqualTo(missing.getMessageType()).isEqualTo("success");
        verify(users, never()).save(any());
        verify(mail).send(message);
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("alice.db@example.com");
    }

    @Test
    void forgotPasswordStoresOnlyTokenHashAndSendsDbAddressResetLink() throws Exception {
        when(users.findByEmail("alice.db@example.com")).thenReturn(Optional.of(user));
        when(tokens.save(any(PasswordResetToken.class))).thenAnswer(inv -> {
            PasswordResetToken t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties())); when(mail.createMimeMessage()).thenReturn(message);
        service.setPassword(forgot("ALICE.DB@example.com"));

        var saved = org.mockito.ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(saved.capture());
        InOrder persistenceBeforeEmail = inOrder(tokens, mail);
        persistenceBeforeEmail.verify(tokens).save(any(PasswordResetToken.class));
        persistenceBeforeEmail.verify(mail).send(message);
        persistenceBeforeEmail.verify(tokens).deleteByUser_IdAndUsedAtIsNullAndIdNot(42L, 99L);
        PasswordResetToken resetToken = saved.getValue();
        assertThat(resetToken.getTokenHash()).hasSize(64).doesNotContain("=");
        assertThat(resetToken.getExpiresAt()).isEqualTo(resetToken.getCreatedAt().plusSeconds(1800));
        assertThat(resetToken.getUser()).isSameAs(user);
        assertThat(encoder.matches("CurrentPassword8", user.getPassword())).isTrue();
        String body = message.getContent().toString();
        assertThat(body).contains("https://farmas.example/auth/resetPassword?token=");
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(body);
        assertThat(matcher.find()).isTrue();
        assertThat(resetToken.getTokenHash()).isEqualTo(hash(matcher.group(1)));
        assertThat(body).doesNotContain("ALICE.DB@example.com");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("alice.db@example.com");
    }

    @Test
    void failedResetEmailCleansUpNewTokenAndKeepsExistingTokens() {
        when(users.findByEmail("alice.db@example.com")).thenReturn(Optional.of(user));
        when(tokens.save(any(PasswordResetToken.class))).thenAnswer(inv -> {
            PasswordResetToken t = inv.getArgument(0);
            t.setId(77L);
            return t;
        });
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mail.createMimeMessage()).thenReturn(message);
        doThrow(new RuntimeException("SMTP down")).when(mail).send(message);

        ForgotPasswordRequestDTO response = service.setPassword(forgot("alice.db@example.com"));

        assertThat(response.getMessageType()).isEqualTo("success");
        verify(tokens).deleteById(77L);
        verify(tokens, never()).deleteByUser_IdAndUsedAtIsNullAndIdNot(any(), any());
    }

    @Test
    void resetTokenIsSingleUseAndChangesPasswordAndRevokesSessions() throws Exception {
        PasswordResetToken token = validToken(user);
        String raw = "raw-reset-token";
        when(tokens.findByTokenHash(hash(raw))).thenReturn(Optional.of(token));
        when(users.findById(42L)).thenReturn(Optional.of(user));

        ResetPasswordRequestDTO request = reset(raw, "AnotherPassword9!");
        assertThat(service.resetPassword(request).getMessageType()).isEqualTo("success");
        assertThat(encoder.matches("AnotherPassword9!", user.getPassword())).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        verify(sessions).revoke(user);
        verify(users).save(user);

        ResetPasswordRequestDTO secondUse = reset(raw, "ThirdPassword8!");
        assertThat(service.resetPassword(secondUse).getMessageType()).isEqualTo("error");
        verify(users, times(1)).save(user);
    }

    @Test
    void invalidAndExpiredResetTokensAreRejectedWithoutSavingPassword() throws Exception {
        String raw = "old-token";
        PasswordResetToken expired = validToken(user); expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(tokens.findByTokenHash(hash(raw))).thenReturn(Optional.of(expired), Optional.empty());
        assertThat(service.resetPassword(reset(raw, "AnotherPassword9!")).getMessageType()).isEqualTo("error");
        assertThat(service.resetPassword(reset("not-found", "AnotherPassword9!")).getMessageType()).isEqualTo("error");
        verify(users, never()).save(any());
        verify(sessions, never()).revoke(any());
    }

    @Test
    void authenticatedPasswordChangeRejectsWrongCurrentPassword() {
        when(users.findById(42L)).thenReturn(Optional.of(user));
        ChangePasswordRequestDTO request = change("incorrect", "AnotherPassword9!");
        assertThat(service.setPasswordAuthenticated(request, user, "current-session").getMessage())
                .isEqualTo("La contraseña actual es incorrecta.");
        verify(users, never()).save(any());
        verify(sessions, never()).revokeOthers(any(), any());
    }

    @Test
    void authenticatedPasswordChangeReloadsFreshDemotedUserAndNeverSavesStalePrincipal() {
        User staleAdminPrincipal = user(42L, "alice", "alice.db@example.com", encoder.encode("CurrentPassword8"));
        staleAdminPrincipal.setProfile(profile("ADMIN"));
        User freshDemotedUser = user(42L, "alice", "alice.db@example.com", encoder.encode("CurrentPassword8"));
        freshDemotedUser.setProfile(profile("USER"));
        when(users.findById(42L)).thenReturn(Optional.of(freshDemotedUser));

        ChangePasswordRequestDTO result = service.setPasswordAuthenticated(change("CurrentPassword8", "AnotherPassword9!"), staleAdminPrincipal, "current-session");

        assertThat(result.getMessageType()).isEqualTo("success");
        assertThat(freshDemotedUser.getProfile().getType()).isEqualTo("USER");
        assertThat(encoder.matches("AnotherPassword9!", freshDemotedUser.getPassword())).isTrue();
        verify(users).save(same(freshDemotedUser));
        verify(users, never()).save(same(staleAdminPrincipal));
        verify(tokens).deleteByUser_IdAndUsedAtIsNull(42L);
        verify(sessions).revokeOthers(freshDemotedUser, "current-session");
    }

    @Test
    void authenticatedPasswordChangeHandlesNullFieldsWithoutNpe() {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO();
        assertThat(service.setPasswordAuthenticated(request, user, null).getMessageType()).isEqualTo("error");
    }

    @Test
    void registrationRejectsMismatchedConfirmationBeforePersistence() {
        RegisterFormDTO form = registration("Alice", "Alice@Example.com"); form.setConfirmPassword("DifferentPassword8!");
        assertThatThrownBy(() -> service.setRegister(form)).isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(users, profiles);
    }

    @Test
    void registrationNormalizesUsernameAndEmailBeforeDuplicateChecks() {
        RegisterFormDTO form = registration(" Alice ", " ALICE@Example.com ");
        when(users.existsByUsername("alice")).thenReturn(true);
        assertThatThrownBy(() -> service.setRegister(form)).isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(users).existsByUsername("alice");
        verify(users, never()).existsByUsername(" Alice ");
        verify(users, never()).save(any());
    }

    @Test
    void registrationConvertsUniqueConstraintRaceIntoConflict() {
        RegisterFormDTO form = registration("Alice", "ALICE@Example.com");
        when(profiles.findByType("USER")).thenReturn(profile("USER"));
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> service.setRegister(form)).isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void invalidUsernameIsRejectedByBeanValidationBeforePersistence() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        RegisterFormDTO form = registration("bad username", "alice@example.com");
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        verifyNoInteractions(users, profiles);
    }

    @Test
    void passwordPolicyAllowsSymbolsAndPassphrasesButEnforcesBoundariesAndByteLimit() {
        assertThat(com.iyg16260.farmasterrae.utils.PasswordUtils.isValidPassword("Pass phrase 9!" )).isTrue();
        assertThat(com.iyg16260.farmasterrae.utils.PasswordUtils.isValidPassword("Short9!" )).isFalse();
        assertThat(com.iyg16260.farmasterrae.utils.PasswordUtils.isValidPassword("A" + "a".repeat(62) + "9!" )).isFalse();
        assertThat(com.iyg16260.farmasterrae.utils.PasswordUtils.isValidPassword("Á".repeat(40) + "A1a" )).isFalse();
    }

    @Test
    void missingUsernameThrowsUsernameNotFoundException() {
        UserService userService = new UserService(); userService.userRepository = users;
        when(users.findByUsername("unknown")).thenReturn(Optional.empty());
        when(users.findByEmail("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.loadUserByUsername("  UNKNOWN ")).isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Usuario no encontrado");
        verify(users).findByUsername("unknown");
        verify(users).findByEmail("unknown");
    }

    private static ForgotPasswordRequestDTO forgot(String email) { ForgotPasswordRequestDTO d = new ForgotPasswordRequestDTO(); d.setEmail(email); return d; }
    private static ResetPasswordRequestDTO reset(String token, String password) { ResetPasswordRequestDTO d = new ResetPasswordRequestDTO(); d.setToken(token); d.setNewPassword(password); d.setConfirmPassword(password); return d; }
    private static ChangePasswordRequestDTO change(String current, String next) { ChangePasswordRequestDTO d = new ChangePasswordRequestDTO(); d.setCurrentPassword(current); d.setNewPassword(next); d.setConfirmPassword(next); return d; }
    private static RegisterFormDTO registration(String username, String email) { RegisterFormDTO d = new RegisterFormDTO(); d.setUsername(username); d.setEmail(email); d.setPassword("GoodPassword9!"); d.setConfirmPassword("GoodPassword9!"); return d; }
    private static User user(Long id, String username, String email, String password) { User u = new User(); u.setId(id); u.setUsername(username); u.setEmail(email); u.setPassword(password); u.setProfile(profile("USER")); return u; }
    private static Profile profile(String type) { Profile p = new Profile(); p.setType(type); return p; }
    private static PasswordResetToken validToken(User user) { PasswordResetToken t = new PasswordResetToken(); t.setUser(user); t.setTokenHash("a".repeat(64)); t.setCreatedAt(Instant.now()); t.setExpiresAt(Instant.now().plusSeconds(1800)); return t; }
    private static String hash(String raw) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
}
