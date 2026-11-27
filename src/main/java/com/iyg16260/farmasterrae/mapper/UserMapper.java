package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.user.AdminUserUpdateDTO;
import com.iyg16260.farmasterrae.dto.user.SelfProfileUpdateDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.model.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper (componentModel = "spring")
public interface UserMapper {

    @Mapping (target = "type", source = "profile.type")
    UserDTO userToUserDTO(User user);

    SelfProfileUpdateDTO userToSelfProfileUpdateDTO(User user);

    @Mapping (target = "id", ignore = true)
    @Mapping (target = "authorities", ignore = true)
    @Mapping (target = "password", ignore = true)
    @Mapping (target = "deletedAt", ignore = true)
    @Mapping (target = "profile", ignore = true)
    @Mapping (target = "productList", ignore = true)
    @Mapping (target = "orderList", ignore = true)
    @Mapping (target = "reviewList", ignore = true)
    User userDTOToUser(UserDTO userDTO);

    /**
     * Copia en {@code user} los datos que el propio usuario puede cambiar. Un campo {@code null} (no enviado)
     * conserva el valor guardado; un campo opcional enviado en blanco se borra.
     */
    @BeanMapping (nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping (target = "id", ignore = true)
    @Mapping (target = "authorities", ignore = true)
    @Mapping (target = "password", ignore = true)
    @Mapping (target = "deletedAt", ignore = true)
    @Mapping (target = "profile", ignore = true)
    @Mapping (target = "productList", ignore = true)
    @Mapping (target = "orderList", ignore = true)
    @Mapping (target = "reviewList", ignore = true)
    @Mapping (target = "fullName", qualifiedByName = "blankToNull")
    @Mapping (target = "phone", qualifiedByName = "blankToNull")
    @Mapping (target = "address", qualifiedByName = "blankToNull")
    void updateUserFromSelfProfileDTO(SelfProfileUpdateDTO profileDTO, @MappingTarget User user);

    /**
     * Igual que {@link #updateUserFromSelfProfileDTO}. El rol ({@code type}) nunca se copia aquí: lo valida y
     * asigna {@code UserService}.
     */
    @BeanMapping (nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping (target = "id", ignore = true)
    @Mapping (target = "authorities", ignore = true)
    @Mapping (target = "password", ignore = true)
    @Mapping (target = "deletedAt", ignore = true)
    @Mapping (target = "profile", ignore = true)
    @Mapping (target = "productList", ignore = true)
    @Mapping (target = "orderList", ignore = true)
    @Mapping (target = "reviewList", ignore = true)
    @Mapping (target = "fullName", qualifiedByName = "blankToNull")
    @Mapping (target = "phone", qualifiedByName = "blankToNull")
    @Mapping (target = "address", qualifiedByName = "blankToNull")
    void updateUserFromAdminDTO(AdminUserUpdateDTO userDTO, @MappingTarget User user);

    /**
     * Recorta un campo opcional y lo convierte en {@code null} si queda vacío, para que "borrar" un campo
     * del formulario no guarde cadenas en blanco.
     */
    @Named ("blankToNull")
    default String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
