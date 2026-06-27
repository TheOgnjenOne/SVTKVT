package com.example.demo.Services.Impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhrasePrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MoreLikeThisQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.example.demo.DTOs.SearchDTOs.LocationSearchRequestDTO;
import com.example.demo.DTOs.SearchDTOs.LocationSearchResponseDTO;
import com.example.demo.DTOs.SearchDTOs.LocationSearchResultDTO;
import com.example.demo.Search.LocationDocument;
import com.example.demo.Services.ILocationSearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LocationSearchServiceImpl implements ILocationSearchService {

    private static final Logger logger = LoggerFactory.getLogger(LocationSearchServiceImpl.class);
    private static final List<String> HIGHLIGHT_FIELDS = List.of("naziv", "opis", "pdfOpis");

    private final ElasticsearchOperations elasticsearchOperations;

    public LocationSearchServiceImpl(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public LocationSearchResponseDTO search(LocationSearchRequestDTO request) {
        boolean useAnd = request.getOperator() == null || !request.getOperator().equalsIgnoreCase("OR");

        // --- svi uslovi učestvuju u AND/OR logici (ocena 6 + 8 + 9) ---
        List<Query> conditions = new ArrayList<>();
        addIfNotNull(conditions, buildTextQuery("naziv", request.getNaziv()));
        addIfNotNull(conditions, buildTextQuery("opis", request.getOpis()));
        addIfNotNull(conditions, buildTextQuery("pdfOpis", request.getPdfOpis()));
        if (isNotBlank(request.getTipMesta())) {
            String tip = request.getTipMesta().trim();
            conditions.add(TermQuery.of(t -> t.field("tipMesta").value(tip).caseInsensitive(true))._toQuery());
        }
        addIfNotNull(conditions, buildNumberRange("reviewCount",
                toDouble(request.getReviewCountMin()), toDouble(request.getReviewCountMax())));
        addIfNotNull(conditions, buildNumberRange("avgNastup", request.getAvgNastupMin(), request.getAvgNastupMax()));
        addIfNotNull(conditions, buildNumberRange("avgZvukSvetlo", request.getAvgZvukSvetloMin(), request.getAvgZvukSvetloMax()));
        addIfNotNull(conditions, buildNumberRange("avgProstor", request.getAvgProstorMin(), request.getAvgProstorMax()));
        addIfNotNull(conditions, buildNumberRange("avgUkupno", request.getAvgUkupnoMin(), request.getAvgUkupnoMax()));

        List<Query> filters = new ArrayList<>();

        // --- BooleanQuery (ocena 8) ---
        Query query = buildBoolQuery(conditions, filters, useAnd);

        // --- paginacija + sortiranje + highlight ---
        int page = request.getPage() != null && request.getPage() >= 0 ? request.getPage() : 0;
        int size = request.getSize() != null && request.getSize() > 0 ? Math.min(request.getSize(), 100) : 20;

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(page, size))
                .withHighlightQuery(buildHighlightQuery());

        // sortiranje po nazivu (ocena 10)
        if (isNotBlank(request.getSortBy()) && request.getSortBy().equalsIgnoreCase("naziv")) {
            SortOrder order = "desc".equalsIgnoreCase(request.getSortDir()) ? SortOrder.Desc : SortOrder.Asc;
            builder.withSort(so -> so.field(f -> f.field("naziv.keyword").order(order)));
        }

        SearchHits<LocationDocument> hits = elasticsearchOperations.search(builder.build(), LocationDocument.class);
        return toResponse(hits);
    }

    @Override
    public LocationSearchResponseDTO moreLikeThis(Long locationId, int size) {
        int effectiveSize = size > 0 ? Math.min(size, 50) : 10;

        // Dohvati source dokument da izvučemo tekst — MLT sa like.document(id)
        // ne radi bez term_vectors; koristimo like.text sa sadržajem dokumenta.
        LocationDocument source = elasticsearchOperations.get(String.valueOf(locationId), LocationDocument.class);
        if (source == null) {
            logger.warn("MLT: dokument ID {} nije pronađen u ES indeksu.", locationId);
            return new LocationSearchResponseDTO(0, List.of());
        }
        String likeText = buildMltText(source);

        // Isključi izvorni dokument iz rezultata filterom
        Long excludedId = locationId;
        Query query = MoreLikeThisQuery.of(m -> m
                .fields("naziv", "opis", "tipMesta", "pdfOpis")
                .like(l -> l.text(likeText))
                .minTermFreq(1)
                .minDocFreq(2)   // termin mora biti u >=2 dokumenta → filtrira specifične termove
                .maxQueryTerms(25))._toQuery();

        Query excludeSelf = co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.of(b -> b
                .must(query)
                .mustNot(co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery.of(
                        ids -> ids.values(String.valueOf(excludedId)))._toQuery()))._toQuery();

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(excludeSelf)
                .withPageable(PageRequest.of(0, effectiveSize))
                .withHighlightQuery(buildHighlightQuery())
                .build();

        SearchHits<LocationDocument> hits = elasticsearchOperations.search(nativeQuery, LocationDocument.class);
        return toResponse(hits);
    }

    private String buildMltText(LocationDocument doc) {
        StringBuilder sb = new StringBuilder();
        if (isNotBlank(doc.getNaziv()))    sb.append(doc.getNaziv()).append(' ');
        if (isNotBlank(doc.getTipMesta())) sb.append(doc.getTipMesta()).append(' ');
        if (isNotBlank(doc.getOpis()))     sb.append(doc.getOpis()).append(' ');
        if (isNotBlank(doc.getPdfOpis()))  sb.append(doc.getPdfOpis());
        return sb.toString().trim();
    }

    // ---------------------------------------------------------------------
    // Pomoćne metode za građenje upita
    // ---------------------------------------------------------------------

    /**
     * Parsira vrednost tekstualnog polja iz forme u odgovarajući ES upit (ocena 9):
     * "fraza" -> match_phrase, ~term -> fuzzy, term* -> match_phrase_prefix, inače match.
     */
    private Query buildTextQuery(String field, String raw) {
        if (!isNotBlank(raw)) {
            return null;
        }
        String value = raw.trim();

        // PhraseQuery: "..."
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            String phrase = value.substring(1, value.length() - 1).trim();
            if (phrase.isEmpty()) return null;
            return MatchPhraseQuery.of(m -> m.field(field).query(phrase))._toQuery();
        }

        // FuzzyQuery: ~term
        if (value.startsWith("~")) {
            String term = value.substring(1).trim();
            if (term.isEmpty()) return null;
            return MatchQuery.of(m -> m.field(field).query(term).fuzziness("AUTO"))._toQuery();
        }

        // PrefixQuery: "Ivan M*" -> match_phrase_prefix (radi i sa više reči)
        if (value.contains("*")) {
            String text = value.replace("*", "").trim();
            if (text.isEmpty()) return null;
            return MatchPhrasePrefixQuery.of(m -> m.field(field).query(text))._toQuery();
        }

        // obična match pretraga
        return MatchQuery.of(m -> m.field(field).query(value))._toQuery();
    }

    private Query buildNumberRange(String field, Double min, Double max) {
        if (min == null && max == null) {
            return null;
        }
        return RangeQuery.of(r -> r.number(n -> {
            n.field(field);
            if (min != null) n.gte(min);
            if (max != null) n.lte(max);
            return n;
        }))._toQuery();
    }

    private Query buildBoolQuery(List<Query> textQueries, List<Query> filters, boolean useAnd) {
        if (textQueries.isEmpty() && filters.isEmpty()) {
            return MatchAllQuery.of(m -> m)._toQuery();
        }
        BoolQuery.Builder bool = new BoolQuery.Builder();
        if (!textQueries.isEmpty()) {
            if (useAnd) {
                bool.must(textQueries);
            } else {
                bool.should(textQueries).minimumShouldMatch("1");
            }
        }
        if (!filters.isEmpty()) {
            bool.filter(filters);
        }
        return bool.build()._toQuery();
    }

    private HighlightQuery buildHighlightQuery() {
        List<HighlightField> fields = HIGHLIGHT_FIELDS.stream().map(HighlightField::new).toList();
        return new HighlightQuery(new Highlight(fields), LocationDocument.class);
    }

    // ---------------------------------------------------------------------
    // Mapiranje rezultata
    // ---------------------------------------------------------------------

    private LocationSearchResponseDTO toResponse(SearchHits<LocationDocument> hits) {
        List<LocationSearchResultDTO> results = new ArrayList<>();
        for (SearchHit<LocationDocument> hit : hits) {
            results.add(toResult(hit));
        }
        return new LocationSearchResponseDTO(hits.getTotalHits(), results);
    }

    private LocationSearchResultDTO toResult(SearchHit<LocationDocument> hit) {
        LocationDocument doc = hit.getContent();
        LocationSearchResultDTO dto = new LocationSearchResultDTO();

        dto.setId(parseLong(doc.getId()));
        dto.setNaziv(doc.getNaziv());
        dto.setOpis(doc.getOpis());
        dto.setAdresa(doc.getAdresa());
        dto.setTipMesta(doc.getTipMesta());
        dto.setReviewCount(doc.getReviewCount());
        dto.setProsecnaOcena(doc.getProsecnaOcena());
        dto.setAvgNastup(doc.getAvgNastup());
        dto.setAvgZvukSvetlo(doc.getAvgZvukSvetlo());
        dto.setAvgProstor(doc.getAvgProstor());
        dto.setAvgUkupno(doc.getAvgUkupno());
        dto.setImageId(doc.getImageId());
        dto.setHasPdf(doc.getHasPdf());
        // Kod sortiranja ES vraća _score = null -> Spring mapira na NaN, a Jackson bi serijalizovao
        // nevalidan JSON token "NaN" (Angular HttpClient bi pukao pri parsiranju). Zato čuvamo null.
        double score = hit.getScore();
        dto.setScore(Double.isNaN(score) ? null : score);

        List<String> highlights = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : hit.getHighlightFields().entrySet()) {
            highlights.addAll(entry.getValue());
        }
        dto.setHighlights(highlights);

        return dto;
    }

    // ---------------------------------------------------------------------
    // Sitnice
    // ---------------------------------------------------------------------

    private void addIfNotNull(List<Query> list, Query query) {
        if (query != null) {
            list.add(query);
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private Double toDouble(Integer value) {
        return value != null ? value.doubleValue() : null;
    }

    private Long parseLong(String s) {
        try {
            return s != null ? Long.parseLong(s) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
