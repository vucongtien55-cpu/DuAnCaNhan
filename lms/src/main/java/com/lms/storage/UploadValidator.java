package com.lms.storage;

import com.lms.exception.BusinessException;
import com.lms.exception.StorageException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Kiểm tra file tải lên: tối đa 20 MB, chỉ PDF/DOCX/PPTX/XLSX/ZIP, theo phần mở rộng và chữ ký nội dung.
 * Không tin Content-Type từ client; Content-Type trả về do server xác định theo phần mở rộng.
 */
@Component
public class UploadValidator {

    public static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final int MAX_FILE_NAME_LENGTH = 255;

    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] ZIP_SIGNATURE = {'P', 'K', 0x03, 0x04};

    private record AllowedType(String contentType, byte[] signature) {
    }

    private static final Map<String, AllowedType> ALLOWED_TYPES = Map.of(
            "pdf", new AllowedType("application/pdf", PDF_SIGNATURE),
            "docx", new AllowedType("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    ZIP_SIGNATURE),
            "pptx", new AllowedType("application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    ZIP_SIGNATURE),
            "xlsx", new AllowedType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    ZIP_SIGNATURE),
            "zip", new AllowedType("application/zip", ZIP_SIGNATURE));

    /** File đã qua kiểm tra: tên hiển thị đã làm sạch, phần mở rộng thường, kiểu nội dung và dữ liệu. */
    public record ValidatedUpload(String originalFileName, String extension, String contentType, byte[] content) {
    }

    public ValidatedUpload validate(MultipartFile file) {
        String fileName = sanitizeFileName(file);
        String extension = extensionOf(fileName);
        AllowedType allowed = ALLOWED_TYPES.get(extension);
        if (allowed == null) {
            throw new BusinessException("Chỉ chấp nhận file PDF, DOCX, PPTX, XLSX hoặc ZIP");
        }
        byte[] content = readContent(file);
        if (!startsWith(content, allowed.signature())) {
            throw new BusinessException("Nội dung file không khớp với định dạng ." + extension);
        }
        return new ValidatedUpload(fileName, extension, allowed.contentType(), content);
    }

    private String sanitizeFileName(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn file tải lên");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("File vượt quá dung lượng tối đa 20 MB");
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new BusinessException("Tên file không hợp lệ");
        }
        // Bỏ mọi thành phần đường dẫn (cả '/' lẫn '\') và ký tự điều khiển, chỉ giữ tên file cuối cùng.
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("\\p{Cntrl}", "").replaceAll("[<>:\"|?*]", "_").trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            throw new BusinessException("Tên file không hợp lệ");
        }
        if (name.length() > MAX_FILE_NAME_LENGTH) {
            throw new BusinessException("Tên file quá dài (tối đa " + MAX_FILE_NAME_LENGTH + " ký tự)");
        }
        return name;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            throw new BusinessException("File phải có phần mở rộng (PDF, DOCX, PPTX, XLSX hoặc ZIP)");
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readContent(MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new StorageException("Không thể đọc file tải lên", e);
        }
        if (content.length == 0) {
            throw new BusinessException("File tải lên trống");
        }
        if (content.length > MAX_FILE_SIZE) {
            throw new BusinessException("File vượt quá dung lượng tối đa 20 MB");
        }
        return content;
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
