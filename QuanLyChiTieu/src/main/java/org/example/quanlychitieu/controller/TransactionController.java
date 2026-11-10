package org.example.quanlychitieu.controller;

import org.example.quanlychitieu.entity.Transaction;
import org.example.quanlychitieu.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(@RequestParam String email) {
        return ResponseEntity.ok(transactionService.getTransactionsByUser(email));
    }

    @PostMapping
    public ResponseEntity<Transaction> addTransaction(@RequestBody Transaction transaction) {
        return ResponseEntity.ok(transactionService.addTransaction(transaction));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id, @RequestParam String email) {
        try {
            transactionService.deleteTransaction(id, email);
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<?> clearAll(@RequestParam String email) {
        try {
            transactionService.clearAllTransactions(email);
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
    public ResponseEntity<?> syncTransactions(@RequestParam String email, @RequestBody List<Map<String, Object>> transactionsData) {
        try {
            transactionService.clearAllTransactions(email);
            for (Map<String, Object> data : transactionsData) {
                Transaction tx = new Transaction();
                tx.setUserEmail(email);

                Object idObj = data.get("id");
                if (idObj != null) {
                    String idStr = idObj.toString();
                    if (!idStr.startsWith("tx-")) {
                        try {
                            tx.setId(Long.parseLong(idStr));
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }

                tx.setDate((String) data.get("date"));
                tx.setAmount(Double.parseDouble(data.get("amount").toString()));
                tx.setType(((String) data.get("type")).toLowerCase());
                tx.setCategory((String) data.get("category"));

                // Get note or notes
                String note = (String) data.get("notes");
                if (note == null) {
                    note = (String) data.get("note");
                }
                tx.setNote(note);

                transactionService.addTransaction(tx);
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
