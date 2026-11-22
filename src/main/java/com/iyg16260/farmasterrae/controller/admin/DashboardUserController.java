package com.iyg16260.farmasterrae.controller.admin;

import com.iyg16260.farmasterrae.dto.response.MessageResponseDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static com.iyg16260.farmasterrae.config.OpenApiConfig.*;
import static com.iyg16260.farmasterrae.utils.SuccessMessageUtils.buildSuccessMessage;

@Tag (name = TAG_ADMIN_USERS)
@SecurityRequirement (name = SESSION_COOKIE)
@ApiResponse (responseCode = "401", ref = UNAUTHORIZED)
@ApiResponse (responseCode = "403", ref = FORBIDDEN)
@RestController
@RequestMapping ("/api/admin/users")
public class DashboardUserController {

    @Autowired
    UserService userService;

    @io.swagger.v3.oas.annotations.Operation (summary = "Datos de un usuario")
    @ApiResponse (responseCode = "200", description = "Datos del usuario")
    @ApiResponse (responseCode = "409", ref = CONFLICT)
    @GetMapping ("/{username}")
    public UserDTO getUser(@Parameter (description = "Nombre de usuario", example = "maria")
                           @PathVariable String username) {
        return userService.getUserByUsername(username);
    }

    @io.swagger.v3.oas.annotations.Operation (summary = "Actualizar un usuario",
            description = "Actualiza el usuario indicado en la ruta (el nombre de usuario puede cambiarse en el cuerpo)")
    @ApiResponse (responseCode = "200", description = "Usuario actualizado")
    @ApiResponse (responseCode = "400", ref = BAD_REQUEST)
    @ApiResponse (responseCode = "409", ref = CONFLICT)
    @PutMapping ("/{oldUsername}")
    public MessageResponseDTO updateUser(@Valid @RequestBody UserDTO userDTO,
                                         @Parameter (description = "Nombre de usuario actual", example = "maria")
                                         @PathVariable String oldUsername) {
        userService.updateUserByUsername(userDTO, oldUsername);
        return buildSuccessMessage(EntityType.USERS, Operation.PUT);
    }

    @io.swagger.v3.oas.annotations.Operation (summary = "Eliminar un usuario",
            description = "Elimina el usuario anonimizando sus datos. Debe quedar al menos un administrador")
    @ApiResponse (responseCode = "200", description = "Usuario eliminado")
    @ApiResponse (responseCode = "400", ref = BAD_REQUEST)
    @ApiResponse (responseCode = "404", ref = NOT_FOUND)
    @DeleteMapping ("/{username}")
    public MessageResponseDTO deleteUser(@Parameter (description = "Nombre de usuario", example = "pedro")
                                         @PathVariable String username) {
        userService.deleteUserByUsername(username);
        return buildSuccessMessage(EntityType.USERS, Operation.DELETE);
    }
}
