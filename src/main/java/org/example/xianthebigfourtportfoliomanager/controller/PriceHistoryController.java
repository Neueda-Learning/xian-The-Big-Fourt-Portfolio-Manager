package org.example.xianthebigfourtportfoliomanager.controller;

<<<<<<< HEAD
import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.example.xianthebigfourtportfoliomanager.repository.PriceHistoryRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
=======
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41

@RestController
public class PriceHistoryController {

<<<<<<< HEAD
    private final PriceHistoryRepository repository;

    public PriceHistoryController(PriceHistoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/price/{ticker}/{date}")
    public priceHistory getPriceByDate(
            @PathVariable String ticker,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return repository.getPriceByTickerAndDate(ticker, date);
    }

    @GetMapping("/prices/{ticker}")
    public List<priceHistory> getPricesByRange(
            @PathVariable String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return repository.getPricesByTickerAndRange(ticker, startDate, endDate);
    }

    @PostMapping("/saveprice")
    public String addPrice(@RequestBody priceHistory history) {
        priceHistory saved = repository.save(history);
        if (saved != null) {
            return "Record added successfully!";
        } else {
            return "Insert failed!";
        }
    }

    @DeleteMapping("/delete/price")
    public String deletePrice(
            @RequestParam String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        int row = repository.deleteByTickerAndDate(ticker, date);
        if (row == 1) {
            return "Delete successful " + ticker + " " + date;
        } else {
            return "Delete failed!";
        }
=======
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
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }
}
