package com.example.demo.DTOs.ReviewDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserReviewDTO {
    private Long id;
    private String locationName;
    private int rating;
    private String text;
    private String reviewDate;
}
