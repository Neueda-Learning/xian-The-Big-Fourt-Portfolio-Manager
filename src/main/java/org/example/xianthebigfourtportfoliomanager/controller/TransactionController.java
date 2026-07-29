package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TransactionController {

    /**
     * Eren issue: transaction APIs bypassed business rules, allowing state drift with holdings and cash.
     * Fix: delegate transaction CRUD to TransactionService to enforce cash constraints and holding recalculation.
     * Reviewer: GitHub Copilot (GPT-5.3-Codex).
     */

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping("/transaction/{id}")
    public Transaction getTransaction(@PathVariable int id) {
        return service.getTransactionById(id);
    }

    @GetMapping("/transactions/holding/{holdingId}")
    public List<Transaction> getByHoldingId(@PathVariable int holdingId) {
        return service.getTransactionsByHoldingId(holdingId);
    }

    @PostMapping("/savetransaction")
    public String addTransaction(@RequestBody Transaction transaction) {
        Transaction saved = service.create(transaction);
        if (saved != null) {
            return "Record added successfully!";
        } else {
            return "Insert failed!";
        }
    }

    @PatchMapping("/transaction/{id}")
    public String updateTransaction(@PathVariable int id, @RequestBody Transaction transaction) {
        transaction.setId(id);
        Transaction updated = service.update(transaction);
        if (updated != null) {
            return "Record has been updated";
        } else {
            return "Update failed!";
        }
    }

    @DeleteMapping("/delete/transaction/{id}")
    public String deleteTransaction(@PathVariable int id) {
        int row = service.deleteById(id);
        if (row == 1) {
            return "Delete successful " + id;
        } else {
            return "Delete failed!";
        }
    }
}
