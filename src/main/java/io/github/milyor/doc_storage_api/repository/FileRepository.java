package io.github.milyor.doc_storage_api.repository;

import io.github.milyor.doc_storage_api.model.FileDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface FileRepository extends JpaRepository<FileDocument, UUID> {

    // Only this owner's files.
    List<FileDocument> findByOwnerId(UUID ownerId);

    // Owner-scoped lookup: returns empty if the file exists but belongs to someone else,
    // so the caller can return 404 without leaking the file's existence.
    Optional<FileDocument> findByIdAndOwnerId(UUID id, UUID ownerId);
}
