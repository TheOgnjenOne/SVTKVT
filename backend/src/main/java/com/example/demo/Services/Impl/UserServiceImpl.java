package com.example.demo.Services.Impl;

import com.example.demo.DTOs.LocationDTOs.ManagedLocationDTO;
import com.example.demo.DTOs.ReviewDTOs.UserReviewDTO;
import com.example.demo.DTOs.UserDTOs.UserDTO;
import com.example.demo.DTOs.UserDTOs.UserUpdateRequestDTO;
import com.example.demo.Enums.UserRole;
import com.example.demo.Model.Image;
import com.example.demo.Model.User;
import com.example.demo.Repository.IUserRepository;
import com.example.demo.Services.IImageService;
import com.example.demo.Services.ILocationManagerService;
import com.example.demo.Services.IUserService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final ILocationManagerService locationManagerService;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final EmailService emailService;
    private final IImageService imageService;
    @Autowired
    @Lazy
    private  PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(IUserRepository userRepository, ILocationManagerService locationManagerService, EmailService emailService, IImageService imageService) {
        this.userRepository = userRepository;
        this.locationManagerService = locationManagerService;
        this.emailService = emailService;
        this.imageService = imageService;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findByEmail(username);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                )
        );
        return userDetails;
    }

    @Override
    public void save(User user){
        userRepository.save(user);
    }

    @Override
    public User registerNewUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalStateException("Error: Email is already in use!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(UserRole.USER);

        User savedUser = userRepository.save(user);

        logger.warn("Novi korisnik registrovan i čeka odobrenje: ID={}, Email={}", savedUser.getId(), savedUser.getEmail());

        return savedUser;
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Trenutna lozinka nije tačna!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailService.sendSimpleEmail(
                user.getEmail(),
                "Promena lozinke",
                "Vaša lozinka je uspešno promenjena!"
        );
    }

    @Override
    public UserDTO getUserProfile(String email) {
        User user = findByEmail(email);
        Long imageId = user.getProfileImage() != null ? user.getProfileImage().getId() : null;

        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                user.getBirthday(),
                user.getAddress(),
                user.getCity(),
                user.getRole(),
                imageId
        );
    }

    @Override
    @Transactional
    public void updateProfile(String email, UserUpdateRequestDTO request) {
        User user = findByEmail(email);

        user.setName(request.getName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setBirthday(request.getBirthday());
        user.setAddress(request.getAddress());
        user.setCity(request.getCity());

        userRepository.save(user);
    }

    @Override
    public List<UserReviewDTO> getUserReviews(String email) {
        User user = findByEmail(email);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

        return user.getReviews().stream()
                .map(review -> new UserReviewDTO(
                        review.getId(),
                        review.getLocation().getName(),
                        review.getOverallRating(),
                        review.getCommentText(),
                        review.getCreatedAt().format(formatter)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<ManagedLocationDTO> getManagedLocations(String email) {
        return locationManagerService.getManagedLocationsByEmail(email);
    }

    @Transactional
    @Override
    public void uploadProfileImage(String email, MultipartFile file) throws IOException {
        User user = findByEmail(email);

        Image newImage = imageService.saveImageFile(file);

        if (user.getProfileImage() != null) {
            Long oldImageId = user.getProfileImage().getId();

            user.setProfileImage(null);
            userRepository.save(user);
            imageService.deleteImage(oldImageId);
        }

        user.setProfileImage(newImage);
        userRepository.save(user);
    }
}
