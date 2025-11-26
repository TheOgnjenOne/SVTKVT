package com.example.demo.Services.Impl;

import com.example.demo.DTOs.ReviewDTOs.ReviewResponseDTO;
import com.example.demo.DTOs.StatisticsDTO.EventRatingDTO;
import com.example.demo.DTOs.StatisticsDTO.LocationEventStatsDTO;
import com.example.demo.DTOs.StatisticsDTO.LocationRatingDTO;
import com.example.demo.Model.Event;
import com.example.demo.Model.Location;
import com.example.demo.Model.Review;
import com.example.demo.Repository.IEventRepository;
import com.example.demo.Repository.ILocationRepository;
import com.example.demo.Repository.IReviewRepository;
import com.example.demo.Services.ILocationAnalyticsService;
import com.example.demo.Services.ILocationManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class LocationAnalyticsServiceImpl implements ILocationAnalyticsService {

    private final ILocationManagerService locationManagerService;
    private final IEventRepository eventRepository;
    private final IReviewRepository reviewRepository;
    private final ILocationRepository locationRepository;
    private final ReviewServiceImpl reviewService;

    @Autowired
    public LocationAnalyticsServiceImpl(ILocationManagerService locationManagerService,
                                        IEventRepository eventRepository,
                                        IReviewRepository reviewRepository,
                                        ILocationRepository locationRepository,
                                        ReviewServiceImpl reviewService) {
        this.locationManagerService = locationManagerService;
        this.eventRepository = eventRepository;
        this.reviewRepository = reviewRepository;
        this.locationRepository = locationRepository;
        this.reviewService = reviewService;
    }


    @Override
    public List<LocationEventStatsDTO> getManagerEventAnalytics(String userEmail, LocalDateTime startDate, LocalDateTime endDate) {
        List<Long> managedLocationIds = locationManagerService.getManagedLocationIdsByEmail(userEmail);

        return managedLocationIds.stream()
                .map(locationId -> calculateEventStats(locationId, startDate, endDate))
                .collect(Collectors.toList());
    }

    @Override
    public List<LocationRatingDTO> getTopRatedLocations(String userEmail, int limit) {
        List<Long> managedLocationIds = locationManagerService.getManagedLocationIdsByEmail(userEmail);

        List<Location> locations = locationRepository.findByIdIn(managedLocationIds);

        return locations.stream()
                .map(loc -> new LocationRatingDTO(loc.getId(), loc.getName(), loc.getTotalRating()))
                .sorted(Comparator.comparing(LocationRatingDTO::getAverageRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<LocationRatingDTO> getLowestRatedLocations(String userEmail, int limit) {
        List<Long> managedLocationIds = locationManagerService.getManagedLocationIdsByEmail(userEmail);

        List<Location> locations = locationRepository.findByIdIn(managedLocationIds);

        return locations.stream()
                .map(loc -> new LocationRatingDTO(loc.getId(), loc.getName(), loc.getTotalRating()))
                .sorted(Comparator.comparing(LocationRatingDTO::getAverageRating, Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventRatingDTO> getTopRatedEvents(String userEmail, int limit) {
        List<Long> managedLocationIds = locationManagerService.getManagedLocationIdsByEmail(userEmail);

        List<Event> events = eventRepository.findByLocationIdIn(managedLocationIds);

        return events.stream()
                .map(event -> new EventRatingDTO(event.getId(), event.getName(), calculateEventAverageRating(event)))
                .filter(dto -> dto.getAverageRating() != null)
                .sorted(Comparator.comparing(EventRatingDTO::getAverageRating, Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventRatingDTO> getLowestRatedEvents(String userEmail, int limit) {
        List<Long> managedLocationIds = locationManagerService.getManagedLocationIdsByEmail(userEmail);

        List<Event> events = eventRepository.findByLocationIdIn(managedLocationIds);

        return events.stream()
                .map(event -> new EventRatingDTO(event.getId(), event.getName(), calculateEventAverageRating(event)))
                .filter(dto -> dto.getAverageRating() != null)
                .sorted(Comparator.comparing(EventRatingDTO::getAverageRating, Comparator.naturalOrder()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponseDTO> getLatestReviewsForTopLocation(String userEmail, int limit) {
        Optional<LocationRatingDTO> topLocationOpt = getTopRatedLocations(userEmail, 1).stream().findFirst();

        if (topLocationOpt.isEmpty()) {
            return List.of();
        }
        Long topLocationId = topLocationOpt.get().getLocationId();


        List<Review> reviews = reviewRepository.findByLocationIdAndDeletedFalseOrderByCreatedAtDesc(
                topLocationId,
                PageRequest.of(0, limit)
        );


        return reviews.stream()
                .map(reviewService::convertToReviewResponseDTO)
                .collect(Collectors.toList());
    }


    private LocationEventStatsDTO calculateEventStats(Long locationId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Event> events = eventRepository.findByLocationIdAndDateBetween(locationId, startDate, endDate);

        Location location = locationRepository.findById(locationId).orElse(new Location());

        Long total = (long) events.size();
        Long regular = events.stream().filter(Event::getRecurrent).count();
        Long irregular = total - regular;

        Long paid = events.stream()
                .filter(e -> e.getPrice() != null && e.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .count();

        Long free = total - paid;

        LocationEventStatsDTO dto = new LocationEventStatsDTO();
        dto.setLocationId(locationId);
        dto.setLocationName(location.getName() != null ? location.getName() : "Nepoznato Mesto");
        dto.setTotalEvents(total);
        dto.setRegularEvents(regular);
        dto.setIrregularEvents(irregular);
        dto.setPaidEvents(paid);
        dto.setFreeEvents(free);
        return dto;
    }

    private BigDecimal calculateEventAverageRating(Event event) {
        if (event.getReviews() == null || event.getReviews().isEmpty()) {
            return null;
        }

        long count = event.getReviews().size();

        if (count == 0) {
            return null;
        }

        double totalScore = event.getReviews().stream()
                .mapToInt(Review::getOverallRating)
                .sum();

        return BigDecimal.valueOf(totalScore)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}