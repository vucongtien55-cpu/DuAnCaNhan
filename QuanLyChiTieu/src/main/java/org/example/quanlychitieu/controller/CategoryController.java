package org.example.quanlychitieu.controller;

import org.example.quanlychitieu.entity.Category;
import org.example.quanlychitieu.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<Category>> getCategories(@RequestParam String email) {
        return ResponseEntity.ok(categoryService.getCategoriesByUser(email));
    }

    @PostMapping
    public ResponseEntity<Category> addCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryService.addCategory(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id, @RequestParam String email) {
        try {
            categoryService.deleteCategory(id, email);
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncCategories(@RequestParam String email, @RequestBody List<Map<String, Object>> categoriesData) {
        try {
            List<Category> existing = categoryService.getCategoriesByUser(email);
            for (Category c : existing) {
                categoryService.deleteCategory(c.getId(), email);
            }
            for (Map<String, Object> data : categoriesData) {
                Category c = new Category();
                c.setUserEmail(email);
                c.setName((String) data.get("name"));
                c.setType((String) data.get("type"));
                c.setIcon((String) data.get("icon"));
                c.setColor((String) data.get("color"));

                categoryService.addCategory(c);
            }
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
