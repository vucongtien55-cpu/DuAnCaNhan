package ra.edu.restapi.service;


import ra.edu.restapi.dto.StatisticDTO;
import ra.edu.restapi.model.Transaction;
import ra.edu.restapi.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public List<Transaction> getAllByUserId(Integer userId) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getUser() != null && t.getUser().getId().equals(userId))
                .collect(Collectors.toList());
    }

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public List<StatisticDTO> getStatisticsByUserId(Integer userId) {
        List<Transaction> transactions = getAllByUserId(userId);
        Map<String, Double> statsMap = new HashMap<>();

        for (Transaction t : transactions) {
            if (t.getCategory() != null && "EXPENSE".equals(t.getCategory().getType())) {
                String catName = t.getCategory().getName();
                statsMap.put(catName, statsMap.getOrDefault(catName, 0.0) + t.getAmount());
            }
        }

        List<StatisticDTO> resultList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : statsMap.entrySet()) {
            resultList.add(new StatisticDTO(entry.getKey(), entry.getValue()));
        }
        return resultList;
    }
}