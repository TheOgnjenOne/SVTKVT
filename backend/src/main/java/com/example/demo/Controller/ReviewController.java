package com.example.demo.Controller;

import com.example.demo.DTOs.CommentDTOs.CommentRequestDTO;
import com.example.demo.DTOs.ReviewDTOs.ReviewRequestDTO;
import com.example.demo.DTOs.ReviewDTOs.ReviewResponseDTO;
import com.example.demo.Model.Event;
import com.example.demo.Services.ICommentService;
import com.example.demo.Services.IEventService;
import com.example.demo.Services.IReviewService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final IReviewService reviewService;
    private final IEventService eventService;
    private final ICommentService commentService;

    @Autowired
    public ReviewController(IReviewService reviewService, IEventService eventService, ICommentService commentService) {
        this.reviewService = reviewService;
        this.eventService = eventService;
        this.commentService = commentService;
    }

    @PostMapping("/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submitReview(
            @Valid @RequestBody ReviewRequestDTO reviewRequestDTO,
            Authentication authentication) {

        logger.info("Pokušaj podnošenja utiska (Review) od strane korisnika: {}", authentication.getName());

        try {
            Event event = eventService.getById(reviewRequestDTO.getEventId());
            if(event.getRecurrent() == false){
                String errorMessage = "Utisak se može ostaviti samo za redovan događaj.";
                logger.warn("Odbijen utisak za ne-redovan događaj ID: {}. Korisnik: {}", reviewRequestDTO.getEventId(), authentication.getName());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
            }
            ReviewResponseDTO submittedReview = reviewService.submitReview(reviewRequestDTO, authentication);
            logger.info("Utisak uspešno podnet. Review ID: {}, Lokacija ID: {}, Korisnik: {}", submittedReview.getId(), reviewRequestDTO.getLocationId(), authentication.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(submittedReview);

        } catch (IllegalArgumentException e) {
            logger.warn("Nevalidni argumenti prilikom podnošenja utiska: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Greška prilikom podnošenja utiska za korisnika {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<ReviewResponseDTO>> getAllReviewsByLocation(
            @PathVariable Long locationId,
            Authentication authentication) {
        logger.info("Dohvatanje svih utisaka za lokaciju ID: {}", locationId);
        try {
            List<ReviewResponseDTO> reviews = reviewService.getAllReviewsByLocationId(locationId, authentication);
            logger.info("Uspešno dohvaćeno {} utisaka za lokaciju ID: {}", reviews.size(), locationId);
            return ResponseEntity.ok(reviews);
        } catch (EntityNotFoundException e) {
            logger.warn("Pokušaj dohvatanja utisaka za nepostojeću lokaciju ID: {}", locationId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Greška prilikom dohvatanja utisaka za lokaciju ID {}: {}", locationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        logger.info("Pokušaj brisanja utiska ID: {}", reviewId);
        try {
            reviewService.deleteReview(reviewId);
            logger.info("Utisak ID {} uspešno obrisan.", reviewId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            logger.warn("Pokušaj brisanja nepostojećeg utiska ID: {}", reviewId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Greška prilikom brisanja utiska ID {}: {}", reviewId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PutMapping("/{reviewId}/visibility")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ReviewResponseDTO> toggleReviewVisibility(
            @PathVariable Long reviewId,
            @RequestBody ReviewResponseDTO requestDTO) {
        logger.info("Pokušaj promene vidljivosti utiska ID {} na: {}", reviewId, requestDTO.getIsHidden());
        try {
            ReviewResponseDTO updatedReview = reviewService.toggleReviewVisibility(reviewId, requestDTO.getIsHidden());
            logger.info("Vidljivost utiska ID {} uspešno promenjena. Nova vrednost: {}", reviewId, updatedReview.getIsHidden());
            return ResponseEntity.ok(updatedReview);
        } catch (EntityNotFoundException e) {
            logger.warn("Pokušaj promene vidljivosti nepostojećeg utiska ID: {}", reviewId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Greška prilikom promene vidljivosti utiska ID {}: {}", reviewId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/comment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponseDTO> addComment(
            @Valid @RequestBody CommentRequestDTO commentRequestDTO,
            Authentication authentication) {
        logger.info("Pokušaj dodavanja komentara na utisak ID {} od strane korisnika: {}", commentRequestDTO.getReviewId(), authentication.getName());
        try {
            ReviewResponseDTO updatedReview = commentService.addComment(commentRequestDTO, authentication);
            logger.info("Komentar uspešno dodat na utisak ID {}.", commentRequestDTO.getReviewId());
            return ResponseEntity.status(HttpStatus.CREATED).body(updatedReview);
        } catch (EntityNotFoundException e) {
            logger.warn("Pokušaj dodavanja komentara na nepostojeći utisak ID: {}", commentRequestDTO.getReviewId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Greška prilikom dodavanja komentara na utisak ID {}: {}", commentRequestDTO.getReviewId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}