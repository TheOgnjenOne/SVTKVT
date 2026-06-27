package com.example.demo.DTOs.SearchDTOs;

import lombok.Getter;
import lombok.Setter;

/**
 * UES: parametri pretrage mesta.
 * Tekstualna polja (naziv/opis/pdfOpis) podržavaju specijalnu sintaksu iz forme (ocena 9):
 *   "fraza"   -> PhraseQuery (match_phrase)
 *   Ivan M*   -> PrefixQuery (match_phrase_prefix)
 *   ~rizika   -> FuzzyQuery (match + fuzziness AUTO)
 *   inače     -> obična match pretraga
 */
@Getter
@Setter
public class LocationSearchRequestDTO {

    // Tekstualna polja (ocena 6 + 9)
    private String naziv;
    private String opis;
    private String pdfOpis;

    // Tačan filter po tipu mesta (keyword)
    private String tipMesta;

    // Operator između tekstualnih polja: "AND" (must) ili "OR" (should). Default AND. (ocena 8)
    private String operator;

    // Opseg broja review-a (ocena 7) — donja i/ili gornja granica opcione
    private Integer reviewCountMin;
    private Integer reviewCountMax;

    // Opseg prosečne ocene po kategorijama (ocena 10) — granice opcione
    private Double avgNastupMin;
    private Double avgNastupMax;
    private Double avgZvukSvetloMin;
    private Double avgZvukSvetloMax;
    private Double avgProstorMin;
    private Double avgProstorMax;
    private Double avgUkupnoMin;
    private Double avgUkupnoMax;

    // Sortiranje (ocena 10): sortBy = "naziv" -> sort po naziv.keyword; sortDir = "asc"/"desc"
    private String sortBy;
    private String sortDir;

    // Paginacija
    private Integer page;
    private Integer size;
}
