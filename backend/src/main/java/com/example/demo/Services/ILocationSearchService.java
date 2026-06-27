package com.example.demo.Services;

import com.example.demo.DTOs.SearchDTOs.LocationSearchRequestDTO;
import com.example.demo.DTOs.SearchDTOs.LocationSearchResponseDTO;

/**
 * UES: pretraga mesta nad Elasticsearch indeksom (ocene 6–10).
 */
public interface ILocationSearchService {

    /** Glavna pretraga: tekstualna polja, range-ovi, AND/OR, sortiranje, highlight. */
    LocationSearchResponseDTO search(LocationSearchRequestDTO request);

    /** MoreLikeThis (ocena 9): slična mesta na osnovu naziva, opisa i pdfOpis-a. */
    LocationSearchResponseDTO moreLikeThis(Long locationId, int size);
}
