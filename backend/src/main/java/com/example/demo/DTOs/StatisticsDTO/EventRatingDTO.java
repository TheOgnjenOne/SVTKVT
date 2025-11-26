package com.example.demo.DTOs.StatisticsDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventRatingDTO {
    private Long eventId;
    private String eventName;
    private BigDecimal averageRating;
}
