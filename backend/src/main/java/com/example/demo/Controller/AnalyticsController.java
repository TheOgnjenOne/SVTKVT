package com.example.demo.Controller;

import com.example.demo.DTOs.ReviewDTOs.ReviewResponseDTO;
import com.example.demo.DTOs.StatisticsDTO.EventRatingDTO;
import com.example.demo.DTOs.StatisticsDTO.LocationEventStatsDTO;
import com.example.demo.DTOs.StatisticsDTO.LocationRatingDTO;
import com.example.demo.Services.ILocationAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    private final ILocationAnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(ILocationAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/manager-data")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getManagerAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {

        String userEmail = authentication.getName();
        logger.info("User {} is requesting manager analytics for period {} to {}", userEmail, startDate, endDate);

        try {
            List<LocationEventStatsDTO> eventStats = analyticsService.getManagerEventAnalytics(userEmail, startDate, endDate);

            List<LocationRatingDTO> topRated = analyticsService.getTopRatedLocations(userEmail, 5);
            List<LocationRatingDTO> lowestRated = analyticsService.getLowestRatedLocations(userEmail, 5);

            List<ReviewResponseDTO> latestReviews = analyticsService.getLatestReviewsForTopLocation(userEmail, 3);

            List<EventRatingDTO> topRatedEvents = analyticsService.getTopRatedEvents(userEmail, 5);
            List<EventRatingDTO> lowestRatedEvents = analyticsService.getLowestRatedEvents(userEmail, 5);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("eventStatistics", eventStats);
            responseData.put("topRatedLocations", topRated);
            responseData.put("lowestRatedLocations", lowestRated);
            responseData.put("latestReviewsForTopLocation", latestReviews);
            responseData.put("topRatedEvents", topRatedEvents);
            responseData.put("lowestRatedEvents", lowestRatedEvents);

            logger.info("Successfully compiled and returning analytics data for user {}", userEmail);
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            logger.error("Error retrieving manager analytics for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }
}