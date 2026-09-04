package com.abhout.pocket_ledger_be.document;

import com.abhout.pocket_ledger_be.document.DTOs.DocumentIploadResponse;
import com.abhout.pocket_ledger_be.document.DTOs.DocumentResponse;
import com.abhout.pocket_ledger_be.document.enums.DocumentSource;
import com.abhout.pocket_ledger_be.document.enums.DocumentStatus;
import com.abhout.pocket_ledger_be.document.models.Document;
import com.abhout.pocket_ledger_be.document.models.DocumentFile;
import com.abhout.pocket_ledger_be.storage.DocumentNotFoundException;
import com.abhout.pocket_ledger_be.storage.DocumentStorageProvider;
import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.abhout.pocket_ledger_be.document.enums.DocumentStatus.isTabular;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private static final long maxFileSize = 20L * 1024 * 1024;
    private final DocumentRepository documentRepository;
    private final DocumentStorageProvider documentStorageProvider;

    @Transactional
    public DocumentIploadResponse uploadDocument(
        User user, List<MultipartFile> files, DocumentStatus requestedStatus
    ){
        List<DocumentResponse> storedDocList = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            String name = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
            byte[] content;
            try {
                content = file.getBytes();
            } catch (IOException e){
                errors.add(name + " cannot be read.");
                continue;
            }

            String validateError = validateFile(name, content);
            if (validateError != null) {
                errors.add(validateError);
                continue;
            }

            try {
                storedDocList.add(
                        storeDocument(
                                user,
                                name,
                                content,
                                file.getContentType(),
                                requestedStatus,
                                DocumentSource.UPLOAD
                        )
                );
            } catch (RuntimeException e) {
                errors.add(name + " could not be stored. Nothing was saved for it.");
            }
        }
        return DocumentIploadResponse.of(storedDocList, errors);
    }

    public DocumentFile getFileForDownload(User user, UUID id){
        Document document = documentRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(DocumentNotFoundException::new);
        byte[] comtent = documentStorageProvider.get(document.getObjectKey());
        return new DocumentFile(document.getFilename(), document.getMimeType(), comtent);
    }

    @Transactional
    public  void deleteDocument(User user, UUID id){
        Document document = documentRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(DocumentNotFoundException::new);
        documentStorageProvider.delete(document.getObjectKey());
        documentRepository.delete(document);
    }

    public DocumentResponse storeDocument(
            User user,
            String filename,
            byte[] content,
            String contentType,
            DocumentStatus status,
            DocumentSource source
    ){
        String mimeType = Optional.ofNullable(contentType).filter( s -> !s.isBlank())
                .orElse("application/octet-stream");

        DocumentStatus doccStatus = status != null ? status
                : DocumentStatus.defaultStatusFor(mimeType, filename);
        String objectKey = "users/" + user.getId() + "/uploads/"
                + UUID.randomUUID() + "-" + safeSegment(filename);
        documentStorageProvider.put(objectKey,content,mimeType);

        try {
            String storedFilename = filename.length() > 200 ? filename.substring(0,200) : filename;
            Document doc = new Document(
                    user,
                    storedFilename,
                    mimeType,
                    content.length,
                    objectKey,
                    doccStatus,
                    source
            );
            return  DocumentResponse.from(documentRepository.saveAndFlush(doc));
        } catch (RuntimeException e) {
            documentStorageProvider.delete(objectKey);
            throw e;
        }
    }

    private String validateFile(String fileName, byte[] fileContent){
        if (fileContent.length == 0) return fileName + " is empty.";
        if (fileContent.length > maxFileSize) {
            return "%s is %.1f MB. The limit is 20 MB per file.".formatted(
                    fileName, fileContent.length / 1024.0 / 1024.0);
        }
        return null;
    }

    public static String safeSegment(String value) {
        String cleaned = Normalizer.normalize(value,
                        Normalizer.Form.NFKD)
                .replaceAll("[^a-zA-Z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[-.]+|[-.]+$", "");
        if (cleaned.length() > 120) cleaned = cleaned.substring(0, 120);
        return cleaned.isEmpty() ? "file" : cleaned;
    }

    private static DocumentStatus defaultStatusFor(String mimeType, String filename) {
        return isTabular(mimeType, filename) ?
                DocumentStatus.STORED : DocumentStatus.REVIEW;
    }
}
