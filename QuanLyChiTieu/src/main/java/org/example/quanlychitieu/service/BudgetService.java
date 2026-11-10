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

    public List<Budget> getBudgetsByUser(String email) {
        return budgetRepository.findByUserEmail(email);
    }

    public Budget saveOrUpdateBudget(Budget budget) {
        Optional<Budget> existing = budgetRepository.findByUserEmailAndCategory(budget.getUserEmail(), budget.getCategory());
        if (existing.isPresent()) {
            Budget current = existing.get();
            current.setLimitAmount(budget.getLimitAmount());
            return budgetRepository.save(current);
        }
        return budgetRepository.save(budget);
    }

    public void deleteBudget(Long id, String email) {
        Budget b = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        if (!b.getUserEmail().equals(email)) {
            throw new SecurityException("Unauthorized access to delete this budget");
        }
        budgetRepository.delete(b);
    }
}
