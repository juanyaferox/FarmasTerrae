package com.iyg16260.farmasterrae.dto.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The auth form DTOs are logged by validation and binding errors: their toString must never carry a credential.
 */
class AuthDtoToStringTest {

    private static final String CURRENT = "CurrentSecret1234";
    private static final String NEW = "BrandNewSecret5678";
    private static final String TOKEN = "reset-token-0f3c9a1b2d";

    @Test
    void registerFormHidesBothPasswords() {
        RegisterFormDTO form = new RegisterFormDTO();
        form.setUsername("ana_perez");
        form.setEmail("ana@example.com");
        form.setPassword(NEW);
        form.setConfirmPassword(NEW);

        assertThat(form.toString()).contains("ana_perez").doesNotContain(NEW);
    }

    @Test
    void changePasswordRequestHidesEveryPassword() {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO();
        request.setCurrentPassword(CURRENT);
        request.setNewPassword(NEW);
        request.setConfirmPassword(NEW);
        request.setMessage("Contraseña cambiada correctamente.");

        assertThat(request.toString()).contains("Contraseña cambiada").doesNotContain(CURRENT).doesNotContain(NEW);
    }

    @Test
    void resetPasswordRequestHidesTheTokenAndThePasswords() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setToken(TOKEN);
        request.setNewPassword(NEW);
        request.setConfirmPassword(NEW);

        assertThat(request.toString()).doesNotContain(TOKEN).doesNotContain(NEW);
    }
}
