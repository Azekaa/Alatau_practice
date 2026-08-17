package Alatau.practice.service;

import Alatau.practice.dto.PdfResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.util.ArrayList;
import java.util.List;

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

        }catch (Exception e){
            log.error("Error reading PDF: {}", file.getOriginalFilename(),e);
        }
        return detectedObject;
    }

    private void checkDictionary(COSDictionary catalog, List<String> detectedObject) {
        for (COSName key : catalog.keySet()){
            String objectName = "/" + key.getName();
            if (Dangerous_objects.contains(objectName)){
                detectedObject.add(objectName);
                log.warn("unsafe object detected: {}", objectName);
            }
        COSBase value = catalog.getDictionaryObject(key);
            if (value instanceof COSDictionary nestedDict){
                checkDictionary(nestedDict,detectedObject);
            }
        }
    }


}
