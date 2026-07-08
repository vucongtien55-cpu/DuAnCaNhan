package org.example.quanlychitieu.service;

import org.example.quanlychitieu.entity.Transaction;
import org.example.quanlychitieu.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getTransactionsByUser(String email) {
        return transactionRepository.findByUserEmailOrderByDateDesc(email);
    }

    public Transaction addTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id, String email) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if (!tx.getUserEmail().equals(email)) {
            throw new SecurityException("Unauthorized access to delete this transaction");
        }
        transactionRepository.delete(tx);
    }

    public void clearAllTransactions(String email) {
        List<Transaction> list = transactionRepository.findByUserEmailOrderByDateDesc(email);
        transactionRepository.deleteAll(list);
    }
}
