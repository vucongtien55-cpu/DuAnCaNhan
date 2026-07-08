package org.example.quanlychitieu.repository;

import org.example.quanlychitieu.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserEmail(String userEmail);
    Optional<Budget> findByUserEmailAndCategory(String userEmail, String category);
}
