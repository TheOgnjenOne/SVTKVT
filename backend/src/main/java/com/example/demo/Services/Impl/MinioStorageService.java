package com.example.demo.Services.Impl;

import com.example.demo.Services.IFileStorageService;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * UES: MinIO implementacija object storage-a (ocena 6 — slike i PDF idu u MinIO, ne na fajl-sistem).
 */
@Service
public class MinioStorageService implements IFileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;

    public MinioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public String upload(MultipartFile file, String bucket) throws Exception {
        String key = UUID.randomUUID() + extensionOf(file.getOriginalFilename());

        try (var in = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(in, file.getSize(), -1)
                            .contentType(file.getContentType() != null
                                    ? file.getContentType()
                                    : "application/octet-stream")
                            .build());
        }

        logger.info("Otpremljen objekat '{}' u bucket '{}'.", key, bucket);
        return key;
    }

    @Override
    public byte[] download(String bucket, String key) throws Exception {
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return response.readAllBytes();
        }
    }

    @Override
    public void delete(String bucket, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(key).build());
            logger.info("Obrisan objekat '{}' iz bucket-a '{}'.", key, bucket);
        } catch (Exception e) {
            logger.warn("Ne mogu da obrišem objekat '{}' iz bucket-a '{}': {}", key, bucket, e.getMessage());
        }
    }

    @Override
    public boolean exists(String bucket, String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(key).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }
}
