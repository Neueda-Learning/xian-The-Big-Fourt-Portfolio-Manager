package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TransactionController {

    private final TransactionRepository repository;

    public TransactionController(TransactionRepository repository) {
        this.repository = repository;
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
}
