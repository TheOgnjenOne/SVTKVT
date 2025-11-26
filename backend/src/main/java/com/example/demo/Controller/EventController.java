package com.example.demo.Controller;

import com.example.demo.DTOs.EventDTOs.EventRequestDTO;
import com.example.demo.DTOs.EventDTOs.EventResponseDTO;
import com.example.demo.Services.IEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    private final IEventService eventService;
    private final ObjectMapper objectMapper;

    @Autowired
    public EventController(IEventService eventService, ObjectMapper objectMapper) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }


    @GetMapping
    public ResponseEntity<List<EventResponseDTO>> getAllEvents() {
        logger.info("Fetching all events.");
        return ResponseEntity.ok(eventService.getAllEvents());
    }


    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<EventResponseDTO>> getEventsByLocation(@PathVariable Long locationId) {
        try {
            logger.info("Fetching all events for location ID: {}", locationId);
            return ResponseEntity.ok(eventService.getEventsByLocationId(locationId));
        } catch (EntityNotFoundException e) {
            logger.warn("Location not found when trying to fetch events for ID: {}", locationId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/location/future/{locationId}")
    public ResponseEntity<List<EventResponseDTO>> getFutureEventsByLocationId(@PathVariable Long locationId) {
        logger.info("Fetching future events for location ID: {}", locationId);
        List<EventResponseDTO> events = eventService.getFutureEventsByLocationId(locationId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/location/past/{locationId}")
    public ResponseEntity<List<EventResponseDTO>> getPastEventsByLocationId(@PathVariable Long locationId) {
        logger.info("Fetching past events for location ID: {}", locationId);
        try {
            List<EventResponseDTO> events = eventService.getPastEventsByLocationId(locationId);
            return ResponseEntity.ok(events);
        } catch (EntityNotFoundException e) {
            logger.warn("Location not found when trying to fetch past events for ID: {}", locationId);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<EventResponseDTO> createEvent(
            @RequestParam("event") String eventJson,
            @RequestParam("image") MultipartFile imageFile,
            Authentication authentication) {

        logger.info("Attempting to create a new event. User: {}", authentication.getName());
        logger.debug("RAW Event JSON: {}", eventJson);

        try {
            EventRequestDTO eventDTO = objectMapper.readValue(eventJson, EventRequestDTO.class);
            logger.debug("Parsed Event DTO. Name: {}, Location ID: {}", eventDTO.getName(), eventDTO.getLocationId());

            EventResponseDTO createdEvent = eventService.createEvent(eventDTO, imageFile, authentication);
            logger.info("Event successfully created. Event ID: {}, Location ID: {}", createdEvent.getId(), createdEvent.getLocationId());

            return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);

        } catch (JsonProcessingException e) {
            logger.error("JSON Parsing Error during event creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (SecurityException e) {
            logger.warn("Security error: User {} unauthorized to create event.", authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("General error during event creation by user {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponseDTO> updateEvent(
            @PathVariable Long eventId,
            @RequestParam("event") String eventJson,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            Authentication authentication) {

        logger.info("Attempting to update event ID: {}. User: {}", eventId, authentication.getName());

        try {
            EventRequestDTO eventDTO = objectMapper.readValue(eventJson, EventRequestDTO.class);

            EventResponseDTO updatedEvent = eventService.updateEvent(eventId, eventDTO, imageFile, authentication);

            logger.info("Event ID {} successfully updated.", eventId);
            return ResponseEntity.ok(updatedEvent);

        } catch (SecurityException e) {
            logger.warn("Security error: User {} unauthorized to update event ID {}.", authentication.getName(), eventId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (EntityNotFoundException e) {
            logger.warn("Attempt to update non-existent event ID: {}", eventId);
            return ResponseEntity.notFound().build();
        } catch (JsonProcessingException e) {
            logger.error("JSON Parsing Error during event update for ID {}: {}", eventId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("General error during event update for ID {}: {}", eventId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId, Authentication authentication) {
        logger.info("Attempting to delete event ID: {}. User: {}", eventId, authentication.getName());
        try {
            eventService.deleteEvent(eventId, authentication);
            logger.info("Event ID {} successfully deleted.", eventId);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            logger.warn("Security error: User {} unauthorized to delete event ID {}.", authentication.getName(), eventId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (EntityNotFoundException e) {
            logger.warn("Attempt to delete non-existent event ID: {}", eventId);
            return ResponseEntity.notFound().build();
        }
    }
}