package com.iyg16260.farmasterrae.controller.admin;

import com.iyg16260.farmasterrae.dto.response.MessageResponseDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static com.iyg16260.farmasterrae.utils.SuccessMessageUtils.buildSuccessMessage;

@RestController
@RequestMapping ("/api/admin/users")
public class DashboardUserController {

    @Autowired
    UserService userService;

    @GetMapping ("/{username}")
    public UserDTO getUser(@PathVariable String username) {
        return userService.getUserByUsername(username);
    }

    @PutMapping ("/{oldUsername}")
    public MessageResponseDTO updateUser(@Valid @RequestBody UserDTO userDTO,
                                         @PathVariable String oldUsername) {
        userService.updateUserByUsername(userDTO, oldUsername);
        return buildSuccessMessage(EntityType.USERS, Operation.PUT);
    }

    @DeleteMapping ("/{username}")
    public MessageResponseDTO deleteUser(@PathVariable String username) {
        userService.deleteUserByUsername(username);
        return buildSuccessMessage(EntityType.USERS, Operation.DELETE);
    }
}
