package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.example.xianthebigfourtportfoliomanager.repository.PriceHistoryRepository;
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
import org.example.xianthebigfourtportfoliomanager.service.HoldingService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class TransactionController {

    private final TransactionRepository repository;
    private final HoldingService holdingService;
    private final PriceHistoryRepository priceHistoryRepository;

    public TransactionController(
            TransactionRepository repository,
            HoldingService holdingService,
            PriceHistoryRepository priceHistoryRepository
    ) {
        this.repository = repository;
        this.holdingService = holdingService;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @GetMapping("/transaction/{id}")
    public Transaction getTransaction(@PathVariable int id) {
        return repository.getTransactionById(id);
    }

    @GetMapping("/transactions/holding/{holdingId}")
    public List<Transaction> getByHoldingId(@PathVariable int holdingId) {
        return repository.getTransactionsByHoldingId(holdingId);
    }

    @PostMapping("/savetransaction")
    public String addTransaction(@RequestBody Transaction transaction) {
        applyYahooPriceIfNeeded(transaction);
        if (transaction.getPrice() == null || transaction.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return "Insert failed! Missing valid price (provide price or ensure Yahoo data exists for holding ticker).";
        }
        if (transaction.getTradeDate() == null) {
            transaction.setTradeDate(LocalDateTime.now());
        }

        Transaction saved = repository.save(transaction);
        if (saved != null) {
            return "Record added successfully!";
        } else {
            return "Insert failed!";
        }
    }

    @PatchMapping("/transaction/{id}")
    public String updateTransaction(@PathVariable int id, @RequestBody Transaction transaction) {
        transaction.setId(id);
        applyYahooPriceIfNeeded(transaction);
        if (transaction.getPrice() == null || transaction.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return "Update failed! Missing valid price (provide price or ensure Yahoo data exists for holding ticker).";
        }

        Transaction updated = repository.update(transaction);
        if (updated != null) {
            return "Record has been updated";
        } else {
            return "Update failed!";
        }
    }

    @DeleteMapping("/delete/transaction/{id}")
    public String deleteTransaction(@PathVariable int id) {
        int row = repository.deleteById(id);
        if (row == 1) {
            return "Delete successful " + id;
        } else {
            return "Delete failed!";
        }
    }

    private void applyYahooPriceIfNeeded(Transaction transaction) {
        if (transaction == null) {
            return;
        }
        if (transaction.getPrice() != null && transaction.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return;
        }

        Holding holding = holdingService.getHoldingById(transaction.getHoldingId());
        if (holding == null || holding.getTicker() == null || holding.getTicker().isBlank()) {
            return;
        }

        priceHistory latest = priceHistoryRepository.getLatestPriceByTicker(holding.getTicker().toUpperCase());
        if (latest != null && latest.getCloseprice() != null && latest.getCloseprice().compareTo(BigDecimal.ZERO) > 0) {
            transaction.setPrice(latest.getCloseprice());
        }
    }
}
