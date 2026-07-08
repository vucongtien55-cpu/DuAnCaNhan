package org.example.quanlychitieu.repository;

import org.example.quanlychitieu.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserEmailOrderByDateDesc(String userEmail);
    List<Transaction> findByUserEmailAndType(String userEmail, String type);
}
