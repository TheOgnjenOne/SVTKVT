package com.example.demo.Controller;

import com.example.demo.Config.MinioProperties;
import com.example.demo.Model.Location;
import com.example.demo.Model.User;
import com.example.demo.Services.IFileStorageService;
import com.example.demo.Services.ILocationService;
import com.example.demo.Services.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * UES (ocena 6): upload PDF opisa uz mesto (parsira se i indeksira kao pdfOpis)
 * i download PDF-a iz prikaza mesta.
 */
@RestController
@RequestMapping("/api/locations")
public class LocationPdfController {

    private static final Logger logger = LoggerFactory.getLogger(LocationPdfController.class);

    private final ILocationService locationService;
    private final IFileStorageService fileStorageService;
    private final IUserService userService;
    private final MinioProperties minioProperties;

    public LocationPdfController(ILocationService locationService,
                                IFileStorageService fileStorageService,
                                IUserService userService,
                                MinioProperties minioProperties) {
        this.locationService = locationService;
        this.fileStorageService = fileStorageService;
        this.userService = userService;
        this.minioProperties = minioProperties;
    }

    private String pdfBucket() {
        return minioProperties.getBucket().getPdfs();
    }

    /** Upload PDF-a uz mesto (admin ili menadžer tog mesta). Posle upload-a se mesto reindeksira. */
    @PostMapping(value = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, String>> uploadPdf(
            @PathVariable Long id,
            @RequestParam("pdf") MultipartFile pdf,
            Authentication authentication) throws Exception {

        if (pdf == null || pdf.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "PDF fajl je prazan."));
        }
        String filename = pdf.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dozvoljen je samo PDF fajl."));
        }

        Location location = locationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesto nije pronađeno: " + id));

        if (!isAdminOrManagerOf(authentication, id)) {
            logger.warn("Korisnik {} nema pravo da postavi PDF za mesto {}.", authentication.getName(), id);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        // obriši stari PDF ako postoji
        if (location.getPdfKey() != null && !location.getPdfKey().isBlank()) {
            fileStorageService.delete(pdfBucket(), location.getPdfKey());
        }

        String key = fileStorageService.upload(pdf, pdfBucket());
        location.setPdfKey(key);
        // save() preko servisa pokreće reindeks (parsira PDF -> pdfOpis)
        locationService.save(location);

        logger.info("Postavljen PDF '{}' za mesto {} (korisnik {}).", key, id, authentication.getName());
        return ResponseEntity.ok(Map.of("pdfKey", key));
    }

    /** Download PDF-a mesta. */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Location location = locationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesto nije pronađeno: " + id));

        if (location.getPdfKey() == null || location.getPdfKey().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] bytes = fileStorageService.download(pdfBucket(), location.getPdfKey());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mesto-" + id + ".pdf\"")
                    .body(bytes);
        } catch (Exception e) {
            logger.error("Greška pri preuzimanju PDF-a za mesto {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    private boolean isAdminOrManagerOf(Authentication authentication, Long locationId) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return true;
        }
        User user = userService.findByEmail(authentication.getName());
        return user != null && locationService.isManagerOfLocation(user.getId(), locationId);
    }
}
