package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.auth.ChangePasswordRequestDTO;
import com.iyg16260.farmasterrae.dto.auth.ForgotPasswordRequestDTO;
import com.iyg16260.farmasterrae.dto.auth.ResetPasswordRequestDTO;
import com.iyg16260.farmasterrae.dto.auth.RegisterFormDTO;
import com.iyg16260.farmasterrae.model.PasswordResetToken;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.PasswordResetTokenRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.utils.PasswordUtils;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service @Slf4j
public class AuthService {
    private static final String NEUTRAL_RESET_MESSAGE = "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña.";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender javaMailSender;
    private final SessionRevocationService sessionRevocationService;
    private final String fromAddress;
    private final String baseUrl;

    public AuthService(UserRepository userRepository, ProfileRepository profileRepository,
                       PasswordResetTokenRepository resetTokenRepository, PasswordEncoder passwordEncoder,
                       JavaMailSender javaMailSender, SessionRevocationService sessionRevocationService,
                       @Value("${spring.mail.username:}") String fromAddress,
                       @Value("${app.base-url}") String baseUrl) {
        this.userRepository = userRepository; this.profileRepository = profileRepository;
        this.resetTokenRepository = resetTokenRepository; this.passwordEncoder = passwordEncoder;
        this.javaMailSender = javaMailSender; this.sessionRevocationService = sessionRevocationService;
        this.fromAddress = fromAddress; this.baseUrl = baseUrl;
    }

    @Transactional
    public void setRegister(RegisterFormDTO form) {
        String username = normalize(form.getUsername());
        String email = normalize(form.getEmail());
        if (form.getPassword() == null || !form.getPassword().equals(form.getConfirmPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No coinciden las contraseñas");
        if (!PasswordUtils.isValidPassword(form.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no cumple los requisitos de seguridad");
        if (userRepository.existsByUsername(username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuario ya registrado");
        if (userRepository.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ya registrado");
        User user = new User();
        user.setUsername(username); user.setEmail(email); user.setFullName(form.getFullName());
        user.setPassword(passwordEncoder.encode(form.getPassword())); user.setPhone(form.getPhone());
        user.setProfile(profileRepository.findByType("USER"));
        try { userRepository.saveAndFlush(user); }
        catch (DataIntegrityViolationException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuario o email ya registrado"); }
    }

    public ForgotPasswordRequestDTO setPassword(ForgotPasswordRequestDTO request) {
        String email = normalize(request.getEmail());
        PasswordResetToken created = null;
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                String rawToken = generateRawToken();
                Instant now = Instant.now();
                PasswordResetToken token = new PasswordResetToken();
                token.setUser(user);
                token.setTokenHash(hashToken(rawToken));
                token.setCreatedAt(now);
                token.setExpiresAt(now.plusSeconds(30 * 60L));
                created = resetTokenRepository.save(token);
                sendResetLink(user, rawToken);
                if (created.getId() != null) {
                    resetTokenRepository.deleteByUser_IdAndUsedAtIsNullAndIdNot(user.getId(), created.getId());
                }
            }
        } catch (Exception e) {
            if (created != null && created.getId() != null) {
                try {
                    resetTokenRepository.deleteById(created.getId());
                } catch (Exception ignored) {
                    // Best-effort cleanup of undelivered token
                }
            }
            // Never include submitted or stored email addresses in logs or user-visible responses.
            log.warn("Password reset link could not be sent");
        }
        request.setMessage(NEUTRAL_RESET_MESSAGE);
        request.setMessageType("success");
        return request;
    }

    @Transactional(readOnly = true)
    public boolean isResetTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        Instant now = Instant.now();
        return resetTokenRepository.findFirstByTokenHash(hashToken(rawToken))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(now))
                .isPresent();
    }

    @Transactional
    public ResetPasswordRequestDTO resetPassword(ResetPasswordRequestDTO request) {
        if (request.getNewPassword() == null || !PasswordUtils.isValidPassword(request.getNewPassword()))
            return resetError(request, "La contraseña debe tener entre 12 y 64 caracteres, mayúsculas, minúsculas y números.");
        if (!request.getNewPassword().equals(request.getConfirmPassword())) return resetError(request, "No coinciden las contraseñas.");
        if (request.getToken() == null || request.getToken().isBlank()) return resetError(request, "El enlace no es válido o ha caducado.");
        PasswordResetToken token = resetTokenRepository.findByTokenHash(hashToken(request.getToken())).orElse(null);
        Instant now = Instant.now();
        if (token == null || token.getUsedAt() != null || !token.getExpiresAt().isAfter(now))
            return resetError(request, "El enlace no es válido o ha caducado.");
        User user = userRepository.findById(token.getUser().getId()).orElse(null);
        if (user == null) return resetError(request, "El enlace no es válido o ha caducado.");
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        token.setUsedAt(now);
        resetTokenRepository.save(token);
        resetTokenRepository.deleteByUser_IdAndUsedAtIsNull(user.getId());
        sessionRevocationService.revoke(user);
        request.setMessage("Contraseña restablecida correctamente. Ya puedes iniciar sesión.");
        request.setMessageType("success");
        return request;
    }

    @Transactional
    public ChangePasswordRequestDTO setPasswordAuthenticated(ChangePasswordRequestDTO request, User principal, String currentSessionId) {
        if (principal == null || principal.getId() == null) return changeError(request, "No se pudo identificar la cuenta.");
        if (request.getCurrentPassword() == null || request.getNewPassword() == null || request.getConfirmPassword() == null)
            return changeError(request, "Completa todos los campos.");
        if (!PasswordUtils.isValidPassword(request.getNewPassword())) return changeError(request, "La contraseña debe tener entre 12 y 64 caracteres, mayúsculas, minúsculas y números.");
        if (!request.getNewPassword().equals(request.getConfirmPassword())) return changeError(request, "No coinciden las contraseñas.");
        User freshUser = userRepository.findById(principal.getId()).orElse(null);
        if (freshUser == null) return changeError(request, "No se pudo identificar la cuenta.");
        if (freshUser.getPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), freshUser.getPassword()))
            return changeError(request, "La contraseña actual es incorrecta.");
        freshUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(freshUser);
        resetTokenRepository.deleteByUser_IdAndUsedAtIsNull(freshUser.getId());
        sessionRevocationService.revokeOthers(freshUser, currentSessionId);
        request.setMessage("Contraseña cambiada correctamente."); request.setMessageType("success");
        return request;
    }

    private void sendResetLink(User user, String rawToken) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress); helper.setTo(user.getEmail());
            helper.setSubject("Restablecer contraseña - FarmasTerrae");
            String link = baseUrl.replaceAll("/$", "") + "/auth/resetPassword?token=" + rawToken;
            helper.setText("Hola " + user.getUsername() + ",\n\nUsa este enlace para elegir una contraseña nueva (válido durante 30 minutos):\n" + link);
            javaMailSender.send(message);
        } catch (Exception e) { throw new IllegalStateException("Could not send reset email", e); }
    }

    private String generateRawToken() { byte[] bytes = new byte[32]; SECURE_RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hashToken(String token) {
        try { byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static ResetPasswordRequestDTO resetError(ResetPasswordRequestDTO dto, String message) { dto.setMessage(message); dto.setMessageType("error"); return dto; }
    private static ChangePasswordRequestDTO changeError(ChangePasswordRequestDTO dto, String message) { dto.setMessage(message); dto.setMessageType("error"); return dto; }
}
