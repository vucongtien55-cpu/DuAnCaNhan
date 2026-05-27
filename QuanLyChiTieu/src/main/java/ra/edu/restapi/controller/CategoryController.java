package ra.edu.restapi.controller;

import ra.edu.restapi.model.Category;
import ra.edu.restapi.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin("*")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    // API lấy toàn bộ danh mục lên Web
    @GetMapping
    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    // API nhận dữ liệu từ Web gửi lên để thêm mới vào Database
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Category category) {
        try {
            Category saved = categoryRepository.save(category);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: Danh mục đã tồn tại hoặc dữ liệu sai!");
        }
    }
}
