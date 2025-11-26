package com.example.demo.Services;

import com.example.demo.DTOs.CommentDTOs.CommentRequestDTO;
import com.example.demo.DTOs.ReviewDTOs.ReviewResponseDTO;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

public interface ICommentService {
    @Transactional
    ReviewResponseDTO addComment(CommentRequestDTO commentRequestDTO, Authentication authentication);
}
