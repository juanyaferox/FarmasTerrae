package com.iyg16260.farmasterrae.dto.user;

import com.iyg16260.farmasterrae.model.Profile;

/**
 * Perfil (tipo de usuario)
 *
 * @param id          id del perfil
 * @param type        tipo
 * @param description descripción
 */
public record ProfileDTO(Long id, String type, String description) {

    public static ProfileDTO from(Profile profile) {
        return new ProfileDTO(profile.getId(), profile.getType(), profile.getDescription());
    }
}
