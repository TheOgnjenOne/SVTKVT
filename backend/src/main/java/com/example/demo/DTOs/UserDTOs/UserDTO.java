package com.example.demo.DTOs.UserDTOs;

import com.example.demo.Enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String name;
    private String phoneNumber;
    private LocalDate birthday;
    private String address;
    private String city;
    private UserRole role;
    private Long profileImageId;
}
