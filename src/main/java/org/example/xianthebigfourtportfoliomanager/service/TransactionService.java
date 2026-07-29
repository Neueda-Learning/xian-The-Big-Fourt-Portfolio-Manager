package org.example.xianthebigfourtportfoliomanager.service;

<<<<<<< HEAD
import org.example.xianthebigfourtportfoliomanager.entity.AssetType;
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
=======
import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

<<<<<<< HEAD
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
=======
import java.util.List;
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41

@Service
public class TransactionService {

<<<<<<< HEAD
    /**
     * Eren issue: transactions could violate objective rules (oversell, negative cash, inconsistent holdings).
     * Fix: enforce strict BUY/SELL validation, cash balance debits/credits, and deterministic holding recomputation.
     * Reviewer: GitHub Copilot (GPT-5.3-Codex).
     */

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String TX_BUY = "BUY";
    private static final String TX_SELL = "SELL";

    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;

    public TransactionService(TransactionRepository transactionRepository, HoldingRepository holdingRepository) {
        this.transactionRepository = transactionRepository;
        this.holdingRepository = holdingRepository;
=======
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    public List<Transaction> getTransactionsByHoldingId(int holdingId) {
        return transactionRepository.getTransactionsByHoldingId(holdingId);
    }

    public Transaction getTransactionById(int id) {
        return transactionRepository.getTransactionById(id);
    }

    @Transactional
    public Transaction create(Transaction transaction) {
<<<<<<< HEAD
        Transaction normalized = normalizeTransaction(transaction);
        Holding assetHolding = requireAssetHolding(normalized.getHoldingId());
        Holding cashHolding = requireCashHolding(assetHolding.getPortfolioId());

        ensureSellQuantityValid(normalized, null);
        applyCashDelta(cashHolding, cashDeltaFor(normalized));
        holdingRepository.update(cashHolding);

        Transaction saved = transactionRepository.save(normalized);
        recalculateHoldingFromTransactions(normalized.getHoldingId());
        return saved;
=======
        return transactionRepository.save(transaction);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    @Transactional
    public Transaction update(Transaction transaction) {
<<<<<<< HEAD
        if (transaction.getId() == null) {
            throw new IllegalArgumentException("Transaction id is required for update.");
        }

        Transaction before = requireExistingTransaction(transaction.getId());
        Transaction normalized = normalizeTransaction(transaction);
        normalized.setId(before.getId());

        if (!before.getHoldingId().equals(normalized.getHoldingId())) {
            throw new IllegalArgumentException("Changing holdingId for an existing transaction is not supported.");
        }

        Holding assetHolding = requireAssetHolding(normalized.getHoldingId());
        Holding cashHolding = requireCashHolding(assetHolding.getPortfolioId());

        ensureSellQuantityValid(normalized, before.getId());

        BigDecimal cashAdjustment = cashDeltaFor(normalized).subtract(cashDeltaFor(before));
        applyCashDelta(cashHolding, cashAdjustment);
        holdingRepository.update(cashHolding);

        Transaction updated = transactionRepository.update(normalized);
        recalculateHoldingFromTransactions(normalized.getHoldingId());

        return updated;
=======
        return transactionRepository.update(transaction);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }

    @Transactional
    public int deleteById(int id) {
<<<<<<< HEAD
        Transaction before = requireExistingTransaction(id);
        Holding assetHolding = requireAssetHolding(before.getHoldingId());
        Holding cashHolding = requireCashHolding(assetHolding.getPortfolioId());

        applyCashDelta(cashHolding, cashDeltaFor(before).negate());
        holdingRepository.update(cashHolding);

        int rows = transactionRepository.deleteById(id);
        if (rows > 0) {
            recalculateHoldingFromTransactions(before.getHoldingId());
        }
        return rows;
    }

    private void recalculateHoldingFromTransactions(int holdingId) {
        Holding holding = holdingRepository.getHoldingById(holdingId);
        if (holding == null) {
            return;
        }

        List<Transaction> transactions = transactionRepository.getTransactionsByHoldingId(holdingId);
        BigDecimal quantity = ZERO;
        BigDecimal totalCost = ZERO;

        for (Transaction tx : transactions) {
            if (tx == null || tx.getQuantity() == null || tx.getPrice() == null) {
                continue;
            }

            String txType = tx.getType() == null ? "BUY" : tx.getType().trim().toUpperCase(Locale.ROOT);
            BigDecimal txQty = tx.getQuantity().max(ZERO);
            BigDecimal txPrice = tx.getPrice().max(ZERO);

            if (TX_SELL.equals(txType)) {
                if (txQty.compareTo(quantity) > 0) {
                    throw new IllegalArgumentException("Sell quantity exceeds holding quantity for holding id " + holdingId + ".");
                }

                if (quantity.compareTo(ZERO) > 0) {
                    BigDecimal avgCost = totalCost.divide(quantity, 8, RoundingMode.HALF_UP);
                    totalCost = totalCost.subtract(avgCost.multiply(txQty));
                    quantity = quantity.subtract(txQty);
                }
            } else {
                quantity = quantity.add(txQty);
                totalCost = totalCost.add(txQty.multiply(txPrice));
            }
        }

        if (quantity.compareTo(ZERO) <= 0) {
            holding.setQuantity(ZERO);
            holding.setPurchasePrice(ZERO);
        } else {
            BigDecimal avgPrice = totalCost.divide(quantity, 4, RoundingMode.HALF_UP);
            holding.setQuantity(quantity.setScale(4, RoundingMode.HALF_UP));
            holding.setPurchasePrice(avgPrice);
        }

        holdingRepository.update(holding);
    }

    private Transaction normalizeTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction payload is required.");
        }
        if (transaction.getHoldingId() == null) {
            throw new IllegalArgumentException("holdingId is required.");
        }

        String type = transaction.getType() == null ? TX_BUY : transaction.getType().trim().toUpperCase(Locale.ROOT);
        if (!TX_BUY.equals(type) && !TX_SELL.equals(type)) {
            throw new IllegalArgumentException("Transaction type must be BUY or SELL.");
        }

        BigDecimal quantity = transaction.getQuantity();
        BigDecimal price = transaction.getPrice();
        if (quantity == null || quantity.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction quantity must be greater than 0.");
        }
        if (price == null || price.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction price must be greater than 0.");
        }

        transaction.setType(type);
        transaction.setTradeDate(transaction.getTradeDate() == null ? LocalDateTime.now() : transaction.getTradeDate());
        return transaction;
    }

