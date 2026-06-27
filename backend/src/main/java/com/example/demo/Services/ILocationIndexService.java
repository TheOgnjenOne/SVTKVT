package com.example.demo.Services;

import com.example.demo.Model.Location;

/**
 * UES: održava Elasticsearch indeks "locations" u skladu sa relacionim podacima.
 */
public interface ILocationIndexService {

    /** (Re)indeksira jedno mesto na osnovu trenutnog stanja u bazi i MinIO-u. */
    void indexLocation(Location location);

    /** (Re)indeksira mesto po ID-u (učitava iz baze). Tiho ignoriše ako mesto ne postoji. */
    void indexLocation(Long locationId);

    /** Briše dokument mesta iz indeksa. */
    void deleteFromIndex(Long locationId);

    /** Reindeksira sva mesta (seed / ručno osvežavanje). Vraća broj indeksiranih mesta. */
    long reindexAll();
}
