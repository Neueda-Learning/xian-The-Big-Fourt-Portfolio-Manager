package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getTransactionsByHoldingId(int holdingId) {
        return transactionRepository.getTransactionsByHoldingId(holdingId);
    }

    public Transaction getTransactionById(int id) {
        return transactionRepository.getTransactionById(id);
    }

    @Transactional
    public Transaction create(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction update(Transaction transaction) {
        return transactionRepository.update(transaction);
    }

    @Transactional
    public int deleteById(int id) {
        return transactionRepository.deleteById(id);
    }
}

