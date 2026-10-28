package com.lms.storage;

import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalFileStorage(tempDir.toString());
    }

    @Test
    @DisplayName("Lưu file trả về đường dẫn tương đối và ghi đúng nội dung")
    void store_success() throws IOException {
        String path = storage.store("sections/1", "a.pdf", "hello".getBytes(StandardCharsets.UTF_8));

        assertThat(path).isEqualTo("sections/1/a.pdf");
        assertThat(Files.readString(tempDir.resolve("sections/1/a.pdf"))).isEqualTo("hello");
    }

    @Test
    @DisplayName("Lưu file trùng tên -> StorageException, file cũ không bị ghi đè")
    void store_neverOverwrites() throws IOException {
        storage.store("sections/1", "a.pdf", "original".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storage.store("sections/1", "a.pdf", "new".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(StorageException.class);

        assertThat(Files.readString(tempDir.resolve("sections/1/a.pdf"))).isEqualTo("original");
    }

    @Test
    @DisplayName("Tên file chứa thành phần đường dẫn -> StorageException")
    void store_rejectsTraversalInFileName() {
        assertThatThrownBy(() -> storage.store("sections/1", "../evil.pdf", new byte[]{1}))
                .isInstanceOf(StorageException.class);
        assertThatThrownBy(() -> storage.store("sections/1", "dir/evil.pdf", new byte[]{1}))
                .isInstanceOf(StorageException.class);
        assertThatThrownBy(() -> storage.store("sections/1", "dir\\evil.pdf", new byte[]{1}))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Thư mục con thoát khỏi thư mục gốc -> StorageException, không tạo file bên ngoài")
    void store_rejectsTraversalInDirectory() {
        assertThatThrownBy(() -> storage.store("../../outside", "a.pdf", new byte[]{1}))
                .isInstanceOf(StorageException.class);

        assertThat(tempDir.getParent().resolve("outside")).doesNotExist();
    }

    @Test
    @DisplayName("Mở file đã lưu")
    void open_success() throws IOException {
        storage.store("sections/1", "a.pdf", "hello".getBytes(StandardCharsets.UTF_8));

        Resource resource = storage.open("sections/1/a.pdf");

        assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    @DisplayName("Mở file không còn trên máy chủ -> ResourceNotFoundException")
    void open_missing() {
        assertThatThrownBy(() -> storage.open("sections/1/missing.pdf"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Mở đường dẫn thoát thư mục gốc hoặc đường dẫn tuyệt đối -> StorageException")
    void open_rejectsTraversal() {
        assertThatThrownBy(() -> storage.open("../secret.txt")).isInstanceOf(StorageException.class);
        assertThatThrownBy(() -> storage.open(tempDir.getParent().resolve("secret.txt").toString()))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Xóa file; xóa file không tồn tại không báo lỗi")
    void delete() {
        String path = storage.store("sections/1", "a.pdf", new byte[]{1});

        storage.delete(path);
        storage.delete(path);

        assertThat(tempDir.resolve("sections/1/a.pdf")).doesNotExist();
    }

    @Test
    @DisplayName("Xóa đường dẫn thoát thư mục gốc -> StorageException")
    void delete_rejectsTraversal() {
        assertThatThrownBy(() -> storage.delete("../secret.txt")).isInstanceOf(StorageException.class);
    }
}
