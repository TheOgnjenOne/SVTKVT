package com.example.demo.Services.Impl;

import com.example.demo.DTOs.ImageDTOs.ImageBytesWrapper;
import com.example.demo.DTOs.ImageDTOs.ImageResponseDTO;
import com.example.demo.Model.Image;
import com.example.demo.Repository.IImageRepository;
import com.example.demo.Services.IImageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class ImageServiceImpl implements IImageService {

    private final IImageRepository imageRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Autowired
    public ImageServiceImpl(IImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public Optional<Image> getImageById(Long id) {
        return imageRepository.findById(id);
    }

    @Override
    public Image saveImageFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        Path uploadPath = Paths.get("uploads");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = System.currentTimeMillis() + "_" + originalFilename;
        Path filePath = uploadPath.resolve(uniqueFilename);

        Files.copy(file.getInputStream(), filePath);

        Image image = new Image();
        image.setPath(uniqueFilename);
        return imageRepository.save(image);
    }

    @Override
    public ImageBytesWrapper getImageData(Long imageId) throws IOException {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Slika nije pronađena sa ID-em: " + imageId));

        Path filePath = Paths.get("uploads").resolve(image.getPath());
        byte[] imageBytes = Files.readAllBytes(filePath);

        String fileName = image.getPath();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else if (fileName.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (fileName.endsWith(".gif")) {
            mediaType = MediaType.IMAGE_GIF;
        }
        return new ImageBytesWrapper(imageBytes, mediaType);
    }

    @Transactional
    @Override
    public void deleteImage(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with ID: " + imageId));

        try {
            Path filePath = Paths.get("uploads").resolve(image.getPath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Warning: Could not delete file from disk: " + image.getPath());
        }

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



}

