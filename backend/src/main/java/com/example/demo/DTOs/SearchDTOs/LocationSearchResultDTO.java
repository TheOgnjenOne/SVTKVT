package com.example.demo.DTOs.SearchDTOs;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * UES: jedan rezultat pretrage (mesto) + highlight sažeci (ocena 10).
 */
@Getter
@Setter
public class LocationSearchResultDTO {

    private Long id;
    private String naziv;
    private String opis;          // opis iz UI-a (NE prikazuje se pdfOpis)
    private String adresa;
    private String tipMesta;

    private Integer reviewCount;
    private Double prosecnaOcena;
    private Double avgNastup;
    private Double avgZvukSvetlo;
    private Double avgProstor;
    private Double avgUkupno;

    private Long imageId;
    private Boolean hasPdf;

    private Double score;

    // Highlight sažeci po poljima (npr. ["...<em>Beograd</em>..."])
    private List<String> highlights;
}
