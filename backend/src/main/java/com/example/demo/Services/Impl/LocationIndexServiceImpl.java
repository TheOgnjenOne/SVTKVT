package com.example.demo.Services.Impl;

import com.example.demo.Config.MinioProperties;
import com.example.demo.Model.Location;
import com.example.demo.Repository.ILocationRepository;
import com.example.demo.Repository.IReviewRepository;
import com.example.demo.Search.LocationDocument;
import com.example.demo.Services.IFileStorageService;
import com.example.demo.Services.ILocationIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UES: gradi {@link LocationDocument} iz relacionog modela (+ parsira PDF iz MinIO-a)
 * i upisuje ga u Elasticsearch. PDF tekst se rekonstruiše iz izvora (MinIO) pri svakom
 * indeksiranju, tako da je indeks uvek konzistentan sa stvarnim PDF-om.
 */
@Service
public class LocationIndexServiceImpl implements ILocationIndexService {

    private static final Logger logger = LoggerFactory.getLogger(LocationIndexServiceImpl.class);

    private final ElasticsearchOperations elasticsearchOperations;
    private final ILocationRepository locationRepository;
    private final IReviewRepository reviewRepository;
    private final IFileStorageService fileStorageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final MinioProperties minioProperties;

    public LocationIndexServiceImpl(ElasticsearchOperations elasticsearchOperations,
                                    ILocationRepository locationRepository,
                                    IReviewRepository reviewRepository,
                                    IFileStorageService fileStorageService,
                                    PdfTextExtractor pdfTextExtractor,
                                    MinioProperties minioProperties) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.locationRepository = locationRepository;
        this.reviewRepository = reviewRepository;
        this.fileStorageService = fileStorageService;
        this.pdfTextExtractor = pdfTextExtractor;
        this.minioProperties = minioProperties;
    }

    @Override
    public void indexLocation(Location location) {
        if (location == null || location.getId() == null) {
            return;
        }
        try {
            LocationDocument doc = buildDocument(location);
            elasticsearchOperations.save(doc);
            logger.debug("Indeksirano mesto ID {} ('{}').", location.getId(), location.getName());
        } catch (Exception e) {
            // Ne rušimo SVT/KVA CRUD ako je ES nedostupan — samo logujemo.
            logger.warn("Neuspešno indeksiranje mesta ID {}: {}", location.getId(), e.getMessage());
        }
    }

    @Override
    public void indexLocation(Long locationId) {
        if (locationId == null) {
            return;
        }
        locationRepository.findById(locationId).ifPresent(this::indexLocation);
    }

    @Override
    public void deleteFromIndex(Long locationId) {
        if (locationId == null) {
            return;
        }
        try {
            elasticsearchOperations.delete(String.valueOf(locationId), LocationDocument.class);
        } catch (Exception e) {
            logger.warn("Neuspešno brisanje mesta ID {} iz indeksa: {}", locationId, e.getMessage());
        }
    }

    @Override
    public long reindexAll() {
        List<Location> all = locationRepository.findAll();
        long count = 0;
        for (Location location : all) {
            indexLocation(location);
            count++;
        }
        logger.info("Reindeksirano {} mesta.", count);
        return count;
    }

    private LocationDocument buildDocument(Location location) {
        Long id = location.getId();

        LocationDocument doc = new LocationDocument();
        doc.setId(String.valueOf(id));
        doc.setNaziv(location.getName());
        doc.setOpis(location.getDescription());
        doc.setAdresa(location.getAddress());
        doc.setTipMesta(location.getType());
        doc.setImageId(location.getImage() != null ? location.getImage().getId() : null);
        doc.setProsecnaOcena(location.getTotalRating());

        Long reviewCount = reviewRepository.countByLocationIdAndDeletedFalse(id);
        doc.setReviewCount(reviewCount != null ? reviewCount.intValue() : 0);

        doc.setAvgNastup(reviewRepository.avgPerformanceByLocationId(id));
        doc.setAvgZvukSvetlo(reviewRepository.avgSoundLightingByLocationId(id));
        doc.setAvgProstor(reviewRepository.avgVenueByLocationId(id));
        doc.setAvgUkupno(reviewRepository.calculateAverageOverallRatingByLocationId(id));

        String pdfKey = location.getPdfKey();
        if (pdfKey != null && !pdfKey.isBlank()) {
            doc.setHasPdf(true);
            doc.setPdfOpis(extractPdfText(pdfKey));
        } else {
            doc.setHasPdf(false);
            doc.setPdfOpis("");
        }

        return doc;
    }

    private String extractPdfText(String pdfKey) {
        try {
            byte[] pdfBytes = fileStorageService.download(minioProperties.getBucket().getPdfs(), pdfKey);
            return pdfTextExtractor.extractText(pdfBytes);
        } catch (Exception e) {
            logger.warn("Ne mogu da pročitam/parsiram PDF '{}': {}", pdfKey, e.getMessage());
            return "";
        }
    }
}
