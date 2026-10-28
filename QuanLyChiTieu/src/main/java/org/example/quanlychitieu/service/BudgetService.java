package org.example.quanlychitieu.service;

import org.example.quanlychitieu.entity.Budget;
import org.example.quanlychitieu.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class BudgetService {

    private final BudgetRepository budgetRepository;

    @Autowired
    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    //Láy danh sách ngân sách của nguofi dùng theo email
    public List<Budget> getBudgetsByUser(String email) {
        return budgetRepository.findByUserEmail(email);
    }
    //Lưu ngân sách mới hặc cập nhật ngân sách đã tồn tại


    // Lấy danh sách ngân sách của người dùng theo email.
    public List<Budget> getBudgetsByUser(String email) {
        return budgetRepository.findByUserEmail(email);
    }

    // Lưu ngân sách mới hoặc cập nhật ngân sách đã tồn tại.
 f39ea5a373c66891f211fb7c9f6d1bcf5f0333d4
    public Budget saveOrUpdateBudget(Budget budget) {
        Optional<Budget> existing = budgetRepository.findByUserEmailAndCategory(budget.getUserEmail(), budget.getCategory());
        if (existing.isPresent()) {
            Budget current = existing.get();
            current.setLimitAmount(budget.getLimitAmount());
            return budgetRepository.save(current);
        }
        return budgetRepository.save(budget);
    }

    //Xóa ngân sách theo id sau khi kiểm tra quyền sở hữu


    // Xóa ngân sách theo ID sau khi kiểm tra quyền sở hữu
 
    public void deleteBudget(Long id, String email) {
        Budget b = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ngân sách."));
        if (!b.getUserEmail().equals(email)) {
            throw new SecurityException("Không có quyền xóa ngân sách này");
        }
        budgetRepository.delete(b);
    }
}
