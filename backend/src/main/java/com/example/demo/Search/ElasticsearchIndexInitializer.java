package com.example.demo.Search;

import com.example.demo.Services.ILocationIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * UES: pri startu eksplicitno kreira indeks "locations" sa custom analyzer-om (ako ne postoji)
 * i seed-uje ga postojećim mestima iz baze. Sve je u try/catch da app može da se digne i
 * kad ES još nije spreman (npr. docker se tek pokreće).
 */
@Component
public class ElasticsearchIndexInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndexInitializer.class);

    private final ElasticsearchOperations elasticsearchOperations;
    private final ILocationIndexService locationIndexService;

    public ElasticsearchIndexInitializer(ElasticsearchOperations elasticsearchOperations,
                                         ILocationIndexService locationIndexService) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.locationIndexService = locationIndexService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(LocationDocument.class);
            if (!indexOps.exists()) {
                // createWithMapping primenjuje settings iz @Setting (sr_custom analyzer) + mapping iz @Field
                indexOps.createWithMapping();
                logger.info("Kreiran ES indeks 'locations' sa custom analyzer-om 'sr_custom'.");
            } else {
                logger.info("ES indeks 'locations' već postoji.");
            }

            long count = locationIndexService.reindexAll();
            logger.info("Inicijalni reindeks završen: {} mesta.", count);
        } catch (Exception e) {
            logger.warn("Inicijalizacija ES indeksa preskočena (ES nedostupan?): {}", e.getMessage());
        }
    }
}
