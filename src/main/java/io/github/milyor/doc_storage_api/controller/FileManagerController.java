package io.github.milyor.doc_storage_api.controller;

import io.github.milyor.doc_storage_api.service.FileStorageService;
import io.github.milyor.doc_storage_api.dto.ResponseFile;
import io.github.milyor.doc_storage_api.model.FileDocument;
import io.github.milyor.doc_storage_api.model.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
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
    public ResponseEntity<byte[]> downloadFile(@PathVariable String id,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        try {
            FileDocument fileDB = fileStorageService.getFile(id, principal.getUser().getId());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDB.getFileName() + "\"")
                    .body(fileDB.getData());
        } catch (FileNotFoundException e) {
            // Not found, or owned by another user — return 404 either way (no existence leak).
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}
