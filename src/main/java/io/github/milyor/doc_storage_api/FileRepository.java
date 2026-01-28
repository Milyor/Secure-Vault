package io.github.milyor.doc_storage_api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface FileRepository extends JpaRepository<FileDocument, UUID> { }
