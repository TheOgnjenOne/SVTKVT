package com.example.demo.DTOs.UserDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequestDTO {
    private String name;
    private String phoneNumber;
    private LocalDate birthday;
    private String address;
    private String city;
}
