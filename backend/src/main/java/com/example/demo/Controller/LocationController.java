package com.example.demo.Controller;

import com.example.demo.DTOs.LocationDTOs.LocationListDTO;
import com.example.demo.Model.Image;
import com.example.demo.Model.Location;
import com.example.demo.Model.User;
import com.example.demo.Services.IImageService;
import com.example.demo.Services.ILocationManagerService;
import com.example.demo.Services.ILocationService;
import com.example.demo.Services.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    private final ILocationService locationService;
    private final ILocationManagerService locationManagerService;
    private final IImageService imageService;
    private final IUserService userService;

    public LocationController(ILocationService locationService, ILocationManagerService locationManagerService, IImageService imageService, IUserService userService) {
        this.locationService = locationService;
        this.locationManagerService = locationManagerService;
        this.imageService = imageService;
        this.userService = userService;
    }

    @GetMapping()
    public ResponseEntity<List<LocationListDTO>> getAllLocations(Authentication authentication) {
        logger.info("User {} requested all locations.", authentication != null ? authentication.getName() : "Anonymous");
        List<LocationListDTO> locations = locationService.findAll(authentication);
        return  ResponseEntity.ok(locations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationListDTO> getLocationById(@PathVariable Long id,Authentication authentication) {
        logger.info("User {} requested location ID: {}", authentication != null ? authentication.getName() : "Anonymous", id);
        Optional<LocationListDTO> locationDTOOptional = locationService.findByIdAsDTO(id,authentication);

        return locationDTOOptional
                .map(locationDTO -> {
                    logger.info("Successfully retrieved location ID: {}", id);
                    return ResponseEntity.ok(locationDTO);
                })
                .orElseGet(() -> {
                    logger.warn("Location ID {} not found.", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping(value = "create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Location> createLocation(
            @RequestParam("location") String locationJson,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) throws IOException {

        logger.info("Admin attempting to create a new location.");
        ObjectMapper mapper = new ObjectMapper();
        Location location = mapper.readValue(locationJson, Location.class);

        Image uploadedImage = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            uploadedImage = imageService.saveImageFile(imageFile);
            logger.info("Image uploaded for the new location.");
        }

        if (uploadedImage != null) {
            location.setImage(uploadedImage);
        } else {
            location.setImage(imageService.getImageById(1L).orElse(null));
            logger.info("No image provided, setting default image for new location.");
        }

        Location saved = locationService.save(location);
        logger.info("Location successfully created with ID: {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @PutMapping(value = "{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Location> updateLocation(
            @PathVariable Long id,
            @RequestPart("location") String locationJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            Authentication authentication) throws IOException {

        String userEmail = authentication.getName();
        User user = userService.findByEmail(userEmail);
        Long currentUserId = user.getId();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        logger.info("User {} attempting to update location ID: {}", userEmail, id);

        if (!isAdmin) {
            if (!locationService.isManagerOfLocation(currentUserId, id)) {
                logger.warn("Manager {} forbidden to update location ID {} (not assigned).", userEmail, id);
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        }

        Location existingLocation = locationService.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Attempt to update non-existent location ID: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found with ID: " + id);
                });

        ObjectMapper mapper = new ObjectMapper();
        Location updatedDetails = mapper.readValue(locationJson, Location.class);

        existingLocation.setName(updatedDetails.getName());
        existingLocation.setAddress(updatedDetails.getAddress());
        existingLocation.setType(updatedDetails.getType());
        existingLocation.setDescription(updatedDetails.getDescription());

        if (imageFile != null && !imageFile.isEmpty()) {
            Image newImage = imageService.saveImageFile(imageFile);
            existingLocation.setImage(newImage);
            logger.info("New image uploaded and set for location ID: {}", id);
        }

        Location saved = locationService.save(existingLocation);
        logger.info("Location ID {} successfully updated by user {}.", id, userEmail);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        logger.info("Admin attempting to delete location ID: {}", id);

        if (locationService.findById(id).isEmpty()) {
            logger.warn("Attempt to delete non-existent location ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        locationService.delete(id);
        logger.info("Location ID {} successfully deleted.", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-managed-ids")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<Long>> getMyManagedLocationIds(Authentication authentication) {
        String userEmail = authentication.getName();

        List<Long> managedIds = locationManagerService.getManagedLocationIdsByEmail(userEmail);

        logger.info("User {} retrieved {} managed location IDs.", userEmail, managedIds.size());
        return ResponseEntity.ok(managedIds);
    }

    @GetMapping("/top")
    public ResponseEntity<List<LocationListDTO>> getTopLocations(@RequestParam(defaultValue = "3") int limit, Authentication authentication) {
        logger.info("User {} requested top {} rated locations.", authentication != null ? authentication.getName() : "Anonymous", limit);
        List<LocationListDTO> topLocations = locationService.findTopRated(limit, authentication);
        return ResponseEntity.ok(topLocations);
    }
}