package com.example.demo.DTOs.ManagerDTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ManagerInfoDTO {

    private Long userId;
    private String email;
    private LocalDate startDate;
    private LocalDate endDate;

    public ManagerInfoDTO(Long userId, String email, LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.email = email;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public ManagerInfoDTO() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}