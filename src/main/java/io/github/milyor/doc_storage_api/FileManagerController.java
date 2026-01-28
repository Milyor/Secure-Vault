package io.github.milyor.doc_storage_api;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.logging.Level;
import java.util.logging.Logger;

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

    @GetMapping("/dowload")
    public ResponseEntity<Resource> downloadFile(@RequestParam("file") String filename) {
        fileStorageService.getDownloadFile()
    }
}
