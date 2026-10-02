package com.ejoobi.shortlist_copilot.controller;

import com.ejoobi.shortlist_copilot.service.PdfTextService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping ("/api")
public class CvFileController {

    private final PdfTextService pdfTextService;

    public CvFileController(PdfTextService pdfTextService) {
        this.pdfTextService = pdfTextService;
    }

    @PostMapping (value = "/extract-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> extractText(@RequestParam("file") MultipartFile file) {
        return Map.of("text", pdfTextService.extractText(file));
    }
}
