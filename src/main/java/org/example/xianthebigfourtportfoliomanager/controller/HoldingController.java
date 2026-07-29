package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.service.HoldingService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class HoldingController {

    private final HoldingService service;

    public HoldingController(HoldingService service) {
        this.service = service;
    }

    @GetMapping("/holding/{id}")
    public Holding getHolding(@PathVariable int id) {
        return service.getHoldingById(id);
    }

    @GetMapping("/holdings/portfolio/{portfolioId}")
    public List<Holding> getHoldingsByPortfolio(@PathVariable int portfolioId) {
        return service.getHoldingsByPortfolioId(portfolioId);
    }

    @PatchMapping("/holding/{id}/price")
    public Holding updateCurrentPrice(@PathVariable int id, @RequestParam BigDecimal currentPrice) {
        return service.updateCurrentPrice(id, currentPrice);
    }

    @PostMapping("/saveholding")
    public String addHolding(@RequestBody Holding ignored) {
        throw new IllegalArgumentException("Holding create is disabled. Use /savetransaction (BUY) to change positions.");
    }

    @PatchMapping("/holding/{id}")
    public String updateHolding(@PathVariable int id, @RequestBody Holding ignored) {
        throw new IllegalArgumentException("Holding quantity edits are disabled. Use /savetransaction for BUY/SELL.");
    }

    @DeleteMapping("/delete/holding/{id}")
    public String deleteHolding(@PathVariable int id) {
        throw new IllegalArgumentException("Holding delete is disabled. Quantity changes must come from BUY/SELL only.");
    }
}
