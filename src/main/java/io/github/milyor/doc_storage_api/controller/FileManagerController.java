package io.github.milyor.doc_storage_api.controller;

import io.github.milyor.doc_storage_api.service.FileStorageService;
import io.github.milyor.doc_storage_api.dto.ResponseFile;
import io.github.milyor.doc_storage_api.model.FileDocument;
import io.github.milyor.doc_storage_api.model.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
public class FileManagerController {

    @Autowired
    private FileStorageService fileStorageService;
    private static final Logger log = Logger.getLogger(FileManagerController.class.getName());

    @PostMapping("/upload-file")
    public boolean uploadFile(@RequestParam("file") MultipartFile file,
                              @AuthenticationPrincipal UserPrincipal principal) {
        try{
        fileStorageService.saveFile(file, principal.getUser().getId());
        return true;
        } catch(Exception e){
            log.log(Level.SEVERE, "Exception occurred while trying to upload file", e );
        }
        return false;
    }

    @GetMapping("/files")
    public ResponseEntity<List<ResponseFile>> getListOfFiles(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID ownerId = principal.getUser().getId();
        List<ResponseFile> files = fileStorageService.getAllFiles(ownerId).map(dbFile -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/download/")
                    .path(dbFile.getId().toString())
                    .toUriString();
            return new ResponseFile(
                    dbFile.getId().toString(),
                    dbFile.getFileName(),
                    dbFile.getSize(),
                    fileDownloadUri,
                    dbFile.getContentType()
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<StreamingResponseBody> downloadFile(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileDocument fileDB;
        try {
            fileDB = fileStorageService.getFile(id, principal.getUser().getId());
        } catch (FileNotFoundException e) {
            // 404 (not 403) so a non-owned file's existence isn't revealed
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        StreamingResponseBody body = outputStream -> {
            try (InputStream in = fileStorageService.openDownloadStream(fileDB)) {
                in.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileDB.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

}
