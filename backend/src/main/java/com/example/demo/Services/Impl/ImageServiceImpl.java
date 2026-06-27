package com.example.demo.Services.Impl;

import com.example.demo.Config.MinioProperties;
import com.example.demo.DTOs.ImageDTOs.ImageBytesWrapper;
import com.example.demo.DTOs.ImageDTOs.ImageResponseDTO;
import com.example.demo.Model.Image;
import com.example.demo.Repository.IImageRepository;
import com.example.demo.Services.IFileStorageService;
import com.example.demo.Services.IImageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * UES (ocena 6): slike se sada čuvaju u MinIO bucket-u, ne na fajl-sistemu.
 * Kolona {@code Image.path} ostaje ista, ali sada nosi MinIO object key (šema baze nepromenjena).
 * Ugovor {@code /api/images/{id}} (vraća bajtove) ostaje identičan pa frontend ne treba menjati.
 */
@Service
public class ImageServiceImpl implements IImageService {

    private final IImageRepository imageRepository;
    private final IFileStorageService fileStorageService;
    private final MinioProperties minioProperties;

    @Autowired
    public ImageServiceImpl(IImageRepository imageRepository,
                            IFileStorageService fileStorageService,
                            MinioProperties minioProperties) {
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
        this.minioProperties = minioProperties;
    }

    private String imagesBucket() {
        return minioProperties.getBucket().getImages();
    }

    @Override
    public Optional<Image> getImageById(Long id) {
        return imageRepository.findById(id);
    }

    @Override
    public Image saveImageFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String key;
        try {
            key = fileStorageService.upload(file, imagesBucket());
        } catch (Exception e) {
            throw new IOException("Neuspešno otpremanje slike u MinIO: " + e.getMessage(), e);
        }

        Image image = new Image();
        image.setPath(key);
        return imageRepository.save(image);
    }

    @Override
    public ImageBytesWrapper getImageData(Long imageId) throws IOException {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Slika nije pronađena sa ID-em: " + imageId));

        byte[] imageBytes;
        try {
            imageBytes = fileStorageService.download(imagesBucket(), image.getPath());
        } catch (Exception e) {
            throw new IOException("Neuspešno preuzimanje slike iz MinIO: " + e.getMessage(), e);
        }

        return new ImageBytesWrapper(imageBytes, resolveMediaType(image.getPath()));
    }

    @Transactional
    @Override
    public void deleteImage(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with ID: " + imageId));

        fileStorageService.delete(imagesBucket(), image.getPath());
        imageRepository.delete(image);
    }

    @Override
    public ImageResponseDTO mapToDTO(Image image) {
        if (image == null) {
            return null;
        }
        ImageResponseDTO dto = new ImageResponseDTO();
        dto.setId(image.getId());
        dto.setPath(image.getPath());
        return dto;
    }

    private MediaType resolveMediaType(String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".jfif")) {
            return MediaType.IMAGE_JPEG;
        } else if (name.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (name.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        } else if (name.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
