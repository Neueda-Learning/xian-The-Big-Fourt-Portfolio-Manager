package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HoldingController {

    private final HoldingRepository repository;

    public HoldingController(HoldingRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/holding/{id}")
    public Holding getHolding(@PathVariable int id) {
        return repository.getHoldingById(id);
    }

    @GetMapping("/holdings/portfolio/{portfolioId}")
    public List<Holding> getHoldingsByPortfolio(@PathVariable int portfolioId) {
        return repository.getHoldingsByPortfolioId(portfolioId);
    }

    @PostMapping("/saveholding")
    public String addHolding(@RequestBody Holding holding) {
        Holding saved = repository.save(holding);
        if (saved != null) {
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
}
