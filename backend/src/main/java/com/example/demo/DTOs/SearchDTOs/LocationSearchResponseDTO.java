package com.example.demo.DTOs.SearchDTOs;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * UES: omotač odgovora pretrage (ukupan broj pogodaka + lista rezultata).
 */
@Getter
@Setter
public class LocationSearchResponseDTO {

    private long total;
    private List<LocationSearchResultDTO> results;

    public LocationSearchResponseDTO() {
    }

    public LocationSearchResponseDTO(long total, List<LocationSearchResultDTO> results) {
        this.total = total;
        this.results = results;
    }
}
