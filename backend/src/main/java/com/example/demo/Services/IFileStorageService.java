package com.example.demo.Services;

import org.springframework.web.multipart.MultipartFile;

/**
 * UES: apstrakcija nad object storage-om (MinIO). Koristi se i za slike i za PDF dokumente.
 */
public interface IFileStorageService {

    /** Otprema fajl u dati bucket i vraća generisani object key (UUID + ekstenzija). */
    String upload(MultipartFile file, String bucket) throws Exception;

    /** Vraća sadržaj objekta kao niz bajtova. */
    byte[] download(String bucket, String key) throws Exception;

    /** Briše objekat (tiho ignoriše ako ne postoji). */
    void delete(String bucket, String key);

    /** Proverava da li objekat postoji. */
    boolean exists(String bucket, String key);
}
