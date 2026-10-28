package com.lms.storage;

import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(String subDirectory, String fileName, byte[] content) {
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new StorageException("Tên file lưu trữ không hợp lệ");
        }
        Path directory = resolve(subDirectory);
        Path target = resolve(subDirectory + "/" + fileName);
        try {
            Files.createDirectories(directory);
            // CREATE_NEW: thất bại nếu file đã tồn tại, không bao giờ ghi đè.
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException e) {
            throw new StorageException("File đã tồn tại, không được ghi đè", e);
        } catch (IOException e) {
            throw new StorageException("Không thể lưu file", e);
        }
        return root.relativize(target).toString().replace('\\', '/');
    }

    @Override
    public Resource open(String relativePath) {
        Path path = resolve(relativePath);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ResourceNotFoundException("Tệp tài liệu", relativePath);
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            throw new StorageException("Không thể xóa file", e);
        }
    }

    /** Chặn path traversal: mọi đường dẫn sau khi chuẩn hóa phải nằm trong thư mục gốc. */
    private Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new StorageException("Đường dẫn file không hợp lệ");
        }
        return path;
    }
}
