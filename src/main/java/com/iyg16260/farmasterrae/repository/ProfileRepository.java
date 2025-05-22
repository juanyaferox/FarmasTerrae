package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Profile findByType(String type);
}
