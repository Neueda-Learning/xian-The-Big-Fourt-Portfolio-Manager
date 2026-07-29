package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.example.xianthebigfourtportfoliomanager.service.PortfolioService;
import org.example.xianthebigfourtportfoliomanager.service.PortfolioSummaryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioSummaryService portfolioSummaryService;

    public PortfolioController(PortfolioService portfolioService,
                               PortfolioSummaryService portfolioSummaryService) {
        this.portfolioService = portfolioService;
        this.portfolioSummaryService = portfolioSummaryService;
    }

    @GetMapping("/portfolio/{id}")
    public portfolio getPortfolio(@PathVariable int id) {
        return portfolioService.getPortfolioById(id);
    }

    @GetMapping("/portfolios")
    public List<portfolio> getPortfolios() {
        return portfolioService.getAllPortfolios();
    }

    @GetMapping("/portfolios/{id}/summary")
    public PortfolioSummaryService.PortfolioSummaryResponse getSummary(@PathVariable int id) {
        return portfolioSummaryService.getSummary(id);
    }

    @PostMapping("/saveportfolio")
    public String addPortfolio(@RequestBody portfolio portf) {
        portfolio saved = portfolioService.create(portf);
        if (saved != null) {
            return "Record added successfully!";
        } else {
            return "Insert failed!";
        }
    }

    @PatchMapping("/portfolio/{id}")
    public String updatePortfolio(@PathVariable int id, @RequestBody portfolio portf) {
        portf.setId(id);
        portfolio updated = portfolioService.update(portf);
        if (updated != null) {
            return "Record has been updated";
        } else {
            return "Update failed!";
        }
    }

    @DeleteMapping("/delete/portfolio/{id}")
    public String deletePortfolio(@PathVariable int id) {
        int row = portfolioService.deleteById(id);
        if (row == 1) {
            return "Delete successful " + id;
        } else {
            return "Delete failed!";
        }
    }
}
