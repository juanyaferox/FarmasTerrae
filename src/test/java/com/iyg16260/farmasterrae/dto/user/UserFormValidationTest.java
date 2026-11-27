package com.iyg16260.farmasterrae.dto.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Bean Validation rules of the self-service and admin user forms. */
class UserFormValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static SelfProfileUpdateDTO validSelfForm() {
        SelfProfileUpdateDTO form = new SelfProfileUpdateDTO();
        form.setUsername("valid_user");
        form.setEmail("valid@example.com");
        return form;
    }

    private static AdminUserUpdateDTO validAdminForm() {
        AdminUserUpdateDTO form = new AdminUserUpdateDTO();
        form.setUsername("valid_user");
        form.setEmail("valid@example.com");
        form.setType("USER");
        return form;
    }

    private static <T> Set<String> invalidProperties(T form) {
        return VALIDATOR.validate(form).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void selfProfileFormHasNoPrivilegedFields() {
        assertThat(Arrays.stream(SelfProfileUpdateDTO.class.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(Field::getName))
                .containsExactlyInAnyOrder("username", "fullName", "email", "phone", "address");
    }

    @ParameterizedTest
    @ValueSource (strings = {"+34 600 123 456", "600123456", "985123456", "(+34) 985-12-34-56"})
    void acceptsCommonPhoneFormats(String phone) {
        SelfProfileUpdateDTO self = validSelfForm();
        self.setPhone(phone);
        AdminUserUpdateDTO admin = validAdminForm();
        admin.setPhone(phone);

        assertThat(invalidProperties(self)).isEmpty();
        assertThat(invalidProperties(admin)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource (strings = {"", "ab", "bad user", "has-dash", "#del-0f8fad5b-d9cb-469f-a165-70867728950e", "abcdefghijklmnopqrstu"})
    void rejectsUsernamesThatRegistrationRejects(String username) {
        SelfProfileUpdateDTO self = validSelfForm();
        self.setUsername(username);
        AdminUserUpdateDTO admin = validAdminForm();
        admin.setUsername(username);

        assertThat(invalidProperties(self)).containsExactly("username");
        assertThat(invalidProperties(admin)).containsExactly("username");
    }

    @ParameterizedTest
    @ValueSource (strings = {"", "not-an-email", "two@@example.com"})
    void rejectsInvalidEmails(String email) {
        SelfProfileUpdateDTO self = validSelfForm();
        self.setEmail(email);

        assertThat(invalidProperties(self)).containsExactly("email");
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThat(invalidProperties(new SelfProfileUpdateDTO())).containsExactlyInAnyOrder("username", "email");
        assertThat(invalidProperties(new AdminUserUpdateDTO())).containsExactlyInAnyOrder("username", "email", "type");
    }

    @Test
    void rejectsOverlongFields() {
        SelfProfileUpdateDTO form = validSelfForm();
        form.setFullName("a".repeat(256));
        form.setPhone("6".repeat(31));
        form.setAddress("a".repeat(256));
        form.setEmail("a".repeat(64) + "@" + "b".repeat(190) + ".com");

        assertThat(invalidProperties(form)).containsExactlyInAnyOrder("fullName", "phone", "address", "email");
    }

    @ParameterizedTest
    @ValueSource (strings = {"ADMIN", "USER"})
    void adminFormAcceptsTheKnownProfiles(String type) {
        AdminUserUpdateDTO form = validAdminForm();
        form.setType(type);

        assertThat(invalidProperties(form)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource (strings = {"", "SUPERADMIN", "ROLE_ADMIN", "admin", " ADMIN"})
    void adminFormRejectsUnknownProfiles(String type) {
        AdminUserUpdateDTO form = validAdminForm();
        form.setType(type);

        assertThat(invalidProperties(form)).containsExactly("type");
    }
}
