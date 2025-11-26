package com.example.demo.Controller;

import com.example.demo.DTOs.ImageDTOs.ImageBytesWrapper;
import com.example.demo.Services.IImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    private final IImageService imageService;

    @Autowired
    public ImageController(IImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long imageId) {
        logger.info("Request received to fetch image with ID: {}", imageId);
        try {
            ImageBytesWrapper imageWrapper = imageService.getImageData(imageId);

            if (imageWrapper == null) {
                logger.warn("Image ID {} not found in storage.", imageId);
                return ResponseEntity.notFound().build();
            }

            logger.info("Successfully retrieved image ID {} with content type: {}", imageId, imageWrapper.mediaType());
            return ResponseEntity.ok()
                    .contentType(imageWrapper.mediaType())
                    .body(imageWrapper.imageBytes());

        } catch (Exception e) {
            logger.error("Error occurred while fetching image ID {}: {}", imageId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}