package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.model.User;
import org.mapstruct.Mapper;

@Mapper (componentModel = "spring")
public interface UserMapper {
    UserDTO userToUserDTO(User user);
}