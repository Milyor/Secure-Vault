package io.github.milyor.doc_storage_api.service;

import io.github.milyor.doc_storage_api.model.FileDocument;
import io.github.milyor.doc_storage_api.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;


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
        FileDocument fileDocument = new FileDocument(
                fileName,
                file.getContentType(),
                file.getBytes()
        );
        fileRepository.save(fileDocument);
    }

    public FileDocument getFile(String id) throws FileNotFoundException {
       return fileRepository.findById(UUID.fromString(id)).orElseThrow(() -> new FileNotFoundException("File not found with id " + id));
    }
    public Stream<FileDocument> getAllFiles() {
        return fileRepository.findAll().stream();
    }
}
