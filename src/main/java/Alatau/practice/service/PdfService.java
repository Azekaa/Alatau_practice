package Alatau.practice.service;

import Alatau.practice.dto.PdfResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PdfService {
    private static final long MAX_FILE_SIZE = 5*1024*1024;
    private static final List<String> Dangerous_objects = List.of(
            "/JS", "/JavaScript", "/OpenAction", "/AA", "/A", "/S", "/EmbeddedFiles", "/FileAttachment", "/Collection", "/FS", "/F", "/Launch", "/Win", "/Mac", "/Unix", "/RichMedia", "/RichMediaContent", "/RichMediaConfiguration", "/RichMediaAnnotation"
    );


    public PdfResponse checkPdf(MultipartFile file) {
        String fileName = file.getOriginalFilename();

        log.info("File checking has started: {}",fileName);

        if (file.isEmpty()){
            log.warn("File is empty: {}", fileName);
            return new PdfResponse(fileName,false,"File is empty", List.of());
        }
        if (file.getSize() > MAX_FILE_SIZE){
            log.warn("File too large: {}, SIZE: {} byte",fileName,file.getSize());
            return new PdfResponse(fileName,false,"The file size must not exceed than 5MB",List.of());
        }
        if(fileName == null || !fileName.toLowerCase().endsWith(".pdf")){
            log.warn("Format not correctly: {}", fileName);
            return new PdfResponse(fileName,false,"Only PDF format are allowed", List.of());
        }
        log.info("File pass basic checking: {}, File size: {}", fileName,file.getSize());

        List<String> detectedObject = findDangerousObj(file);
        if (detectedObject.isEmpty()){
            log.info("File is safe: {}", fileName);
            return new PdfResponse(fileName,true,"No unsafe object detected",detectedObject);
        }
        log.warn("unsafe objects detected: {}: {}", fileName,detectedObject);
        return new PdfResponse(fileName,false,"Unsafe objects detected",detectedObject);
    }
    private List<String> findDangerousObj(MultipartFile file){
        List<String> detectedObject = new ArrayList<>();
        try(PDDocument document = Loader.loadPDF(file.getBytes())) {
            COSDictionary catalog = document.getDocumentCatalog().getCOSObject();
            checkDictionary(catalog,detectedObject);
        for (var page : document.getPages()){
            checkDictionary(page.getCOSObject(),detectedObject);
        }
        }catch (Exception e){
            log.error("Error reading PDF: {}", file.getOriginalFilename(),e);
        }
        return detectedObject;
    }

    private void checkDictionary(COSDictionary catalog, List<String> detectedObject) {
        for (COSName key : catalog.keySet()){
            String objectName = "/" + key.getName();
            if (Dangerous_objects.contains(objectName)){
                if(!detectedObject.contains(objectName)){
                detectedObject.add(objectName);
                }
                log.warn("unsafe object detected: {}", objectName);
            }
        COSBase value = catalog.getDictionaryObject(key);
            if (value instanceof COSDictionary nestedDict){
                checkDictionary(nestedDict,detectedObject);
            }
            if (value instanceof COSArray array){
                checkArray(array,detectedObject);
            }
        }
    }
    private void checkArray(COSArray array, List<String> detectedObject){
        for (COSBase item : array){
            if (item instanceof COSDictionary dictionary){
                checkDictionary(dictionary,detectedObject);
            }
            if (item instanceof COSArray nestedArray){
                checkArray(nestedArray,detectedObject );
            }
        }
    }
    private void storageFile(MultipartFile file){
        try {
            Path diriectory = Path.of("storage/suspicious");
            Files.createDirectories(diriectory);
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = diriectory.resolve(fileName);
            file.transferTo(filePath);
            log.warn("Suspicious file saved: {}", filePath);
        }catch (Exception e){
            log.error("Failed to save the suspicious file: {}" , file.getOriginalFilename(),e);
        }

    }
}
