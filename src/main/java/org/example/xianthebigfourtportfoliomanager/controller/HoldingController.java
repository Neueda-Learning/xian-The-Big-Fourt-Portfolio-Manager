package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;
<<<<<<< HEAD
import org.example.xianthebigfourtportfoliomanager.service.HoldingService;
=======
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HoldingController {

<<<<<<< HEAD
    /**
     * Eren issue: holdings and transactions became inconsistent because controllers wrote directly to repositories.
     * Fix: route holding mutations through HoldingService so merge logic, cash rules, and transaction mirroring are centralized.
     * Reviewer: GitHub Copilot (GPT-5.3-Codex).
     */

    private final HoldingService service;

    public HoldingController(HoldingService service) {
        this.service = service;
=======
    private final HoldingRepository repository;

    public HoldingController(HoldingRepository repository) {
        this.repository = repository;
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    @GetMapping("/holding/{id}")
    public Holding getHolding(@PathVariable int id) {
<<<<<<< HEAD
        return service.getHoldingById(id);
=======
        return repository.getHoldingById(id);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    @GetMapping("/holdings/portfolio/{portfolioId}")
    public List<Holding> getHoldingsByPortfolio(@PathVariable int portfolioId) {
<<<<<<< HEAD
        return service.getHoldingsByPortfolioId(portfolioId);
=======
        return repository.getHoldingsByPortfolioId(portfolioId);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    @PostMapping("/saveholding")
    public String addHolding(@RequestBody Holding holding) {
<<<<<<< HEAD
        Holding saved = service.create(holding);
=======
        Holding saved = repository.save(holding);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
        if (saved != null) {
            return "Record added successfully!";
        } else {
            return "Insert failed!";
        }
    }

    @PatchMapping("/holding/{id}")
    public String updateHolding(@PathVariable int id, @RequestBody Holding holding) {
        holding.setId(id);
<<<<<<< HEAD
        Holding updated = service.update(holding);
=======
        Holding updated = repository.update(holding);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
        if (updated != null) {
            return "Record has been updated";
        } else {
            return "Update failed!";
        }
    }

    @DeleteMapping("/delete/holding/{id}")
    public String deleteHolding(@PathVariable int id) {
<<<<<<< HEAD
        int row = service.deleteById(id);
=======
        int row = repository.deleteById(id);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
        if (row == 1) {
            return "Delete successful " + id;
        } else {
            return "Delete failed!";
        }
    }
}
