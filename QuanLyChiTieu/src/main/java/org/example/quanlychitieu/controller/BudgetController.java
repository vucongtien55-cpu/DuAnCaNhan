package org.example.quanlychitieu.controller;

import org.example.quanlychitieu.entity.Budget;
import org.example.quanlychitieu.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "*")
public class BudgetController {

    private final BudgetService budgetService;

    @Autowired
    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ResponseEntity<List<Budget>> getBudgets(@RequestParam String email) {
        return ResponseEntity.ok(budgetService.getBudgetsByUser(email));
    }

    @PostMapping
    public ResponseEntity<Budget> saveBudget(@RequestBody Budget budget) {
        return ResponseEntity.ok(budgetService.saveOrUpdateBudget(budget));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBudget(@PathVariable Long id, @RequestParam String email) {
        try {
            budgetService.deleteBudget(id, email);
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
    public ResponseEntity<?> syncBudgets(@RequestParam String email, @RequestBody List<Map<String, Object>> budgetsData) {
        try {
            List<Budget> existing = budgetService.getBudgetsByUser(email);
            for (Budget b : existing) {
                budgetService.deleteBudget(b.getId(), email);
            }
            for (Map<String, Object> data : budgetsData) {
                Budget b = new Budget();
                b.setUserEmail(email);
                b.setCategory((String) data.get("category"));

                Object limitObj = data.get("limit");
                if (limitObj == null) {
                    limitObj = data.get("limitAmount");
                }
                b.setLimitAmount(Double.parseDouble(limitObj.toString()));

                budgetService.saveOrUpdateBudget(b);
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
