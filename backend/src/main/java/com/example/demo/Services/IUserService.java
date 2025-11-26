package com.example.demo.Services;

import com.example.demo.DTOs.LocationDTOs.ManagedLocationDTO;
import com.example.demo.DTOs.ReviewDTOs.UserReviewDTO;
import com.example.demo.DTOs.UserDTOs.UserDTO;
import com.example.demo.DTOs.UserDTOs.UserUpdateRequestDTO;
import com.example.demo.Model.User;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IUserService extends UserDetailsService {
    void save(User user);

    User registerNewUser(User user);

    User findById(Long id);

    User findByEmail(String email);

    List<User> findAll();

    void changePassword(User user, String currentPassword, String newPassword);

    UserDTO getUserProfile(String email);
    void updateProfile(String email, UserUpdateRequestDTO request);
    List<UserReviewDTO> getUserReviews(String email);

    List<ManagedLocationDTO> getManagedLocations(String email);

    @Transactional
    void uploadProfileImage(String email, MultipartFile file) throws IOException;
}