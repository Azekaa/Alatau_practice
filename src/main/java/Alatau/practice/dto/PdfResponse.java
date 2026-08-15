package Alatau.practice.dto;

import java.util.List;

public record PdfResponse(
        String fileName,
        boolean isSafe,
        String result,
        List<String> detectedObjects
) {

}