    private Transaction requireExistingTransaction(int id) {
        Transaction existing = transactionRepository.getTransactionById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Transaction not found: " + id);
        }
        return existing;
    }

    private Holding requireAssetHolding(int holdingId) {
        Holding holding = holdingRepository.getHoldingById(holdingId);
        if (holding == null) {
            throw new IllegalArgumentException("Holding not found: " + holdingId);
        }
        if (holding.getAssetType() == AssetType.CASH) {
            throw new IllegalArgumentException("Direct CASH transactions are not supported. Use BUY/SELL on non-cash holdings.");
        }
        return holding;
    }

    private Holding requireCashHolding(int portfolioId) {
        return holdingRepository.getHoldingsByPortfolioId(portfolioId).stream()
                .filter(h -> h.getAssetType() == AssetType.CASH || (h.getTicker() != null && "CASH".equalsIgnoreCase(h.getTicker())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No CASH holding found for portfolio " + portfolioId + ". Please create a CASH holding first."));
    }

    private void ensureSellQuantityValid(Transaction candidate, Integer excludeTxId) {
        if (!TX_SELL.equals(candidate.getType())) {
            return;
        }

        BigDecimal projectedQuantity = computeQuantityAfterApplying(candidate.getHoldingId(), candidate, excludeTxId);
        if (projectedQuantity.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Sell quantity exceeds available shares for this holding.");
        }
    }

    private BigDecimal computeQuantityAfterApplying(int holdingId, Transaction candidate, Integer excludeTxId) {
        List<Transaction> transactions = transactionRepository.getTransactionsByHoldingId(holdingId);
        transactions.sort(Comparator
                .comparing(Transaction::getTradeDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(tx -> tx.getId() == null ? Integer.MAX_VALUE : tx.getId()));

        BigDecimal quantity = ZERO;
        for (Transaction tx : transactions) {
            if (tx == null || tx.getQuantity() == null || tx.getType() == null) {
                continue;
            }
            if (excludeTxId != null && tx.getId() != null && excludeTxId.equals(tx.getId())) {
                continue;
            }
            quantity = TX_SELL.equalsIgnoreCase(tx.getType()) ? quantity.subtract(tx.getQuantity()) : quantity.add(tx.getQuantity());
        }

        if (candidate != null) {
            quantity = TX_SELL.equals(candidate.getType())
                    ? quantity.subtract(candidate.getQuantity())
                    : quantity.add(candidate.getQuantity());
        }

        return quantity;
    }

    private BigDecimal cashDeltaFor(Transaction tx) {
        BigDecimal amount = tx.getQuantity().multiply(tx.getPrice());
        return TX_SELL.equals(tx.getType()) ? amount : amount.negate();
    }

    private void applyCashDelta(Holding cashHolding, BigDecimal delta) {
        BigDecimal current = cashHolding.getQuantity() == null ? ZERO : cashHolding.getQuantity();
        BigDecimal next = current.add(delta);
        if (next.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient cash balance. Available: " + current + ", required change: " + delta.abs());
        }
        cashHolding.setQuantity(next.setScale(4, RoundingMode.HALF_UP));
        if (cashHolding.getPurchasePrice() == null || cashHolding.getPurchasePrice().compareTo(ZERO) <= 0) {
            cashHolding.setPurchasePrice(BigDecimal.ONE);
        }
=======
        return transactionRepository.deleteById(id);
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
    }
}

