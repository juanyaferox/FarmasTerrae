package com.iyg16260.farmasterrae.mapper.user;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.User;

public interface UserMapper {
    UserDTO userToUserDTO(User user);
}
