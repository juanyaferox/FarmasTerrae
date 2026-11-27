package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.user.AdminUserUpdateDTO;
import com.iyg16260.farmasterrae.dto.user.SelfProfileUpdateDTO;
import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Partial updates: {@code null} (field not sent) keeps the stored value, a blank optional field clears it, and
 * identity, role, credentials and deletion state are never copied from a form.
 */
class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    private static User storedUser() {
        Profile profile = new Profile();
        profile.setId(2L);
        profile.setType("USER");
        User user = new User();
        user.setId(7L);
        user.setUsername("stored_user");
        user.setEmail("stored@example.com");
        user.setFullName("Stored Name");
        user.setPhone("+34 600 123 456");
        user.setAddress("Calle Mayor 1");
        user.setPassword("$2a$10$storedhash");
        user.setProfile(profile);
        return user;
    }

    @Test
    void omittedFieldsKeepStoredValuesAndBlankOptionalFieldsAreCleared() {
        User user = storedUser();
        SelfProfileUpdateDTO form = new SelfProfileUpdateDTO();
        form.setEmail("new@example.com");
        form.setPhone("");
        form.setAddress("   ");

        mapper.updateUserFromSelfProfileDTO(form, user);

        assertThat(user.getUsername()).isEqualTo("stored_user");
        assertThat(user.getFullName()).isEqualTo("Stored Name");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPhone()).isNull();
        assertThat(user.getAddress()).isNull();
    }

    @Test
    void optionalValuesAreTrimmed() {
        User user = storedUser();
        SelfProfileUpdateDTO form = new SelfProfileUpdateDTO();
        form.setFullName("  Ana Pérez ");
        form.setPhone(" 600123456 ");
        form.setAddress(" Calle Uría 45 ");

        mapper.updateUserFromSelfProfileDTO(form, user);

        assertThat(user.getFullName()).isEqualTo("Ana Pérez");
        assertThat(user.getPhone()).isEqualTo("600123456");
        assertThat(user.getAddress()).isEqualTo("Calle Uría 45");
    }

    @Test
    void selfProfileMappingNeverTouchesIdentityRoleCredentialsOrDeletion() {
        User user = storedUser();
        Profile profile = user.getProfile();
        SelfProfileUpdateDTO form = new SelfProfileUpdateDTO();
        form.setUsername("renamed_user");
        form.setEmail("renamed@example.com");

        mapper.updateUserFromSelfProfileDTO(form, user);

        assertThat(user.getId()).isEqualTo(7L);
        assertThat(user.getProfile()).isSameAs(profile);
        assertThat(user.getProfile().getType()).isEqualTo("USER");
        assertThat(user.getPassword()).isEqualTo("$2a$10$storedhash");
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getUsername()).isEqualTo("renamed_user");
    }

    @Test
    void adminMappingNeverCopiesTheRoleOrDeletionState() {
        User user = storedUser();
        Profile profile = user.getProfile();
        LocalDateTime deletedAt = LocalDateTime.of(2024, 1, 1, 0, 0);
        user.setDeletedAt(deletedAt);
        AdminUserUpdateDTO form = new AdminUserUpdateDTO();
        form.setType("ADMIN");
        form.setPhone("985123456");

        mapper.updateUserFromAdminDTO(form, user);

        assertThat(user.getProfile()).isSameAs(profile);
        assertThat(user.getProfile().getType()).isEqualTo("USER");
        assertThat(user.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(user.getPassword()).isEqualTo("$2a$10$storedhash");
        assertThat(user.getPhone()).isEqualTo("985123456");
        assertThat(user.getFullName()).isEqualTo("Stored Name");
        assertThat(user.getAddress()).isEqualTo("Calle Mayor 1");
    }

    @Test
    void ownProfileViewCarriesTheEditableFields() {
        SelfProfileUpdateDTO view = mapper.userToSelfProfileUpdateDTO(storedUser());

        assertThat(view.getUsername()).isEqualTo("stored_user");
        assertThat(view.getEmail()).isEqualTo("stored@example.com");
        assertThat(view.getFullName()).isEqualTo("Stored Name");
        assertThat(view.getPhone()).isEqualTo("+34 600 123 456");
        assertThat(view.getAddress()).isEqualTo("Calle Mayor 1");
    }
}
