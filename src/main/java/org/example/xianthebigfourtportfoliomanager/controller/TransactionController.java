package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.entity.CashDepositRequest;
import org.example.xianthebigfourtportfoliomanager.entity.TradeRequest;
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

    @GetMapping("/portfolios/{id}/transactions")
    public List<Transaction> getByPortfolioId(@PathVariable int id) {
        return service.getTransactionsByPortfolioId(id);
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

    @PostMapping("/portfolios/{id}/trades/buy")
    public Transaction buy(@PathVariable int id, @RequestBody TradeRequest request) {
        return service.buy(id, request);
    }

    @PostMapping("/portfolios/{id}/trades/sell")
    public Transaction sell(@PathVariable int id, @RequestBody TradeRequest request) {
        return service.sell(id, request);
    }

    @PostMapping("/portfolios/{id}/cash/deposit")
    public Transaction depositCash(@PathVariable int id, @RequestBody CashDepositRequest request) {
        return service.depositCash(id, request);
    }

    @PatchMapping("/transaction/{id}")
    public String updateTransaction(@PathVariable int id, @RequestBody Transaction transaction) {
        throw new IllegalArgumentException("Transaction update is disabled. Transactions are immutable once created.");
    }

    @DeleteMapping("/delete/transaction/{id}")
    public String deleteTransaction(@PathVariable int id) {
        throw new IllegalArgumentException("Transaction delete is disabled. Transactions are immutable once created.");
    }
}
