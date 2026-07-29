package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.example.xianthebigfourtportfoliomanager.repository.PortfolioRepository;
import org.example.xianthebigfourtportfoliomanager.service.PortfolioSummaryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PortfolioController {

    private final PortfolioRepository repository;
    private final PortfolioSummaryService portfolioSummaryService;

    public PortfolioController(PortfolioRepository repository,
                               PortfolioSummaryService portfolioSummaryService) {
        this.repository = repository;
        this.portfolioSummaryService = portfolioSummaryService;
    }

    @GetMapping("/portfolio/{id}")
    public portfolio getPortfolio(@PathVariable int id) {
        return repository.getPortfolioById(id);
    }

    @GetMapping("/portfolios")
    public List<portfolio> getPortfolios() {
        return repository.getAllPortfolios();
    }

    @GetMapping("/portfolios/{id}/summary")
    public PortfolioSummaryService.PortfolioSummaryResponse getSummary(@PathVariable int id) {
        return portfolioSummaryService.getSummary(id);
    }

    @PostMapping("/saveportfolio")
    public String addPortfolio(@RequestBody portfolio portf) {
        portfolio saved = repository.save(portf);
        if (saved != null) {
            return "Record added successfully!";
        } else {
            return "Insert failed!";
        }
    }

    @PatchMapping("/portfolio/{id}")
    public String updatePortfolio(@PathVariable int id, @RequestBody portfolio portf) {
        portf.setId(id);
        portfolio updated = repository.update(portf);
        if (updated != null) {
            return "Record has been updated";
        } else {
            return "Update failed!";
        }
    }

    @DeleteMapping("/delete/portfolio/{id}")
    public String deletePortfolio(@PathVariable int id) {
        int row = repository.deleteById(id);
        if (row == 1) {
            return "Delete successful " + id;
        } else {
            return "Delete failed!";
        }
    }
}
