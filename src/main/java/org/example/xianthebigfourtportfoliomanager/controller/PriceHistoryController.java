package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class PriceHistoryController {

    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    public PriceHistoryController(HoldingRepository holdingRepository, TransactionRepository transactionRepository) {
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/price/current/holding/{holdingId}")
    public Map<String, Object> getCurrentPrice(@PathVariable int holdingId) {
        Holding holding = holdingRepository.getHoldingById(holdingId);
        if (holding == null) {
            return null;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("holdingId", holdingId);
        response.put("ticker", holding.getTicker());
        response.put("currentPrice", holding.getPurchasePrice());
        return response;
    }

    @GetMapping("/price/after-transaction/{transactionId}")
    public Map<String, Object> getPriceAfterTransaction(@PathVariable int transactionId) {
        Transaction transaction = transactionRepository.getTransactionById(transactionId);
        if (transaction == null) {
            return null;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("transactionId", transactionId);
        response.put("holdingId", transaction.getHoldingId());
        response.put("priceAfterTransaction", transaction.getPrice());
        response.put("tradeDate", transaction.getTradeDate());
        return response;
    }
}
