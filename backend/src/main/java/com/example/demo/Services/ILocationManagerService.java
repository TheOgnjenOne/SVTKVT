package com.example.demo.Services;

import com.example.demo.DTOs.LocationDTOs.ManagedLocationDTO;
import com.example.demo.DTOs.ManagerDTOs.ManagerInfoDTO;
import com.example.demo.DTOs.UserDTOs.AvailableUserDTO;
import com.example.demo.Model.LocationManager;

import java.time.LocalDate;
import java.util.List;

public interface ILocationManagerService {

    void assignManager(Long userId, Long locationId, LocalDate startDate, LocalDate endDate);

    void unassignManager(Long userId, Long locationId);

    List<ManagerInfoDTO> getActiveManagersForLocation(Long locationId);

    List<AvailableUserDTO> getUsersAvailableForAssignment();

    List<LocationManager> findAllActiveByUserId(Long userId);

    List<Long> getManagedLocationIdsByEmail(String userEmail);

    ManagedLocationDTO getLocationDTOById(Long locationId);

    List<ManagedLocationDTO> getManagedLocationsByEmail(String userEmail);
}
