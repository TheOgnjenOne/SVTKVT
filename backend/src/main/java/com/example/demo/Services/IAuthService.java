package com.example.demo.Services;

import com.example.demo.Model.User;

public interface IAuthService {
    User authenticate(String email, String password) throws Exception;
}
