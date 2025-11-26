package com.example.demo.DTOs.UserDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailableUserDTO {
    private Long id;
    private String email;

}
