package ra.edu.restapi.controller;

import ra.edu.restapi.dto.StatisticDTO;
import ra.edu.restapi.model.Transaction;
import ra.edu.restapi.repository.TransactionRepository;
import ra.edu.restapi.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin("*")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    // Lấy danh sách giao dịch theo User ID truyền từ Front-end lên
    @GetMapping
    public List<Transaction> getAllByUserId(@RequestParam Integer userId) {
        return transactionRepository.findByUserId(userId);
    }
}