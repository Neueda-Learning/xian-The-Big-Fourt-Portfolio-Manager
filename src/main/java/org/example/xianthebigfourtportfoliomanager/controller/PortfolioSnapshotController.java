package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.PortfolioSnapshot;
import org.example.xianthebigfourtportfoliomanager.service.PortfolioSnapshotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class PortfolioSnapshotController {

    private final PortfolioSnapshotService portfolioSnapshotService;

    public PortfolioSnapshotController(PortfolioSnapshotService portfolioSnapshotService) {
        this.portfolioSnapshotService = portfolioSnapshotService;
    }

    @GetMapping("/portfolios/{id}/snapshots")
    public List<PortfolioSnapshot> getSnapshots(
            @PathVariable int id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate to = endDate == null ? LocalDate.now() : endDate;
        LocalDate from = startDate == null ? to.minusDays(365) : startDate;
        return portfolioSnapshotService.getSnapshots(id, from, to);
    }

    @PostMapping("/portfolios/{id}/snapshots/capture")
    public String captureSnapshot(@PathVariable int id) {
        portfolioSnapshotService.captureToday(id);
        return "Snapshot captured.";
    }
}
