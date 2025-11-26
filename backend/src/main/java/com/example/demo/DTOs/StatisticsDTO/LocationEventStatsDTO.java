package com.example.demo.DTOs.StatisticsDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationEventStatsDTO {
    private Long locationId;
    private String locationName;
    private Long totalEvents;
    private Long regularEvents;
    private Long irregularEvents;
    private Long paidEvents;
    private Long freeEvents;
}
