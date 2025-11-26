package com.example.demo.DTOs.RegistrationDTOs;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RejectionRequest {

    @NotBlank(message = "Rejection reason cannot be empty")
    private String reason;
}