package io.github.milyor.doc_storage_api;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;


@Service
public class FileStorageService {

    private final FileRepository fileRepository;

    public FileStorageService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public void saveFile(MultipartFile file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        FileDocument  fileDocument = new FileDocument(
                fileName,
                file.getContentType(),
                file.getBytes()
        );
        fileRepository.save(fileDocument);
    }

    public FileDocument getDownloadFile(UUID id) throws FileNotFoundException {
        Optional<FileDocument> fileDocument = fileRepository.findById(id);

        if (fileDocument.isPresent()) {
            return fileDocument.get();
        } else  {
            throw new FileNotFoundException("File not found with id " + id);
        }
    }
}
