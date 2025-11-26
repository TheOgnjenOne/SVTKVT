package com.example.demo.DTOs.LocationDTOs;

import java.time.LocalDateTime;

import com.example.demo.DTOs.ImageDTOs.ImageResponseDTO;
import com.example.demo.Model.Image;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationListDTO {

    private Long id;
    private String name;
    private String address;
    private String type;
    private String description;
    private Double totalRating;
    private ImageResponseDTO image;
    private Long reviewCount;


}