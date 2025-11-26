package com.example.demo.Services.Impl;

import com.example.demo.DTOs.LocationDTOs.ManagedLocationDTO;
import com.example.demo.DTOs.ManagerDTOs.ManagerInfoDTO;
import com.example.demo.DTOs.UserDTOs.AvailableUserDTO;
import com.example.demo.Enums.UserRole;
import com.example.demo.Model.Location;
import com.example.demo.Model.LocationManager;
import com.example.demo.Model.LocationManagerId;
import com.example.demo.Model.User;
import com.example.demo.Repository.ILocationManagerRepository;
import com.example.demo.Repository.ILocationRepository;
import com.example.demo.Repository.IUserRepository;
import com.example.demo.Services.ILocationManagerService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LocationManagerServiceImpl implements ILocationManagerService {

    private final ILocationManagerRepository locationManagerRepository;
    private final IUserRepository userRepository;
    private final ILocationRepository locationRepository;

    @Autowired
    public LocationManagerServiceImpl(ILocationManagerRepository locationManagerRepository, IUserRepository userRepository, ILocationRepository locationRepository) {
        this.locationManagerRepository = locationManagerRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public void assignManager(Long userId, Long locationId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalStateException("Start date cannot be in the past.");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalStateException("End date cannot be before start date.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Location not found with ID: " + locationId));

        Optional<LocationManager> existingActive = locationManagerRepository.findActiveManager(userId, locationId);
        if (existingActive.isPresent()) {
            throw new IllegalStateException("User is already an active manager for this location.");
        }

        if (user.getRole() == UserRole.USER) {
            user.setRole(UserRole.MANAGER);
            userRepository.save(user);
        }

        LocationManagerId id = new LocationManagerId(locationId, userId);
        LocationManager newAssignment = new LocationManager();

        newAssignment.setId(id);
        newAssignment.setLocation(location);
        newAssignment.setUser(user);
        newAssignment.setStartDate(startDate);
        newAssignment.setEndDate(endDate);

        locationManagerRepository.save(newAssignment);
    }

    @Override
    public void unassignManager(Long userId, Long locationId) {
        LocationManager activeAssignment = locationManagerRepository.findActiveManager(userId, locationId)
                .orElseThrow(() -> new IllegalStateException("Active manager assignment not found for this user and location."));
        activeAssignment.setEndDate(LocalDate.now().minusDays(1));
        locationManagerRepository.save(activeAssignment);
    }

    @Override
    public List<ManagerInfoDTO> getActiveManagersForLocation(Long locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new EntityNotFoundException("Location not found with ID: " + locationId);
        }

        List<LocationManager> activeManagers = locationManagerRepository.findAllActiveByLocationId(locationId);

        return activeManagers.stream()
                .map(assignment -> new ManagerInfoDTO(
                        assignment.getUser().getId(),
                        assignment.getUser().getEmail(),
                        assignment.getStartDate(),
                        assignment.getEndDate()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<AvailableUserDTO> getUsersAvailableForAssignment() {
        List<User> allUsers = userRepository.findAll();

        return allUsers.stream()
                .map(user -> new AvailableUserDTO(
                        user.getId(),
                        user.getEmail()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<LocationManager> findAllActiveByUserId(Long userId){
        return locationManagerRepository.findAllActiveByUserId(userId);
    }

    @Override
    public List<Long> getManagedLocationIdsByEmail(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userEmail));

        List<LocationManager> activeAssignments = locationManagerRepository.findAllActiveByUserId(user.getId());

        return activeAssignments.stream()
                .map(lm -> lm.getLocation().getId())
                .collect(Collectors.toList());
    }

    @Override
    public ManagedLocationDTO getLocationDTOById(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Location not found with ID: " + locationId));
        return new ManagedLocationDTO(location.getId(), location.getName());
    }

    @Override
    public List<ManagedLocationDTO> getManagedLocationsByEmail(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userEmail));

        List<LocationManager> activeAssignments = locationManagerRepository.findAllActiveByUserId(user.getId());

        return activeAssignments.stream()
                .map(lm -> new ManagedLocationDTO(
                        lm.getLocation().getId(),
                        lm.getLocation().getName()
                ))
                .collect(Collectors.toList());
    }
}