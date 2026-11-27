package com.iyg16260.farmasterrae.controller.admin;

import com.iyg16260.farmasterrae.dto.user.AdminUserUpdateDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;
import java.util.stream.Collectors;

import static com.iyg16260.farmasterrae.utils.SuccessMessageUtils.buildSuccessMessage;

@Controller
@RequestMapping ("/admin/dashboard/users")
public class DashboardUserController {
    @Autowired
    UserService userService;
    private final String USER_PATH = "redirect:/admin/dashboard/users";

    @GetMapping ("/{username}")
    public ModelAndView getUser(@PathVariable String username) {
        UserDTO userDTO = userService.getUserByUsername(username);
        return new ModelAndView("admin/user-details")
                .addObject("user", userDTO);
    }

    /**
     * Actualiza un usuario desde el panel
     *
     * @throws ResponseStatusException BAD_REQUEST si los datos no son válidos (p. ej. un tipo de usuario desconocido)
     */
    @PutMapping
    public String updateUser(@Valid @ModelAttribute AdminUserUpdateDTO userDTO, BindingResult result,
                             @RequestParam String oldUsername, RedirectAttributes ra) {
        if (result.hasErrors())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, validationMessage(result));
        userService.updateUserByUsername(userDTO, oldUsername);
        buildSuccessMessage(ra, EntityType.USERS, Operation.PUT);
        return USER_PATH;
    }

    @DeleteMapping
    public String deleteUser(@ModelAttribute UserDTO userDTO, RedirectAttributes ra) {
        userService.deleteUserByUsername(userDTO.getUsername());
        buildSuccessMessage(ra, EntityType.USERS, Operation.DELETE);
        return USER_PATH;
    }

    /**
     * Resume los errores de validación con los mensajes fijos de las anotaciones del DTO (nunca con el valor
     * enviado, que el aviso de error muestra sin escapar)
     */
    private static String validationMessage(BindingResult result) {
        String details = result.getFieldErrors().stream()
                .filter(error -> !error.isBindingFailure())
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("; "));
        return details.isEmpty() ? "Datos de usuario no válidos" : "Datos de usuario no válidos: " + details;
    }
}
