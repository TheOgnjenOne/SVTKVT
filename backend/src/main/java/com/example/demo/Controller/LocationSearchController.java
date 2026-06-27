package com.example.demo.Controller;

import com.example.demo.DTOs.SearchDTOs.LocationSearchRequestDTO;
import com.example.demo.DTOs.SearchDTOs.LocationSearchResponseDTO;
import com.example.demo.Services.ILocationIndexService;
import com.example.demo.Services.ILocationSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * UES: REST endpointi za pretragu mesta nad Elasticsearch indeksom.
 */
@RestController
@RequestMapping("/api/search")
public class LocationSearchController {

    private static final Logger logger = LoggerFactory.getLogger(LocationSearchController.class);

    private final ILocationSearchService searchService;
    private final ILocationIndexService indexService;

    public LocationSearchController(ILocationSearchService searchService, ILocationIndexService indexService) {
        this.searchService = searchService;
        this.indexService = indexService;
    }

    /** Glavna pretraga (ocene 6–10). */
    @PostMapping("/locations")
    public ResponseEntity<LocationSearchResponseDTO> search(@RequestBody LocationSearchRequestDTO request) {
        logger.info("UES pretraga: naziv='{}', opis='{}', pdfOpis='{}', operator='{}', sortBy='{}'",
                request.getNaziv(), request.getOpis(), request.getPdfOpis(), request.getOperator(), request.getSortBy());
        return ResponseEntity.ok(searchService.search(request));
    }

    /** MoreLikeThis — slična mesta (ocena 9). */
    @GetMapping("/locations/{id}/more-like-this")
    public ResponseEntity<LocationSearchResponseDTO> moreLikeThis(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("UES MoreLikeThis za mesto ID {}", id);
        return ResponseEntity.ok(searchService.moreLikeThis(id, size));
    }

    /** Ručni reindeks svih mesta (seed / osvežavanje) — samo admin. */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> reindexAll() {
        long count = indexService.reindexAll();
        logger.info("Admin pokrenuo reindeks: {} mesta.", count);
        return ResponseEntity.ok(Map.of("reindexed", count));
    }
}
