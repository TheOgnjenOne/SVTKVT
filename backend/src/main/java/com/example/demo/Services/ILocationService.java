package com.example.demo.Services;

import com.example.demo.DTOs.LocationDTOs.LocationListDTO;
import com.example.demo.Model.Location;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

public interface ILocationService {

    List<LocationListDTO> findAll(Authentication authentication);
    Location save(Location location);

    Optional<Location> findById(Long id);

    void delete(Long id);

    boolean isManagerOfLocation(Long userId, Long locationId);

    Optional<LocationListDTO> findByIdAsDTO(Long id, Authentication authentication);

    List<LocationListDTO> findTopRated(int limit, Authentication authentication);
}
