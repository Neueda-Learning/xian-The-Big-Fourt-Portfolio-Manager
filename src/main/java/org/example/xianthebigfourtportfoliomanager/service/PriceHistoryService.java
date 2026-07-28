package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.example.xianthebigfourtportfoliomanager.repository.PriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;

    public PriceHistoryService(PriceHistoryRepository priceHistoryRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public priceHistory getPriceByTickerAndDate(String ticker, LocalDate date) {
        return priceHistoryRepository.getPriceByTickerAndDate(ticker, date);
    }

    public List<priceHistory> getPricesByTickerAndRange(String ticker, LocalDate startDate, LocalDate endDate) {
        return priceHistoryRepository.getPricesByTickerAndRange(ticker, startDate, endDate);
    }

    @Transactional
    public priceHistory create(priceHistory history) {
        return priceHistoryRepository.save(history);
    }

    @Transactional
    public int deleteByTickerAndDate(String ticker, LocalDate date) {
        return priceHistoryRepository.deleteByTickerAndDate(ticker, date);
    }
}

