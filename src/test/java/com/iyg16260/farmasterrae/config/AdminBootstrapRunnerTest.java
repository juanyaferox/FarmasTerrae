package com.iyg16260.farmasterrae.config;

import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith ({MockitoExtension.class, OutputCaptureExtension.class})
class AdminBootstrapRunnerTest {

    private static final String USERNAME = "Root_Admin";
    private static final String EMAIL = "root.admin@example.com";
    private static final String PASSWORD = "Sup3rSecret!12";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrapRunner runner(String username, String email, String password) {
        return new AdminBootstrapRunner(userRepository, profileRepository, passwordEncoder, username, email, password);
    }

    private static Profile adminProfile() {
        Profile profile = new Profile();
        profile.setId(1L);
        profile.setType("ADMIN");
        return profile;
    }

    @Test
    void createsTheInitialAdminWhenConfiguredAndNoAdminExists(CapturedOutput output) throws Exception {
        when(userRepository.findAllByProfileType("ADMIN")).thenReturn(List.of());
        when(profileRepository.findByType("ADMIN")).thenReturn(adminProfile());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");

        runner(" " + USERNAME + " ", EMAIL, PASSWORD).run(new DefaultApplicationArguments());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("root_admin");
        assertThat(saved.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getValue().getProfile().getType()).isEqualTo("ADMIN");
        assertThat(output).contains("created the initial ADMIN account")
                .doesNotContain(PASSWORD)
                .doesNotContain(EMAIL)
                .doesNotContain("root_admin");
    }

    @ParameterizedTest
    @CsvSource (value = {
            "'', '', ''",
            ",,",
            "root_admin, '', ''",
            "'', root.admin@example.com, Sup3rSecret",
            "root_admin, root.admin@example.com, '   '",
            "root_admin, root.admin@example.com,"
    })
    void doesNothingUnlessAllThreeValuesAreSet(String username, String email, String password) {
        assertThat(runner(username, email, password).bootstrapAdmin()).isFalse();

        verifyNoInteractions(userRepository, profileRepository, passwordEncoder);
    }

    @Test
    void skipsWhenAnAdminAlreadyExists() {
        when(userRepository.findAllByProfileType("ADMIN")).thenReturn(List.of(new User()));

        assertThat(runner(USERNAME, EMAIL, PASSWORD).bootstrapAdmin()).isFalse();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(profileRepository, passwordEncoder);
    }

    @Test
    void skipsWhenTheAdminProfileHasNotBeenSeeded() {
        when(userRepository.findAllByProfileType("ADMIN")).thenReturn(List.of());
        when(profileRepository.findByType("ADMIN")).thenReturn(null);

        assertThat(runner(USERNAME, EMAIL, PASSWORD).bootstrapAdmin()).isFalse();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void skipsWhenTheUsernameOrEmailIsAlreadyTaken() {
        when(userRepository.findAllByProfileType("ADMIN")).thenReturn(List.of());
        when(profileRepository.findByType("ADMIN")).thenReturn(adminProfile());
        when(userRepository.existsByUsername("root_admin")).thenReturn(false);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThat(runner(USERNAME, EMAIL, PASSWORD).bootstrapAdmin()).isFalse();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @ParameterizedTest
    @CsvSource ({
            "ab, root.admin@example.com, Sup3rSecret",
            "root admin, root.admin@example.com, Sup3rSecret",
            "root_admin, not-an-email, Sup3rSecret",
            "root_admin, root.admin@example.com, 123",
            "root_admin, root.admin@example.com, nouppercase1",
            "root_admin, root.admin@example.com, ThisPasswordNoDigit!",
            "root_admin, root.admin@example.com, ThisPasswordIsMoreThanSixtyFourCharactersLongWithUppercaseAnd1234567890"
    })
    void rejectsInvalidSettingsWithoutLoggingThem(String username, String email, String password,
                                                 CapturedOutput output) {
        when(userRepository.findAllByProfileType("ADMIN")).thenReturn(List.of());

        assertThat(runner(username, email, password).bootstrapAdmin()).isFalse();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(profileRepository, passwordEncoder);
        assertThat(output).contains("Admin bootstrap skipped").doesNotContain(password);
    }

    @Test
    void reportsAConflictInsteadOfFailingStartup(CapturedOutput output) {
        when(userRepository.findAllByProfileType("ADMIN")).thenReturn(List.of());
        when(profileRepository.findByType("ADMIN")).thenReturn(adminProfile());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThat(runner(USERNAME, EMAIL, PASSWORD).bootstrapAdmin()).isFalse();

        assertThat(output).contains("Admin bootstrap skipped").doesNotContain(PASSWORD);
    }

    @Test
    void runsAfterTheLiquibaseSeed() {
        assertThat(AdminBootstrapRunner.RUNNER_ORDER).isGreaterThan(LiquibaseRunnerConfig.RUNNER_ORDER);
    }
}
