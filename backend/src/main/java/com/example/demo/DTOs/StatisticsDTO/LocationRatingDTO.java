package com.example.demo.DTOs.StatisticsDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationRatingDTO {
    private Long locationId;
    private String locationName;
    private Double averageRating;
}
