package com.example.demo.DTOs.CommentDTOs;

import java.time.LocalDateTime;
import java.util.List;

public class CommentResponseDTO {

    private Long id;
    private String text;
    private LocalDateTime createdAt;
    private String userEmail;
    private Long userId;
    private Long parentCommentId;
    private Boolean isManagerReply;
    private List<CommentResponseDTO> replies;

    // ------------------- Getters and Setters -------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Long parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public Boolean getIsManagerReply() {
        return isManagerReply;
    }

    public void setIsManagerReply(Boolean isManagerReply) {
        this.isManagerReply = isManagerReply;
    }

    public List<CommentResponseDTO> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentResponseDTO> replies) {
        this.replies = replies;
    }
}