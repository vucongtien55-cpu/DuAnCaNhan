package com.lms.security.probe;

import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.StorageException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint thăm dò chỉ tồn tại trong test: dùng để kiểm chứng phân quyền theo vai trò và định dạng lỗi thống nhất
 * trước khi các Controller nghiệp vụ được viết. Không nằm trong mã production.
 */
@RestController
@RequestMapping("/api/probe")
public class SecurityProbeController {

    public record Who(String userId, List<String> authorities) {
    }

    public record EchoRequest(@NotBlank String name, @Min(1) int age) {
    }

    private Who who(Authentication authentication) {
        return new Who(authentication.getName(),
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
    }

    @GetMapping("/authenticated")
    public Who authenticated(Authentication authentication) {
        return who(authentication);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Who admin(Authentication authentication) {
        return who(authentication);
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    public Who staff(Authentication authentication) {
        return who(authentication);
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public Who student(Authentication authentication) {
        return who(authentication);
    }

    @GetMapping("/business-error")
    public void businessError() {
        throw new BusinessException("Vi phạm quy tắc nghiệp vụ");
    }

    @GetMapping("/not-found")
    public void notFound() {
        throw new ResourceNotFoundException("Khóa học", 99);
    }

    @GetMapping("/forbidden")
    public void forbidden() {
        throw new ForbiddenException("Không được phép");
    }

    @GetMapping("/storage-error")
    public void storageError() {
        throw new StorageException("Disk /var/lms/uploads đầy");
    }

    @GetMapping("/boom")
    public void boom() {
        throw new IllegalStateException("password=hunter2 jdbc:postgresql://db-noi-bo:5432/lms");
    }

    @PostMapping("/echo")
    public EchoRequest echo(@Valid @RequestBody EchoRequest request) {
        return request;
    }
}
