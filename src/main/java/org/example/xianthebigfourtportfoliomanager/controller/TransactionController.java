package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
<<<<<<< HEAD
import org.example.xianthebigfourtportfoliomanager.service.TransactionService;
=======
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TransactionController {

<<<<<<< HEAD
    /**
     * Eren issue: transaction APIs bypassed business rules, allowing state drift with holdings and cash.
     * Fix: delegate transaction CRUD to TransactionService to enforce cash constraints and holding recalculation.
     * Reviewer: GitHub Copilot (GPT-5.3-Codex).
     */

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
=======
    private final TransactionRepository repository;

    public TransactionController(TransactionRepository repository) {
        this.repository = repository;
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    @GetMapping("/transaction/{id}")
    public Transaction getTransaction(@PathVariable int id) {
<<<<<<< HEAD
        return service.getTransactionById(id);
=======
        return repository.getTransactionById(id);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    @GetMapping("/transactions/holding/{holdingId}")
    public List<Transaction> getByHoldingId(@PathVariable int holdingId) {
<<<<<<< HEAD
        return service.getTransactionsByHoldingId(holdingId);
=======
        return repository.getTransactionsByHoldingId(holdingId);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    @PostMapping("/savetransaction")
    public String addTransaction(@RequestBody Transaction transaction) {
<<<<<<< HEAD
        Transaction saved = service.create(transaction);
=======
        Transaction saved = repository.save(transaction);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
        if (saved != null) {
            return "Record added successfully!";
        } else {
            return "Insert failed!";
        }
    }

    @PatchMapping("/transaction/{id}")
    public String updateTransaction(@PathVariable int id, @RequestBody Transaction transaction) {
        transaction.setId(id);
<<<<<<< HEAD
        Transaction updated = service.update(transaction);
=======
        Transaction updated = repository.update(transaction);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
        if (updated != null) {
            return "Record has been updated";
        } else {
            return "Update failed!";
        }
    }

    @DeleteMapping("/delete/transaction/{id}")
    public String deleteTransaction(@PathVariable int id) {
<<<<<<< HEAD
        int row = service.deleteById(id);
=======
        int row = repository.deleteById(id);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
        if (row == 1) {
            return "Delete successful " + id;
        } else {
            return "Delete failed!";
        }
    }
}
