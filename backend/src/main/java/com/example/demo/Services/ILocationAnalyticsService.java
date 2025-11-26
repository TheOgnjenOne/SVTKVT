package com.example.demo.Services;

import com.example.demo.DTOs.ReviewDTOs.ReviewResponseDTO;
import com.example.demo.DTOs.StatisticsDTO.EventRatingDTO;
import com.example.demo.DTOs.StatisticsDTO.LocationEventStatsDTO;
import com.example.demo.DTOs.StatisticsDTO.LocationRatingDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface ILocationAnalyticsService {
    List<LocationEventStatsDTO> getManagerEventAnalytics(String userEmail, LocalDateTime startDate, LocalDateTime endDate);

    // DOHVAT TOP/LOW RATED LOKACIJA
    List<LocationRatingDTO> getTopRatedLocations(String userEmail, int limit);

    List<LocationRatingDTO> getLowestRatedLocations(String userEmail, int limit);

    // NOVO: Dohvaćanje Top Rated Događaja
    List<EventRatingDTO> getTopRatedEvents(String userEmail, int limit);

    List<EventRatingDTO> getLowestRatedEvents(String userEmail, int limit);

    // METODA ZA NAJSKORIJE UTISKE
    List<ReviewResponseDTO> getLatestReviewsForTopLocation(String userEmail, int limit);
}
