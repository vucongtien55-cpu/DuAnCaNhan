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

    // Lấy danh sách giao dịch của một người dùng theo email.
    public List<Transaction> getTransactionsByUser(String email) {
        return transactionRepository.findByUserEmailOrderByDateDesc(email);
    }

    //Thêm giao dịch
    public Transaction addTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    //Xóa giao dịch
    public void deleteTransaction(Long id, String email) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch"));
        if (!tx.getUserEmail().equals(email)) {
            throw new SecurityException("Bạn không có quyền xóa giao dịch này");
        }
        transactionRepository.delete(tx);
    }

    //Xóa tất cả giao dịch của người dùng theo email
    public void clearAllTransactions(String email) {
        List<Transaction> list = transactionRepository.findByUserEmailOrderByDateDesc(email);
        transactionRepository.deleteAll(list);
    }
}
