package com.iyg16260.farmasterrae.config;

import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.utils.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Creates the first {@code ADMIN} account from the {@code ADMIN_BOOTSTRAP_USERNAME},
 * {@code ADMIN_BOOTSTRAP_EMAIL} and {@code ADMIN_BOOTSTRAP_PASSWORD} settings.
 * <p>
 * Production only seeds the ADMIN/USER profiles, so this is how a fresh deployment gets its
 * administrator. Nothing happens unless all three values are set, and nothing happens once any
 * {@code ADMIN} user exists. Credentials are never logged.
 */
@Slf4j
@Component
@Order (AdminBootstrapRunner.RUNNER_ORDER)
public class AdminBootstrapRunner implements ApplicationRunner {

    /** Runs right after {@link LiquibaseRunnerConfig}, which seeds the ADMIN profile in prod. */
    public static final int RUNNER_ORDER = LiquibaseRunnerConfig.RUNNER_ORDER + 1;

    static final String ADMIN_PROFILE = "ADMIN";

    /** Same rule that {@code User} enforces before persisting a username. */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,20}$");
    /** Basic sanity check only (registration relies on {@code @Email}). */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+$");

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public AdminBootstrapRunner(UserRepository userRepository,
                                ProfileRepository profileRepository,
                                PasswordEncoder passwordEncoder,
                                @Value ("${app.admin-bootstrap.username:}") String username,
                                @Value ("${app.admin-bootstrap.email:}") String email,
                                @Value ("${app.admin-bootstrap.password:}") String password) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapAdmin();
    }

    /**
     * @return {@code true} if the initial ADMIN account was created
     */
    boolean bootstrapAdmin() {
        boolean hasUsername = StringUtils.hasText(username);
        boolean hasEmail = StringUtils.hasText(email);
        boolean hasPassword = StringUtils.hasText(password);
        if (!hasUsername && !hasEmail && !hasPassword) {
            return false;
        }
        if (!hasUsername || !hasEmail || !hasPassword) {
            log.warn("Admin bootstrap skipped: ADMIN_BOOTSTRAP_USERNAME, ADMIN_BOOTSTRAP_EMAIL and "
                    + "ADMIN_BOOTSTRAP_PASSWORD must all be set.");
            return false;
        }
        if (!userRepository.findAllByProfileType(ADMIN_PROFILE).isEmpty()) {
            log.info("Admin bootstrap skipped: an ADMIN account already exists; "
                    + "the ADMIN_BOOTSTRAP_* settings can be removed.");
            return false;
        }

        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        String normalizedEmail = email.trim();
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            log.warn("Admin bootstrap skipped: ADMIN_BOOTSTRAP_USERNAME must have 3-20 letters, digits or underscores.");
            return false;
        }
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            log.warn("Admin bootstrap skipped: ADMIN_BOOTSTRAP_EMAIL is not a valid email address.");
            return false;
        }
        if (!PasswordUtils.isValidPassword(password)) {
            log.warn("Admin bootstrap skipped: ADMIN_BOOTSTRAP_PASSWORD must follow the user password rules "
                    + "(currently 6-20 letters or digits, with at least one uppercase letter and one digit).");
            return false;
        }

        Profile adminProfile = profileRepository.findByType(ADMIN_PROFILE);
        if (adminProfile == null) {
            log.warn("Admin bootstrap skipped: the ADMIN profile does not exist yet (has the profiles seed run?).");
            return false;
        }
        if (userRepository.existsByUsername(normalizedUsername) || userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Admin bootstrap skipped: the configured username or email already belongs to another account.");
            return false;
        }

        User admin = new User();
        admin.setUsername(normalizedUsername);
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setProfile(adminProfile);
        try {
            userRepository.save(admin);
        } catch (DataIntegrityViolationException e) {
            log.warn("Admin bootstrap skipped: the account could not be stored because it conflicts with an existing user.");
            return false;
        }
        log.info("Admin bootstrap: created the initial ADMIN account; "
                + "remove ADMIN_BOOTSTRAP_PASSWORD from the environment.");
        return true;
    }
}
