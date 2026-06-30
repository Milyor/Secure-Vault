package io.github.milyor.doc_storage_api.service;

import io.github.milyor.doc_storage_api.model.FileDocument;
import io.github.milyor.doc_storage_api.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public void saveFile(MultipartFile file, UUID ownerId) throws IOException {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        FileDocument fileDocument = new FileDocument(
                fileName,
                file.getContentType(),
                file.getBytes(),
                ownerId
        );
        fileRepository.save(fileDocument);
    }

    // Owner-scoped: a file owned by someone else is treated as not found (no existence leak).
    // @Transactional: Postgres OID large objects can only be read inside a transaction.
    // We force the lob to materialize here (getData()) so the bytes are available to the
    // controller after the transaction closes. NOTE: temporary — Phase 1b moves bytes to S3
    // and removes the OID blob entirely, making this annotation unnecessary.
    @Transactional(readOnly = true)
    public FileDocument getFile(String id, UUID ownerId) throws FileNotFoundException {
        FileDocument doc = fileRepository.findByIdAndOwnerId(UUID.fromString(id), ownerId)
                .orElseThrow(() -> new FileNotFoundException("File not found with id " + id));
        // touch the lob inside the tx so it's fully read before auto-commit resumes
        byte[] data = doc.getData();
        if (data != null) {
            doc.setSize(data.length);
        }
        return doc;
    }

    // @Transactional only needed because the OID lob is read during entity hydration.
    // /files uses metadata only, so no lob touch needed here. Temporary — see Phase 1b.
    @Transactional(readOnly = true)
    public Stream<FileDocument> getAllFiles(UUID ownerId) {
        return fileRepository.findByOwnerId(ownerId).stream();
    }
}
