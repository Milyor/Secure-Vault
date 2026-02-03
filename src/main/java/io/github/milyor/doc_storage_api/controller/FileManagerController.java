package io.github.milyor.doc_storage_api.controller;

import io.github.milyor.doc_storage_api.service.FileStorageService;
import io.github.milyor.doc_storage_api.dto.ResponseFile;
import io.github.milyor.doc_storage_api.model.FileDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
public class FileManagerController {

    @Autowired
    private FileStorageService fileStorageService;
    private static final Logger log = Logger.getLogger(FileManagerController.class.getName());

    @PostMapping("/upload-file")
    public boolean uploadFile(@RequestParam("file") MultipartFile file) {
        try{
        fileStorageService.saveFile(file);
        return true;
        } catch(Exception e){
            log.log(Level.SEVERE, "Exception occurred while trying to upload file", e );
        }
        return false;
    }

    @GetMapping("/files")
    public ResponseEntity<List<ResponseFile>> getListOfFiles() {
        List<ResponseFile> files = fileStorageService.getAllFiles().map(dbFile -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/download/")
                    .path(dbFile.getId().toString())
                    .toUriString();
            return new ResponseFile(
                    dbFile.getId().toString(),
                    dbFile.getFileName(),
                    (long) dbFile.getData().length,
                    fileDownloadUri,
                    dbFile.getContentType()
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String id) throws IOException {
        FileDocument fileDB = fileStorageService.getFile(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDB.getFileName() + "\"")
                .body(fileDB.getData());
    }

}
