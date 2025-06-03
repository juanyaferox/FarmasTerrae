package com.iyg16260.farmasterrae.spec;

import com.iyg16260.farmasterrae.model.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
    public Specification<User> searchLike(String keyword) {
        return GenericSpecification.likeIgnoreCase(new String[]{
                "username", "fullName", "email", "phone",
                "address", "profile.type", "profile.description"}, keyword);
    }
}
