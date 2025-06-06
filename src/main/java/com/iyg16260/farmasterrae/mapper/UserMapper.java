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

    User userDTOToUser(UserDTO userDTO);

    void updateUserFromUserDTO(UserDTO userDTO, @MappingTarget User user);
}