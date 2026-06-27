package com.example.demo.Config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * UES: pravi {@link MinioClient} bean i pri startu kreira bucket-e ako ne postoje.
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final Logger logger = LoggerFactory.getLogger(MinioConfig.class);

    private final MinioProperties properties;

    public MinioConfig(MinioProperties properties) {
        this.properties = properties;
    }

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(properties.getUrl())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();

        ensureBucket(client, properties.getBucket().getImages());
        ensureBucket(client, properties.getBucket().getPdfs());

        return client;
    }

    private void ensureBucket(MinioClient client, String bucket) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                logger.info("MinIO bucket '{}' kreiran.", bucket);
            } else {
                logger.info("MinIO bucket '{}' već postoji.", bucket);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Ne mogu da obezbedim MinIO bucket: " + bucket, e);
        }
    }
}
