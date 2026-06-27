package com.example.demo.Services.Impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * UES (ocena 6): izvlači tekstualni sadržaj iz PDF opisa mesta (PDFBox 3.x),
 * radi indeksiranja u ES polje {@code pdfOpis}.
 */
@Component
public class PdfTextExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PdfTextExtractor.class);

    /** Vraća sav tekst iz PDF-a, ili prazan string ako parsiranje ne uspe. */
    public String extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return "";
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            logger.warn("Neuspešno parsiranje PDF teksta: {}", e.getMessage());
            return "";
        }
    }
}
