package com.example.demo.Services;

import com.example.demo.DTOs.EventDTOs.EventRequestDTO;
import com.example.demo.DTOs.EventDTOs.EventResponseDTO;
import com.example.demo.Model.Event;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IEventService {

    Event getById(Long id);

    List<EventResponseDTO> getAllEvents();

    List<EventResponseDTO> getEventsByLocationId(Long locationId);


    @Transactional(readOnly = true)
    List<EventResponseDTO> getPastEventsByLocationId(Long locationId);

    @Transactional(readOnly = true)
    List<EventResponseDTO> getFutureEventsByLocationId(Long locationId);

    @Transactional
    EventResponseDTO createEvent(EventRequestDTO eventDTO, MultipartFile imageFile, Authentication authentication);


    @Transactional
    EventResponseDTO updateEvent(Long eventId, EventRequestDTO eventDTO, MultipartFile imageFile, Authentication authentication);

    @Transactional
    void deleteEvent(Long eventId, Authentication authentication);
}
