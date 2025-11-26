package com.example.demo.Services;

import com.example.demo.DTOs.ImageDTOs.ImageBytesWrapper;
import com.example.demo.DTOs.ImageDTOs.ImageResponseDTO;
import com.example.demo.Model.Image;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface IImageService {

    Optional<Image> getImageById(Long id);

    Image saveImageFile(MultipartFile file) throws IOException;

    ImageBytesWrapper getImageData(Long imageId) throws IOException;

    @Transactional
    void deleteImage(Long imageId);

    ImageResponseDTO mapToDTO(Image image);
}
