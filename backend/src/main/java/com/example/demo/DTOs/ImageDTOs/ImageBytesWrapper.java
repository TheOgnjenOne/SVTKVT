package com.example.demo.DTOs.ImageDTOs;

import org.springframework.http.MediaType;

public record ImageBytesWrapper(byte[] imageBytes, MediaType mediaType) {
}
