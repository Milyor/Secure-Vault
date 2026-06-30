package io.github.milyor.doc_storage_api.service;

import io.github.milyor.doc_storage_api.model.FileDocument;
import io.github.milyor.doc_storage_api.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


@Service
public class FileStorageService {

    private final FileRepository fileRepository;
    private final S3StorageService s3;

    public FileStorageService(FileRepository fileRepository, S3StorageService s3) {
        this.fileRepository = fileRepository;
        this.s3 = s3;
    }

    public void saveFile(MultipartFile file, UUID ownerId) throws IOException {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String contentType = file.getContentType();
        long size = file.getSize();
        String s3Key = UUID.randomUUID().toString();
        boolean compress = CompressionPolicy.shouldCompress(contentType);

        if (compress) {
            storeCompressed(file, s3Key, contentType);
        } else {
            try (InputStream in = file.getInputStream()) {
                s3.put(s3Key, in, size, contentType);
            }
        }

        FileDocument fileDocument = new FileDocument(
                fileName, contentType, size, s3Key, compress, ownerId);
        fileRepository.save(fileDocument);
    }

    private void storeCompressed(MultipartFile file, String s3Key, String contentType) throws IOException {
        Path tmp = Files.createTempFile("vault-gz-", ".tmp");
        try {
            try (InputStream in = file.getInputStream();
                 GZIPOutputStream gz = new GZIPOutputStream(Files.newOutputStream(tmp))) {
                in.transferTo(gz);
            }
            try (InputStream gzIn = Files.newInputStream(tmp)) {
                s3.put(s3Key, gzIn, Files.size(tmp), contentType);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public FileDocument getFile(String id, UUID ownerId) throws FileNotFoundException {
        return fileRepository.findByIdAndOwnerId(UUID.fromString(id), ownerId)
                .orElseThrow(() -> new FileNotFoundException("File not found with id " + id));
    }

    public InputStream openDownloadStream(FileDocument doc) throws IOException {
        InputStream s3Stream = s3.openStream(doc.getS3Key());
        return doc.isCompressed() ? new GZIPInputStream(s3Stream) : s3Stream;
    }

    public Stream<FileDocument> getAllFiles(UUID ownerId) {
        return fileRepository.findByOwnerId(ownerId).stream();
    }
}
