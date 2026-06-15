package io.github.milyor.doc_storage_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name="users")
@Getter
@Setter
@NoArgsConstructor
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String password; // This field is treated as a stored hash
    @Column(nullable = false)
    private String role;

    @Override
    public String toString() {
        return "Users{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                '}'; // Password is not exposed in toString()
    }
}