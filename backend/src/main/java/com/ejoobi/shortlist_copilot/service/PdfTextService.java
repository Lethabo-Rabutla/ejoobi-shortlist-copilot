package com.ejoobi.shortlist_copilot.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.Loader;

import java.io.IOException;

@Service 
public class PdfTextService {
    public String extractText(MultipartFile file){
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded.");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Uploaded file is not a PDF.");
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            String text = new PDFTextStripper().getText(document).trim();
            if (text.isBlank()) {
                throw new IllegalArgumentException(
                        "No readable text found. This may be a scanned PDF. Please paste the text instead.");
            }
            
            return text;
            
        }catch (IOException e) {
            throw new IllegalArgumentException("This PDF could not be read.");
        }
    }
}