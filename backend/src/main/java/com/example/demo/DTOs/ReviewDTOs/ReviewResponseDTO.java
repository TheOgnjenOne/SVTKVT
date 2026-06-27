package com.example.demo.DTOs.ReviewDTOs;

import com.example.demo.DTOs.CommentDTOs.CommentResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public class ReviewResponseDTO {

    private Long id;
    private String userName;
    private LocalDateTime submissionDate;
    private String commentText;
    private Double overallRating;

    // K5: pojedinačne ocene po kategorijama (potrebno i za prikaz i za UES pretragu po kategorijama)
    private Integer performanceRating;
    private Integer soundLightingRating;
    private Integer venueRating;

    private Long reviewedLocationId;
    private Long reviewedEventId;

    private Boolean isHidden = false;

    private Integer eventCount = 0;

    private List<CommentResponseDTO> comments;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public Double getOverallRating() {
        return overallRating;
    }

    public void setOverallRating(Double overallRating) {
        this.overallRating = overallRating;
    }

    public Integer getPerformanceRating() {
        return performanceRating;
    }

    public void setPerformanceRating(Integer performanceRating) {
        this.performanceRating = performanceRating;
    }

    public Integer getSoundLightingRating() {
        return soundLightingRating;
    }

    public void setSoundLightingRating(Integer soundLightingRating) {
        this.soundLightingRating = soundLightingRating;
    }

    public Integer getVenueRating() {
        return venueRating;
    }

    public void setVenueRating(Integer venueRating) {
        this.venueRating = venueRating;
    }

    public Long getReviewedLocationId() {
        return reviewedLocationId;
    }

    public void setReviewedLocationId(Long reviewedLocationId) {
        this.reviewedLocationId = reviewedLocationId;
    }

    public Long getReviewedEventId() {
        return reviewedEventId;
    }

    public void setReviewedEventId(Long reviewedEventId) {
        this.reviewedEventId = reviewedEventId;
    }


    public Boolean getIsHidden() {
        return isHidden;
    }

    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public List<CommentResponseDTO> getComments() {
        return comments;
    }

    public void setComments(List<CommentResponseDTO> comments) {
        this.comments = comments;
    }
}