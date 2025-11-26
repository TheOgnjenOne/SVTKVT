package com.example.demo.DTOs.EventDTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class EventRequestDTO {

    private String name;
    private String address;
    private String type;
    private LocalDateTime date;
    private String locationId;

    private BigDecimal price;
    private Boolean recurrent = false;
}