package com.example.demo.DTOs.EventDTOs;

import com.example.demo.DTOs.ImageDTOs.ImageResponseDTO;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class EventResponseDTO {
    private Long id;
    private String name;
    private String address;
    private String type;
    private LocalDateTime date;
    private BigDecimal price;
    private Boolean recurrent;
    private Long locationId;
    private String locationName;
    private ImageResponseDTO image;
}