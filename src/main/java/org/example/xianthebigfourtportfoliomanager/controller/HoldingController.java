package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.service.HoldingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HoldingController {

    /**
     * Eren issue: holdings and transactions became inconsistent because controllers wrote directly to repositories.
     * Fix: route holding mutations through HoldingService so merge logic, cash rules, and transaction mirroring are centralized.
     * Reviewer: GitHub Copilot (GPT-5.3-Codex).
     */

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

    @PostMapping("/saveholding")
    public String addHolding(@RequestBody Holding holding) {
        Holding saved = service.create(holding);
        if (saved != null) {
            return "Record added successfully!";
        } else {
            return "Insert failed!";
        }
    }

    @PatchMapping("/holding/{id}")
    public String updateHolding(@PathVariable int id, @RequestBody Holding holding) {
        holding.setId(id);
        Holding updated = service.update(holding);
        if (updated != null) {
            return "Record has been updated";
        } else {
            return "Update failed!";
        }
    }

    @DeleteMapping("/delete/holding/{id}")
    public String deleteHolding(@PathVariable int id) {
        int row = service.deleteById(id);
        if (row == 1) {
            return "Delete successful " + id;
        } else {
            return "Delete failed!";
        }
    }
}
