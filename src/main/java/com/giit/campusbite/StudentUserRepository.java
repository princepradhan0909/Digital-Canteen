package com.giit.campusbite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentUserRepository extends JpaRepository<StudentUser, Long> {
    Optional<StudentUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
