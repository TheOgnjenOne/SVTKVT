package com.example.demo.Services.Impl;

import com.example.demo.DTOs.ImageDTOs.ImageResponseDTO;
import com.example.demo.DTOs.LocationDTOs.LocationListDTO;
import com.example.demo.Model.Image;
import com.example.demo.Model.Location;
import com.example.demo.Repository.ILocationManagerRepository;
import com.example.demo.Repository.ILocationRepository;
import com.example.demo.Repository.IReviewRepository;
import com.example.demo.Services.ILocationIndexService;
import com.example.demo.Services.ILocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LocationServiceImpl implements ILocationService {

    private final ILocationRepository locationRepository;
    private final ILocationManagerRepository locationManagerRepository;
    private final IReviewRepository reviewRepository;
    private final ILocationIndexService locationIndexService;

    @Autowired
    public LocationServiceImpl(ILocationRepository locationRepository, ILocationManagerRepository locationManagerRepository, IReviewRepository reviewRepository, ILocationIndexService locationIndexService) {
        this.locationRepository = locationRepository;
        this.locationManagerRepository = locationManagerRepository;
        this.reviewRepository = reviewRepository;
        this.locationIndexService = locationIndexService;
    }

    @Override
    public List<LocationListDTO> findAll(Authentication authentication) {
        List<Location> locations = locationRepository.findAll();

        boolean isManagerOrAdmin = authentication != null &&
                (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")) ||
                        authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        return locations.stream()
                .map(loc -> {
                    Long reviewCount;

                    if (isManagerOrAdmin) {
                        reviewCount = reviewRepository.countByLocationIdAndDeletedFalse(loc.getId());
                    } else {
                        reviewCount = reviewRepository.countActiveAndVisibleByLocationId(loc.getId());
                    }

                    ImageResponseDTO imageDto = null;
                    Image imageEntity = loc.getImage();

                    if (imageEntity != null) {
                        imageDto = new ImageResponseDTO(
                                imageEntity.getId(),
                                imageEntity.getPath()
                        );
                    }

                    return new LocationListDTO(
                            loc.getId(),
                            loc.getName(),
                            loc.getAddress(),
                            loc.getType(),
                            loc.getDescription(),
                            loc.getTotalRating(),
                            imageDto,
                            reviewCount,
                            loc.getPdfKey() != null && !loc.getPdfKey().isBlank()
                    );
                })
                .collect(Collectors.toList());
    }
    @Override
    public Location save(Location location) {
        Location saved = locationRepository.save(location);
        // UES: osveži ES dokument pri kreiranju/izmeni mesta
        locationIndexService.indexLocation(saved);
        return saved;
    }

    @Override
    public Optional<Location> findById(Long id){
        return locationRepository.findById(id);
    }

    @Override
    public void delete(Long id){
        locationRepository.deleteById(id);
        // UES: ukloni mesto iz ES indeksa
        locationIndexService.deleteFromIndex(id);
    }

    @Override
    public boolean isManagerOfLocation(Long userId, Long locationId){
        return locationManagerRepository.findActiveManager(userId, locationId).isPresent();
    }

    @Override
    public Optional<LocationListDTO> findByIdAsDTO(Long id, Authentication authentication) {
        Optional<Location> locationOptional = locationRepository.findById(id);

        if (locationOptional.isEmpty()) {
            return Optional.empty();
        }

        Location loc = locationOptional.get();

        boolean isManagerOrAdmin = authentication != null &&
                (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")) ||
                        authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        Long reviewCount;
        if (isManagerOrAdmin) {
            reviewCount = reviewRepository.countByLocationIdAndDeletedFalse(loc.getId());
        } else {
            reviewCount = reviewRepository.countActiveAndVisibleByLocationId(loc.getId());
        }

        ImageResponseDTO imageDto = null;
        Image imageEntity = loc.getImage();
        if (imageEntity != null) {
            imageDto = new ImageResponseDTO(
                    imageEntity.getId(),
                    imageEntity.getPath()
            );
        }

        LocationListDTO dto = new LocationListDTO(
                loc.getId(),
                loc.getName(),
                loc.getAddress(),
                loc.getType(),
                loc.getDescription(),
                loc.getTotalRating(),
                imageDto,
                reviewCount,
                loc.getPdfKey() != null && !loc.getPdfKey().isBlank()
        );

        return Optional.of(dto);
    }

    @Override
    public List<LocationListDTO> findTopRated(int limit, Authentication authentication) {
        List<Location> allLocations = locationRepository.findAll();

        List<Location> sortedLocations = allLocations.stream()
                .sorted((loc1, loc2) -> Double.compare(loc2.getTotalRating(), loc1.getTotalRating()))
                .limit(limit)
                .collect(Collectors.toList());

        boolean isManagerOrAdmin = authentication != null &&
                (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")) ||
                        authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        return sortedLocations.stream()
                .map(loc -> {
                    Long reviewCount;

                    if (isManagerOrAdmin) {
                        reviewCount = reviewRepository.countByLocationIdAndDeletedFalse(loc.getId());
                    } else {
                        reviewCount = reviewRepository.countActiveAndVisibleByLocationId(loc.getId());
                    }

                    ImageResponseDTO imageDto = null;
                    Image imageEntity = loc.getImage();

                    if (imageEntity != null) {
                        imageDto = new ImageResponseDTO(
                                imageEntity.getId(),
                                imageEntity.getPath()
                        );
                    }

                    return new LocationListDTO(
                            loc.getId(),
                            loc.getName(),
                            loc.getAddress(),
                            loc.getType(),
                            loc.getDescription(),
                            loc.getTotalRating(),
                            imageDto,
                            reviewCount,
                            loc.getPdfKey() != null && !loc.getPdfKey().isBlank()
                    );
                })
                .collect(Collectors.toList());
    }
}
