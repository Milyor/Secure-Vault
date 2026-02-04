package io.github.milyor.doc_storage_api.repository;

import io.github.milyor.doc_storage_api.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRep extends JpaRepository<Users, UUID> {
    Users findByUsername(String username);
}
