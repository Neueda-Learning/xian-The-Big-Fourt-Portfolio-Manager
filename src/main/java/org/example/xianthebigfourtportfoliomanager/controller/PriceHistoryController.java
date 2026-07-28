package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.example.xianthebigfourtportfoliomanager.repository.PriceHistoryRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class PriceHistoryController {

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
    }
}
