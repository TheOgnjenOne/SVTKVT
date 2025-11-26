package com.example.demo.Services.Impl;

import com.example.demo.DTOs.EventDTOs.EventRequestDTO;
import com.example.demo.DTOs.EventDTOs.EventResponseDTO;
import com.example.demo.Model.Event;
import com.example.demo.Model.Location;
import com.example.demo.Model.Image;
import com.example.demo.Repository.*;
import com.example.demo.Services.IEventService;
import com.example.demo.Services.IImageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; // Dodat import
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements IEventService {

    private final IEventRepository eventRepository;
    private final ILocationRepository locationRepository;
    private final IUserRepository userRepository;
    private final ILocationManagerRepository locationManagerRepository;
    private final IImageService imageService;

    @Autowired
    public EventServiceImpl(IEventRepository eventRepository, ILocationRepository locationRepository,
                            IUserRepository userRepository, ILocationManagerRepository locationManagerRepository,
                            IImageService imageService) {
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.locationManagerRepository = locationManagerRepository;
        this.imageService = imageService;
    }

    @Override
    public Event getById(Long id){
        Optional<Event> event = eventRepository.findById(id);
        return event.orElse(null);
    }
    private void checkManagerAuthorization(Long locationId, String userEmail) {
        Long userId = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found."))
                .getId();

        boolean isActiveManager = locationManagerRepository
                .findActiveManager(userId, locationId)
                .isPresent();

        if (!isActiveManager) {
            throw new SecurityException("User is not an active manager for location ID: " + locationId);
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority ->
                        grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    }


    private EventResponseDTO mapToDTO(Event event) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(event.getId());
        dto.setName(event.getName());
        dto.setAddress(event.getAddress());
        dto.setType(event.getType());
        dto.setDate(event.getDate());
        dto.setPrice(event.getPrice());
        dto.setRecurrent(event.getRecurrent());
        dto.setLocationId(event.getLocation().getId());
        dto.setLocationName(event.getLocation().getName());
        if (event.getImage() != null) {
            dto.setImage(imageService.mapToDTO(event.getImage()));
        }
        return dto;
    }


    @Override
    public List<EventResponseDTO> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventResponseDTO> getEventsByLocationId(Long locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new EntityNotFoundException("Location not found with ID: " + locationId);
        }
        return eventRepository.findByLocationId(locationId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }



    @Transactional(readOnly = true)
    @Override
    public List<EventResponseDTO> getPastEventsByLocationId(Long locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new EntityNotFoundException("Location not found with ID: " + locationId);
        }

        LocalDateTime now = LocalDateTime.now();

        return eventRepository.findByLocationIdAndDateBefore(locationId, now).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventResponseDTO> getFutureEventsByLocationId(Long locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new EntityNotFoundException("Location not found with ID: " + locationId);
        }

        LocalDateTime now = LocalDateTime.now();

        return eventRepository.findByLocationIdAndDateAfter(locationId, now).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public EventResponseDTO createEvent(EventRequestDTO eventDTO, MultipartFile imageFile, Authentication authentication) {
        Long locationId = Long.parseLong(eventDTO.getLocationId());
        String userEmail = authentication.getName();

        if (!isAdmin(authentication)) {
            checkManagerAuthorization(locationId, userEmail);
        }

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Location not found."));

        Image savedImage = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                savedImage = imageService.saveImageFile(imageFile);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save image file for the event.", e);
            }
        }

        Event event = new Event();
        event.setName(eventDTO.getName());
        event.setAddress(eventDTO.getAddress());
        event.setType(eventDTO.getType());
        event.setDate(eventDTO.getDate());
        event.setPrice(eventDTO.getPrice());
        event.setRecurrent(eventDTO.getRecurrent());
        event.setLocation(location);
        event.setImage(savedImage);

        Event savedEvent = eventRepository.save(event);
        return mapToDTO(savedEvent);
    }

    @Transactional
    @Override
    public EventResponseDTO updateEvent(Long eventId, EventRequestDTO eventDTO, MultipartFile imageFile, Authentication authentication) {
        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with ID: " + eventId));

        String userEmail = authentication.getName();
        Long locationId = eventDTO.getLocationId() != null ? Long.parseLong(eventDTO.getLocationId()) : existingEvent.getLocation().getId();

        if (!isAdmin(authentication)) {
            checkManagerAuthorization(locationId, userEmail);
        }

        if (eventDTO.getLocationId() != null && !eventDTO.getLocationId().isEmpty() && !locationId.equals(existingEvent.getLocation().getId())) {
            Location newLocation = locationRepository.findById(locationId)
                    .orElseThrow(() -> new EntityNotFoundException("New location not found."));
            existingEvent.setLocation(newLocation);
        }

        existingEvent.setName(eventDTO.getName());
        existingEvent.setAddress(eventDTO.getAddress());
        existingEvent.setType(eventDTO.getType());
        existingEvent.setDate(eventDTO.getDate());
        existingEvent.setPrice(eventDTO.getPrice());
        existingEvent.setRecurrent(eventDTO.getRecurrent());

        if (imageFile != null && !imageFile.isEmpty()) {
            if (existingEvent.getImage() != null) {
                imageService.deleteImage(existingEvent.getImage().getId());
            }

            try {
                Image newImage = imageService.saveImageFile(imageFile);
                existingEvent.setImage(newImage);
            } catch (Exception e) {
                throw new RuntimeException("Failed to update event image.", e);
            }
        }

        Event updatedEvent = eventRepository.save(existingEvent);
        return mapToDTO(updatedEvent);
    }

    @Transactional
    @Override
    public void deleteEvent(Long eventId, Authentication authentication) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with ID: " + eventId));

        String userEmail = authentication.getName();
        Long locationId = event.getLocation().getId();

        if (!isAdmin(authentication)) {
            checkManagerAuthorization(locationId, userEmail);
        }

        if (event.getImage() != null) {
            imageService.deleteImage(event.getImage().getId());
        }

        eventRepository.delete(event);
    }
}