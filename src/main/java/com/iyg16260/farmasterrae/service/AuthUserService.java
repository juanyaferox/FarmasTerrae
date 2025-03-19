package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.User;
import lombok.Data;
import org.springframework.stereotype.Service;

@Service
@Data
public class AuthUserService {
    private User authUser;
}
