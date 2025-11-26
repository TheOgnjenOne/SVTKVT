package com.example.demo.Services.Impl;


import com.example.demo.DTOs.ReviewDTOs.ReviewRequestDTO;
import com.example.demo.DTOs.ReviewDTOs.ReviewResponseDTO;
import com.example.demo.Model.*;
import com.example.demo.Repository.*;
import com.example.demo.Services.IReviewService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements IReviewService {

    private final IReviewRepository reviewRepository;
    private final IUserRepository userRepository;
    private final ILocationRepository locationRepository;
    private final IEventRepository eventRepository;
    private final CommentServiceImpl commentService;

    @Autowired
    public ReviewServiceImpl(IReviewRepository reviewRepository,
                             IUserRepository userRepository,
                             ILocationRepository locationRepository,
                             IEventRepository eventRepository,
                             ICommentRepository commentRepository,
                             CommentServiceImpl commentService) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.eventRepository = eventRepository;
        this.commentService = commentService;
    }

    @Override
    @Transactional
    public ReviewResponseDTO submitReview(ReviewRequestDTO reviewRequestDTO, Authentication authentication) {
        Long locationId = reviewRequestDTO.getLocationId();
        Long eventId = reviewRequestDTO.getEventId();

        if (locationId == null) {
            throw new IllegalArgumentException("ID Lokacije je obavezan, jer se utisak ostavlja na MESTO.");
        }
        if (eventId == null) {
            throw new IllegalArgumentException("ID Događaja je obavezan, jer se utisak ostavlja na OSNOVU DOGAĐAJA.");
        }

        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen."));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Događaj ID " + eventId + " nije pronađen."));

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Lokacija ID " + locationId + " nije pronađena."));


        if (!event.getLocation().getId().equals(locationId)) {
            throw new IllegalArgumentException("Odabrani događaj se nije održao na odabranoj lokaciji.");
        }

        if (!event.getRecurrent()) {
            throw new IllegalArgumentException("Utisak se može ostaviti samo za redovan događaj.");
        }

        if (event.getDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Utisak se može ostaviti samo za događaj koji se već održao.");
        }

        if (reviewRepository.existsByUserIdAndEventId(user.getId(), event.getId())) {
            throw new IllegalArgumentException("Već ste ostavili utisak za ovaj događaj. Utisak se na mestu vezuje za događaj.");
        }

        Review review = new Review();
        review.setUser(user);
        review.setLocation(location);
        review.setEvent(event);
        review.setCreatedAt(LocalDateTime.now());
        review.setCommentText(reviewRequestDTO.getCommentText());
        review.setPerformanceRating(reviewRequestDTO.getPerformanceRating());
        review.setSoundLightingRating(reviewRequestDTO.getSoundLightingRating());
        review.setVenueRating(reviewRequestDTO.getVenueRating());
        review.setOverallRating(reviewRequestDTO.getOverallRating());

        int eventCount = calculateEventCount(event);
        review.setEventCount(eventCount);

        Review savedReview = reviewRepository.save(review);

        Double newAverageRating = reviewRepository.calculateAverageOverallRatingByLocationId(locationId);
        if (newAverageRating != null) {
            location.setTotalRating(newAverageRating);
            locationRepository.save(location);
            locationRepository.flush();
        }

        ReviewResponseDTO responseDTO = new ReviewResponseDTO();
        responseDTO.setId(savedReview.getId());
        responseDTO.setUserName(savedReview.getUser().getEmail());
        responseDTO.setSubmissionDate(savedReview.getCreatedAt());
        responseDTO.setCommentText(savedReview.getCommentText());
        responseDTO.setOverallRating(savedReview.getOverallRating() != null ? (double) savedReview.getOverallRating() : null);
        responseDTO.setReviewedLocationId(savedReview.getLocation().getId());
        responseDTO.setReviewedEventId(savedReview.getEvent().getId());
        responseDTO.setEventCount(savedReview.getEventCount());

        return responseDTO;
    }

    @Override
    public List<ReviewResponseDTO> getAllReviewsByLocationId(Long locationId, Authentication authentication) {
        locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Lokacija ID " + locationId + " nije pronađena."));

        List<Review> reviews = reviewRepository.findByLocationId(locationId);

        boolean isManagerOrAdmin = authentication != null &&
                (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")) ||
                        authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        if (!isManagerOrAdmin) {
            reviews = reviews.stream()
                    .filter(r -> !r.isHidden())
                    .toList();
        }

        return reviews.stream()
                .map(this::convertToReviewResponseDTO)
                .collect(Collectors.toList());
    }

    private int calculateEventCount(Event event) {
        if (!event.getRecurrent()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = event.getDate();

        if (now.isBefore(startDate)) {
            return 0;
        }
        long daysBetween = ChronoUnit.DAYS.between(startDate.toLocalDate(), now.toLocalDate());
        int count = (int) (daysBetween / 7) + 1;

        return count;
    }
    @Transactional
    @Override
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Recenzija ID " + reviewId + " nije pronađena."));

        Long locationId = review.getLocation().getId();

        review.setDeleted(true);
        reviewRepository.save(review);
        reviewRepository.flush();

        recalculateLocationRating(locationId);
    }


    @Transactional
    @Override
    public ReviewResponseDTO toggleReviewVisibility(Long reviewId, boolean isHidden) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Recenzija ID " + reviewId + " nije pronađena."));

        review.setHidden(isHidden);
        Review updatedReview = reviewRepository.save(review);

        recalculateLocationRating(review.getLocation().getId());

        return convertToReviewResponseDTO(updatedReview);
    }


    private void recalculateLocationRating(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Lokacija ID " + locationId + " nije pronađena."));

        Double newAverageRating = reviewRepository.calculateAverageOverallRatingByLocationId(locationId);

        location.setTotalRating(newAverageRating != null ? newAverageRating : 0.0);
        locationRepository.save(location);
        locationRepository.flush();


    }

    ReviewResponseDTO convertToReviewResponseDTO(Review review) {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(review.getId());
        dto.setUserName(review.getUser().getEmail());
        dto.setSubmissionDate(review.getCreatedAt());
        dto.setCommentText(review.getCommentText());
        dto.setOverallRating(review.getOverallRating() != null ? (double) review.getOverallRating() : null);
        dto.setReviewedLocationId(review.getLocation().getId());
        dto.setReviewedEventId(review.getEvent().getId());

        dto.setIsHidden(review.isHidden());

        int calculatedEventCount = calculateEventCount(review.getEvent());
        dto.setEventCount(calculatedEventCount);

        if (review.getComments() != null) {
            dto.setComments(commentService.mapCommentsToHierarchy(review.getComments()));
        }

        return dto;
    }
}