package com.example.demo.DTOs.ManagerDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManagerAssignmentRequest {

    @NotNull(message = "User ID must not be null")
    private Long userId;

    @NotNull(message = "Location ID must not be null")
    private Long locationId;

    @NotNull(message = "Start date must be provided")
    private LocalDate startDate;

    private LocalDate endDate;


}
