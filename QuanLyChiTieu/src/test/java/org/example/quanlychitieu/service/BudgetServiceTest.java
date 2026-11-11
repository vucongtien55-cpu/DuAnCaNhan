package org.example.quanlychitieu.service;

import org.example.quanlychitieu.entity.Budget;
import org.example.quanlychitieu.repository.BudgetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho BudgetService (ngân sách theo danh mục).
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    @DisplayName("Ngân sách cho danh mục đã có -> chỉ cập nhật hạn mức, không tạo bản ghi mới")
    void saveOrUpdate_existingCategory_updatesLimit() {
        Budget existing = new Budget(5L, "tien@example.com", "An uong", 1_000_000.0);
        Budget request = new Budget(null, "tien@example.com", "An uong", 2_500_000.0);
        when(budgetRepository.findByUserEmailAndCategory("tien@example.com", "An uong"))
                .thenReturn(Optional.of(existing));
        when(budgetRepository.save(existing)).thenReturn(existing);

        Budget result = budgetService.saveOrUpdateBudget(request);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getLimitAmount()).isEqualTo(2_500_000.0);
    }

    @Test
    @DisplayName("Ngân sách cho danh mục mới -> lưu bản ghi mới")
    void saveOrUpdate_newCategory_savesNewBudget() {
        Budget request = new Budget(null, "tien@example.com", "Di lai", 500_000.0);
        when(budgetRepository.findByUserEmailAndCategory("tien@example.com", "Di lai"))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(request)).thenReturn(request);

        budgetService.saveOrUpdateBudget(request);

        verify(budgetRepository).save(request);
    }

    @Test
    @DisplayName("Xóa ngân sách của người khác -> ném SecurityException, không xóa")
    void delete_budgetOfAnotherUser_throwsSecurityException() {
        Budget budget = new Budget(7L, "nguoikhac@example.com", "Mua sam", 300_000.0);
        when(budgetRepository.findById(7L)).thenReturn(Optional.of(budget));

        assertThatThrownBy(() -> budgetService.deleteBudget(7L, "tien@example.com"))
                .isInstanceOf(SecurityException.class);
        verify(budgetRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa ngân sách không tồn tại -> ném IllegalArgumentException")
    void delete_unknownBudget_throwsIllegalArgument() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.deleteBudget(99L, "tien@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Budget not found");
    }
}
