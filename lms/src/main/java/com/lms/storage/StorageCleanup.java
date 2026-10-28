package com.lms.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Giữ file và bản ghi database nhất quán quanh ranh giới giao dịch. */
@Slf4j
public final class StorageCleanup {

    private StorageCleanup() {
    }

    /** Xóa file, chỉ ghi log nếu thất bại (không làm hỏng nghiệp vụ đang chạy). */
    public static void deleteQuietly(FileStorage storage, String path) {
        try {
            storage.delete(path);
        } catch (RuntimeException e) {
            log.warn("Không thể xóa file '{}', cần dọn thủ công", path, e);
        }
    }

    /** Nếu giao dịch hiện tại rollback, xóa file vừa ghi để không để lại file mồ côi. */
    public static void deleteOnRollback(FileStorage storage, String path) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteQuietly(storage, path);
                    }
                }
            });
        }
    }

    /** Chạy sau khi giao dịch commit; nếu không có giao dịch thì chạy ngay. */
    public static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
