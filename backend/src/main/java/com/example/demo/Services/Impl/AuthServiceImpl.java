package com.example.demo.Services.Impl;

import com.example.demo.Model.User;
import com.example.demo.Services.IAuthService;
import com.example.demo.Services.IUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements IAuthService {
    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(IUserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public User authenticate(String email, String password) throws Exception {
        User user = userService.findByEmail(email);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new Exception("Netacni email ili sifra!");
        }
        return user;
    }
}
