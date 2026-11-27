package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper (componentModel = "spring")
public interface UserMapper {

    @Mapping (target = "type", source = "profile.type")
    UserDTO userToUserDTO(User user);

    @Mapping (target = "id", ignore = true)
    @Mapping (target = "authorities", ignore = true)
    @Mapping (target = "password", ignore = true)
    @Mapping (target = "deletedAt", ignore = true)
    @Mapping (target = "profile", ignore = true)
    @Mapping (target = "productList", ignore = true)
    @Mapping (target = "orderList", ignore = true)
    @Mapping (target = "reviewList", ignore = true)
    User userDTOToUser(UserDTO userDTO);

    @Mapping (target = "id", ignore = true)
    @Mapping (target = "authorities", ignore = true)
    @Mapping (target = "password", ignore = true)
    @Mapping (target = "deletedAt", ignore = true)
    @Mapping (target = "profile", ignore = true)
    @Mapping (target = "productList", ignore = true)
    @Mapping (target = "orderList", ignore = true)
    @Mapping (target = "reviewList", ignore = true)
    void updateUserFromUserDTO(UserDTO userDTO, @MappingTarget User user);
}
