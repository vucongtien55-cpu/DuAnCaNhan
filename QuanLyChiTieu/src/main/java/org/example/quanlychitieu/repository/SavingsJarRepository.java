package org.example.quanlychitieu.repository;

import org.example.quanlychitieu.entity.SavingsJar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsJarRepository extends JpaRepository<SavingsJar, Long> {
    List<SavingsJar> findByUserEmail(String userEmail);
    Optional<SavingsJar> findByUserEmailAndName(String userEmail, String name);
}
