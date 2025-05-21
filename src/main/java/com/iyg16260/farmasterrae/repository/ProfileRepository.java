package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    List<Profile> findAllByType(String type);

    Profile findByType(String type);
}
