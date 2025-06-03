package com.iyg16260.farmasterrae.controller.admin;

import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @PutMapping
    public String updateUser(@Valid @ModelAttribute UserDTO userDTO,
                             @RequestParam String oldUsername, RedirectAttributes ra) {
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
}
