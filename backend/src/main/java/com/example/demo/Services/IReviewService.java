package com.example.demo.Services;

import com.example.demo.DTOs.CommentDTOs.CommentRequestDTO;
import com.example.demo.DTOs.ReviewDTOs.ReviewRequestDTO;
import com.example.demo.DTOs.ReviewDTOs.ReviewResponseDTO;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IReviewService {

    ReviewResponseDTO submitReview(ReviewRequestDTO reviewRequestDTO, Authentication authentication);

    List<ReviewResponseDTO> getAllReviewsByLocationId(Long locationId, Authentication authentication);
    @Transactional
    void deleteReview(Long reviewId);

    @Transactional
    ReviewResponseDTO toggleReviewVisibility(Long reviewId, boolean isHidden);


   }
