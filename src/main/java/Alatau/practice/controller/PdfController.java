package Alatau.practice.controller;

import Alatau.practice.dto.PdfResponse;
import Alatau.practice.service.PdfService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
@RequestMapping("/api/pdf")
public class PdfController {
    private final PdfService pdfService;
    @PostMapping("/check")
    public PdfResponse checkPdf(@RequestParam("file")MultipartFile file){
        return pdfService.checkPdf(file);
    }
}
