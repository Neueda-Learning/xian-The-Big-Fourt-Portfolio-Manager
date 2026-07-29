package org.example.xianthebigfourtportfoliomanager.service;

<<<<<<< HEAD
import org.example.xianthebigfourtportfoliomanager.entity.AssetType;
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
=======
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41

@Service
public class HoldingService {

<<<<<<< HEAD
    /**
     * Eren issue: adding the same ticker created duplicate holding rows and bypassed cash constraints.
     * Fix: merge same ticker/type positions, enforce cash checks on quantity changes, and mirror quantity deltas into transactions.
     * Reviewer: GitHub Copilot (GPT-5.3-Codex).
     */

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    public HoldingService(HoldingRepository holdingRepository, TransactionRepository transactionRepository) {
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
=======
    private final HoldingRepository holdingRepository;

    public HoldingService(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    public List<Holding> getHoldingsByPortfolioId(int portfolioId) {
        return holdingRepository.getHoldingsByPortfolioId(portfolioId);
    }

    public Holding getHoldingById(int holdingId) {
        return holdingRepository.getHoldingById(holdingId);
    }

    public boolean existsById(int holdingId) {
        return holdingRepository.existsById(holdingId);
    }

    @Transactional
    public Holding create(Holding holding) {
<<<<<<< HEAD
        Holding existing = findExistingHolding(holding);
        if (existing != null) {
            BigDecimal addQty = sanitizePositive(holding.getQuantity());
            BigDecimal tradePrice = holding.getPurchasePrice() == null ? ZERO : holding.getPurchasePrice();

            if (existing.getAssetType() != AssetType.CASH) {
                applyCashChangeForTrade(existing.getPortfolioId(), addQty.multiply(tradePrice).negate(), null);
            }

            BigDecimal oldQty = existing.getQuantity() == null ? ZERO : existing.getQuantity();
            BigDecimal oldPrice = existing.getPurchasePrice() == null ? ZERO : existing.getPurchasePrice();
            BigDecimal oldCost = oldQty.multiply(oldPrice);
            BigDecimal addCost = addQty.multiply(tradePrice);
            BigDecimal mergedQty = oldQty.add(addQty);

            existing.setQuantity(mergedQty);
            if (mergedQty.compareTo(ZERO) > 0) {
                BigDecimal mergedAvg = oldCost.add(addCost).divide(mergedQty, 4, RoundingMode.HALF_UP);
                existing.setPurchasePrice(mergedAvg);
            }

            Holding updated = holdingRepository.update(existing);
            if (updated.getAssetType() != AssetType.CASH) {
                createMirrorBuyTransaction(updated, addQty, tradePrice, holding.getPurchasedata() == null ? null : holding.getPurchasedata().atStartOfDay());
            }
            return updated;
        }

        if (holding.getAssetType() != AssetType.CASH) {
            BigDecimal qty = sanitizePositive(holding.getQuantity());
            BigDecimal price = holding.getPurchasePrice() == null ? ZERO : holding.getPurchasePrice();
            applyCashChangeForTrade(holding.getPortfolioId(), qty.multiply(price).negate(), null);
        }

        Holding saved = holdingRepository.save(holding);
        if (saved != null && saved.getAssetType() != AssetType.CASH) {
            createMirrorBuyTransaction(saved, saved.getQuantity(), saved.getPurchasePrice(),
                    saved.getPurchasedata() == null ? null : saved.getPurchasedata().atStartOfDay());
        }
        return saved;
=======
        return holdingRepository.save(holding);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    @Transactional
    public Holding update(Holding holding) {
<<<<<<< HEAD
        Holding existing = holding.getId() == null ? null : holdingRepository.getHoldingById(holding.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Holding not found: " + holding.getId());
        }

        if (existing.getAssetType() != AssetType.CASH) {
            BigDecimal beforeQty = existing.getQuantity() == null ? ZERO : existing.getQuantity();
            BigDecimal afterQty = holding.getQuantity() == null ? ZERO : holding.getQuantity();
            if (afterQty.compareTo(ZERO) < 0) {
                throw new IllegalArgumentException("Holding quantity cannot be negative.");
            }

            BigDecimal delta = afterQty.subtract(beforeQty);
            if (delta.compareTo(ZERO) != 0) {
                BigDecimal price = holding.getPurchasePrice() == null ? ZERO : holding.getPurchasePrice();
                BigDecimal cashDelta = delta.compareTo(ZERO) > 0
                        ? delta.multiply(price).negate()
                        : delta.abs().multiply(price);
                applyCashChangeForTrade(existing.getPortfolioId(), cashDelta, existing.getId());
            }
        }

        Holding updated = holdingRepository.update(holding);
        if (existing.getAssetType() != AssetType.CASH && updated != null) {
            syncQuantityDeltaToTransactions(existing, updated);
        }
        return updated;
=======
        return holdingRepository.update(holding);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    @Transactional
    public int deleteById(int holdingId) {
        return holdingRepository.deleteById(holdingId);
    }
<<<<<<< HEAD

    private void createMirrorBuyTransaction(Holding holding, BigDecimal quantity, BigDecimal tradePrice, LocalDateTime tradeDate) {
        if (holding.getId() == null || quantity == null || quantity.compareTo(ZERO) <= 0) {
            return;
        }

        BigDecimal price = tradePrice == null ? ZERO : tradePrice;
        LocalDateTime finalTradeDate = tradeDate == null ? LocalDateTime.now() : tradeDate;

        Transaction transaction = new Transaction();
        transaction.setHoldingId(holding.getId());
        transaction.setType("BUY");
        transaction.setQuantity(quantity);
        transaction.setPrice(price);
        transaction.setTradeDate(finalTradeDate);
        transactionRepository.save(transaction);
    }

    private void syncQuantityDeltaToTransactions(Holding before, Holding after) {
        BigDecimal beforeQty = before.getQuantity() == null ? ZERO : before.getQuantity();
        BigDecimal afterQty = after.getQuantity() == null ? ZERO : after.getQuantity();
        BigDecimal delta = afterQty.subtract(beforeQty);

        if (delta.compareTo(ZERO) == 0) {
            return;
        }

        Transaction transaction = new Transaction();
        transaction.setHoldingId(after.getId());
        transaction.setType(delta.compareTo(ZERO) > 0 ? "BUY" : "SELL");
        transaction.setQuantity(delta.abs());
        transaction.setPrice(after.getPurchasePrice() == null ? ZERO : after.getPurchasePrice());
        transaction.setTradeDate(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    private Holding findExistingHolding(Holding incoming) {
        if (incoming == null || incoming.getPortfolioId() == null || incoming.getTicker() == null) {
            return null;
        }

        String incomingTicker = incoming.getTicker().trim().toUpperCase(Locale.ROOT);
        if (incomingTicker.isEmpty()) {
            return null;
        }

        String incomingType = incoming.getAssetType() == null ? "" : incoming.getAssetType().name();
        return holdingRepository.getHoldingsByPortfolioId(incoming.getPortfolioId()).stream()
                .filter(h -> h.getTicker() != null && h.getAssetType() != null)
                .filter(h -> incomingTicker.equals(h.getTicker().trim().toUpperCase(Locale.ROOT)))
                .filter(h -> incomingType.equals(h.getAssetType().name()))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal sanitizePositive(BigDecimal value) {
        if (value == null || value.compareTo(ZERO) < 0) {
            return ZERO;
        }
        return value;
    }

    private void applyCashChangeForTrade(Integer portfolioId, BigDecimal cashDelta, Integer excludeHoldingId) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("portfolioId is required for cash balance validation.");
        }

        Holding cashHolding = holdingRepository.getHoldingsByPortfolioId(portfolioId).stream()
                .filter(h -> h.getId() != null && (excludeHoldingId == null || !excludeHoldingId.equals(h.getId())))
                .filter(h -> h.getAssetType() == AssetType.CASH || (h.getTicker() != null && "CASH".equalsIgnoreCase(h.getTicker())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No CASH holding found for portfolio " + portfolioId + ". Please create a CASH holding first."));

        BigDecimal current = cashHolding.getQuantity() == null ? ZERO : cashHolding.getQuantity();
        BigDecimal next = current.add(cashDelta);
        if (next.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient cash balance. Available: " + current + ", required change: " + cashDelta.abs());
        }

        cashHolding.setQuantity(next.setScale(4, RoundingMode.HALF_UP));
        if (cashHolding.getPurchasePrice() == null || cashHolding.getPurchasePrice().compareTo(ZERO) <= 0) {
            cashHolding.setPurchasePrice(BigDecimal.ONE);
        }
        holdingRepository.update(cashHolding);
    }
=======
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
}
