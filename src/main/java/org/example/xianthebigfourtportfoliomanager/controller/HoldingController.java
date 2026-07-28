package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.AssetType;
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.example.xianthebigfourtportfoliomanager.service.YahooFinanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HoldingController {

    private final HoldingRepository repository;
    private final YahooFinanceService yahooFinanceService;

    public HoldingController(HoldingRepository repository, YahooFinanceService yahooFinanceService) {
        this.repository = repository;
        this.yahooFinanceService = yahooFinanceService;
    }

    @GetMapping("/holding/{id}")
    public Holding getHolding(@PathVariable int id) {
        return repository.getHoldingById(id);
    }

    @GetMapping("/holdings")
    public List<Holding> getAllHoldings() {
        return repository.getAllHoldings();
    }

    @GetMapping("/holdings/portfolio/{portfolioId}")
    public List<Holding> getHoldingsByPortfolio(@PathVariable int portfolioId) {
        return repository.getHoldingsByPortfolioId(portfolioId);
    }

    @PostMapping("/saveholding")
    public String addHolding(@RequestBody Holding holding) {
        Holding saved = repository.save(holding);
        if (saved != null) {
            syncYahooIfNeeded(saved);
            return "Record added successfully!";
        } else {
            return "Insert failed!";
        }
    }

    @PatchMapping("/holding/{id}")
    public String updateHolding(@PathVariable int id, @RequestBody Holding holding) {
        holding.setId(id);
        Holding updated = repository.update(holding);
        if (updated != null) {
            syncYahooIfNeeded(updated);
            return "Record has been updated";
        } else {
            return "Update failed!";
        }
    }

    @DeleteMapping("/delete/holding/{id}")
    public String deleteHolding(@PathVariable int id) {
        int row = repository.deleteById(id);
        if (row == 1) {
            return "Delete successful " + id;
        } else {
            return "Delete failed!";
        }
    }

    private void syncYahooIfNeeded(Holding holding) {
        if (holding == null || holding.getAssetType() == AssetType.CASH) {
            return;
        }
        if (holding.getTicker() == null || holding.getTicker().isBlank()) {
            return;
        }
        yahooFinanceService.fetchAndStoreCurrentPrice(holding.getTicker().toUpperCase());
    }
}
