package com.iyg16260.farmasterrae.dto.response;

import com.iyg16260.farmasterrae.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

/**
 * Información del usuario autenticado
 *
 * @param username nombre de usuario
 * @param fullName nombre completo
 * @param roles    roles del usuario
 */
public record AuthenticatedUserDTO(String username, String fullName, List<String> roles) {

    public static AuthenticatedUserDTO from(Authentication authentication) {
        String fullName = authentication.getPrincipal() instanceof User user ? user.getFullName() : null;
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return new AuthenticatedUserDTO(authentication.getName(), fullName, roles);
    }
}
