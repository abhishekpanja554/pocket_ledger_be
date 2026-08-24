package com.abhout.pocket_ledger_be.document;

import com.abhout.pocket_ledger_be.document.DTOs.DocumentIploadResponse;
import com.abhout.pocket_ledger_be.document.enums.DocumentStatus;
import com.abhout.pocket_ledger_be.document.models.Document;
import com.abhout.pocket_ledger_be.document.models.DocumentFile;
import com.abhout.pocket_ledger_be.user.UserPrincipal;
import com.abhout.pocket_ledger_be.web.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentIploadResponse>> uploadDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(value = "files",required = false) List<MultipartFile> files,
            @RequestParam(value = "file",required = false) List<MultipartFile> file,
            @RequestParam(value = "status",required = false) String status
    ){
        if (userPrincipal == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "You are unauthorized"));
        }

        List<MultipartFile> filesMerged = mergeFiles(files,file);
        if (filesMerged.isEmpty()){
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("NO_FILES", "No files provided"));
        }

        if(filesMerged.size() > 20){
            return ResponseEntity.badRequest().body(ApiResponse.error("TOO_MANY_FILES",
                    "Upload at most 20 files at a time."));
        }

        DocumentStatus requestedStatus = getStatusFromString(status);
        DocumentIploadResponse response = documentService.uploadDocument(
                userPrincipal.getUser(),filesMerged,requestedStatus);
        boolean allFailed = response.documents().isEmpty() &&
                !response.errors().isEmpty();
        return ResponseEntity.status(allFailed ? HttpStatus.BAD_REQUEST : HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<?> downloadDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id
    ){
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error("UNAUTHORIZED", "You are unauthorized"));
        }
        DocumentFile file = documentService.getFileForDownload(userPrincipal.getUser(),id);
        String safeFilename = file.filename().replace("\"", "");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + safeFilename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.mimeType())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.content()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id
    ){
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "You are unauthorized"));
        }
        documentService.deleteDocument(userPrincipal.getUser(), id);
        return  ResponseEntity.ok(ApiResponse.success());
    }

    private List<MultipartFile> mergeFiles(
            List<MultipartFile> files, List<MultipartFile> file
    ) {
        List<MultipartFile> merged = new ArrayList<>();
        if (files != null) merged.addAll(files);
        if (file != null) merged.addAll(file);
        return merged;
    }

    private DocumentStatus getStatusFromString(String status) {
        if (status == null) return null;
        try {
            return
                    DocumentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
