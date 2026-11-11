package org.example.quanlychitieu.service;

import org.example.quanlychitieu.entity.Transaction;
import org.example.quanlychitieu.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho TransactionService (giao dịch thu/chi).
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction tx(Long id, String email) {
        return new Transaction(id, email, "2026-11-10", 50_000.0, "expense", "An uong", "Com trua");
    }

    @Test
    @DisplayName("Xóa giao dịch của chính mình -> xóa thành công")
    void delete_ownTransaction_deletes() {
        Transaction t = tx(1L, "tien@example.com");
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(t));

        transactionService.deleteTransaction(1L, "tien@example.com");

        verify(transactionRepository).delete(t);
    }

    @Test
    @DisplayName("Xóa giao dịch của người khác -> ném SecurityException, không xóa")
    void delete_transactionOfAnotherUser_throwsSecurityException() {
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(tx(2L, "nguoikhac@example.com")));

        assertThatThrownBy(() -> transactionService.deleteTransaction(2L, "tien@example.com"))
                .isInstanceOf(SecurityException.class);
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa toàn bộ giao dịch -> chỉ xóa giao dịch của user đó")
    void clearAll_deletesOnlyUsersTransactions() {
        List<Transaction> list = List.of(tx(1L, "tien@example.com"), tx(3L, "tien@example.com"));
        when(transactionRepository.findByUserEmailOrderByDateDesc("tien@example.com")).thenReturn(list);

        transactionService.clearAllTransactions("tien@example.com");

        verify(transactionRepository).deleteAll(list);
    }
}
