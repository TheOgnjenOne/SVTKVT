package com.example.demo.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * UES: konfiguracija MinIO konekcije i bucket-a (vezuje se na {@code minio.*} iz application.properties).
 */
@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
public class MinioProperties {

    private String url;
    private String accessKey;
    private String secretKey;
    private Bucket bucket = new Bucket();

    @Getter
    @Setter
    public static class Bucket {
        private String images;
        private String pdfs;
    }
}
