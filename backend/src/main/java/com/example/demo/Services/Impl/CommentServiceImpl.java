package com.example.demo.Services.Impl;

import com.example.demo.DTOs.CommentDTOs.CommentRequestDTO;
import com.example.demo.DTOs.CommentDTOs.CommentResponseDTO;
import com.example.demo.DTOs.ReviewDTOs.ReviewResponseDTO;
import com.example.demo.Model.Comment;
import com.example.demo.Model.Review;
import com.example.demo.Model.User;
import com.example.demo.Repository.ICommentRepository;
import com.example.demo.Repository.IReviewRepository;
import com.example.demo.Repository.IUserRepository;
import com.example.demo.Services.ICommentService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements ICommentService {

    private final ICommentRepository commentRepository;
    private final IUserRepository userRepository;
    private final IReviewRepository reviewRepository;

    @Autowired
    public CommentServiceImpl(ICommentRepository commentRepository,
                              IUserRepository userRepository,
                              IReviewRepository reviewRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    @Override
    public ReviewResponseDTO addComment(CommentRequestDTO commentRequestDTO, Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Korisnik nije pronađen."));

        Review review = reviewRepository.findById(commentRequestDTO.getReviewId())
                .orElseThrow(() -> new EntityNotFoundException("Recenzija nije pronađena."));

        Comment parentComment = null;
        if (commentRequestDTO.getParentCommentId() != null) {
            parentComment = commentRepository.findById(commentRequestDTO.getParentCommentId())
                    .orElseThrow(() -> new EntityNotFoundException("Roditeljski komentar nije pronađen."));
        }

        Comment newComment = new Comment();
        newComment.setText(commentRequestDTO.getText());
        newComment.setUser(user);
        newComment.setReview(review);
        newComment.setParentComment(parentComment);
        newComment.setCreatedAt(LocalDateTime.now());

        commentRepository.save(newComment);

        Review refreshedReview = reviewRepository.findById(review.getId()).get();
        return convertToReviewResponseDTO(refreshedReview);
    }

    public List<CommentResponseDTO> mapCommentsToHierarchy(Set<Comment> allComments) {
        if (allComments == null || allComments.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<Comment>> groupedByParent = allComments.stream()
                .filter(c -> c.getParentComment() != null)
                .collect(Collectors.groupingBy(c -> c.getParentComment().getId()));

        List<Comment> topLevelComments = allComments.stream()
                .filter(c -> c.getParentComment() == null)
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .toList();

        return topLevelComments.stream()
                .map(comment -> mapCommentToDTO(comment, groupedByParent))
                .collect(Collectors.toList());
    }

    private CommentResponseDTO mapCommentToDTO(Comment comment, Map<Long, List<Comment>> groupedReplies) {
        CommentResponseDTO dto = new CommentResponseDTO();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUserEmail(comment.getUser().getEmail());
        dto.setUserId(comment.getUser().getId());

        String role = comment.getUser().getRole().name();
        dto.setIsManagerReply(role.equals("MANAGER") || role.equals("ADMIN"));

        if (comment.getParentComment() != null) {
            dto.setParentCommentId(comment.getParentComment().getId());
        }

        List<Comment> replies = groupedReplies.getOrDefault(comment.getId(), new ArrayList<>());
        if (!replies.isEmpty()) {
            replies.sort(Comparator.comparing(Comment::getCreatedAt));

            dto.setReplies(replies.stream()
                    .map(reply -> mapCommentToDTO(reply, groupedReplies))
                    .collect(Collectors.toList()));
        } else {
            dto.setReplies(new ArrayList<>());
        }

        return dto;
    }

    private ReviewResponseDTO convertToReviewResponseDTO(Review review) {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(review.getId());
        dto.setUserName(review.getUser().getEmail());
        dto.setSubmissionDate(review.getCreatedAt());
        dto.setCommentText(review.getCommentText());
        dto.setOverallRating(review.getOverallRating() != null ? (double) review.getOverallRating() : null);
        dto.setReviewedLocationId(review.getLocation().getId());
        dto.setReviewedEventId(review.getEvent().getId());

        dto.setIsHidden(review.isHidden());

        int calculatedEventCount = review.getEvent() != null ? 1 : 0;

        dto.setEventCount(calculatedEventCount);

        if (review.getComments() != null) {
            dto.setComments(mapCommentsToHierarchy(review.getComments()));
        }

        return dto;
    }
}