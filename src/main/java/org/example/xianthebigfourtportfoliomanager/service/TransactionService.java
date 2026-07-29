package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.AssetType;
import org.example.xianthebigfourtportfoliomanager.entity.CashDepositRequest;
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.entity.TradeRequest;
import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.example.xianthebigfourtportfoliomanager.repository.PortfolioRepository;
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class TransactionService {

    /**
     * Eren issue: transactions could violate objective rules (oversell, negative cash, inconsistent holdings).
     * Fix: enforce strict BUY/SELL validation, cash balance debits/credits, and deterministic holding recomputation.
     * Reviewer: GitHub Copilot (GPT-5.3-Codex).
     */

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final String TX_BUY = "BUY";
    private static final String TX_SELL = "SELL";
    private static final String TX_CASH_IN = "IN";

    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioSnapshotService portfolioSnapshotService;

    public TransactionService(TransactionRepository transactionRepository,
                              HoldingRepository holdingRepository,
                              PortfolioRepository portfolioRepository,
                              PortfolioSnapshotService portfolioSnapshotService) {
        this.transactionRepository = transactionRepository;
        this.holdingRepository = holdingRepository;
        this.portfolioRepository = portfolioRepository;
        this.portfolioSnapshotService = portfolioSnapshotService;
    }

    public List<Transaction> getTransactionsByHoldingId(int holdingId) {
        return transactionRepository.getTransactionsByHoldingId(holdingId);
    }

    public Transaction getTransactionById(int id) {
        return transactionRepository.getTransactionById(id);
    }

    public List<Transaction> getTransactionsByPortfolioId(int portfolioId) {
        requirePortfolio(portfolioId);
        return transactionRepository.getTransactionsByPortfolioId(portfolioId);
    }

    @Transactional
    public Transaction buy(int portfolioId, TradeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transaction payload is required.");
        }

        Holding holding = resolveOrCreateHoldingForBuy(portfolioId, request);
        Transaction transaction = toTransaction(request, holding.getId(), TX_BUY);
        return create(transaction);
    }

    @Transactional
    public Transaction sell(int portfolioId, TradeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transaction payload is required.");
        }

        if (request.getHoldingId() == null) {
            throw new IllegalArgumentException("holdingId is required for SELL.");
        }

        ensureHoldingBelongsToPortfolio(request.getHoldingId(), portfolioId);
        Transaction transaction = toTransaction(request, request.getHoldingId(), TX_SELL);
        transaction.setType(TX_SELL);
        return create(transaction);
    }

    @Transactional
    public Transaction depositCash(int portfolioId, CashDepositRequest request) {
        if (request == null || request.getAmount() == null || request.getAmount().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0.");
        }

        portfolio portf = requirePortfolio(portfolioId);
        BigDecimal amount = request.getAmount().setScale(4, RoundingMode.HALF_UP);
        
        portfolio firstPortfolio = requirePortfolio(1);
        BigDecimal currentCash = firstPortfolio.getCashBalance() == null ? ZERO : firstPortfolio.getCashBalance();
        BigDecimal currentInitial = firstPortfolio.getInitialCash() == null ? ZERO : firstPortfolio.getInitialCash();
        BigDecimal nextCash = currentCash.add(amount).setScale(4, RoundingMode.HALF_UP);
        BigDecimal nextInitial = currentInitial.add(amount).setScale(4, RoundingMode.HALF_UP);

        Transaction tx = new Transaction();
        tx.setPortfolioId(portfolioId);
        tx.setHoldingId(null);
        tx.setType(TX_CASH_IN);
        tx.setQuantity(amount);
        tx.setPrice(ONE.setScale(4, RoundingMode.HALF_UP));
        tx.setTradeDate(request.getTradeDate() == null ? LocalDateTime.now() : request.getTradeDate());

        portfolioRepository.updateInitialCashAndBalance(firstPortfolio.getId(), nextInitial, nextCash);
        Transaction saved = transactionRepository.save(tx);
        portfolioSnapshotService.captureToday(portfolioId);
        return saved;
    }

    @Transactional
    public Transaction create(Transaction transaction) {
        Transaction normalized = normalizeTransaction(transaction);
        Holding assetHolding = requireAssetHolding(normalized.getHoldingId());
        portfolio portf = requirePortfolio(assetHolding.getPortfolioId());
        normalized.setPortfolioId(portf.getId());

        ensureSellQuantityValid(normalized, null);
        
        portfolio firstPortfolio = requirePortfolio(1);
        applyCashDelta(firstPortfolio, cashDeltaFor(normalized));
        portfolioRepository.updateCashBalance(firstPortfolio.getId(), firstPortfolio.getCashBalance());

        Transaction saved = transactionRepository.save(normalized);
        recalculateHoldingFromTransactions(normalized.getHoldingId());
        portfolioSnapshotService.captureToday(portf.getId());
        return saved;
    }

    @Transactional
    public Transaction update(Transaction transaction) {
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
        portfolio portf = requirePortfolio(assetHolding.getPortfolioId());

        ensureSellQuantityValid(normalized, before.getId());

        portfolio firstPortfolio = requirePortfolio(1);
        BigDecimal cashAdjustment = cashDeltaFor(normalized).subtract(cashDeltaFor(before));
        applyCashDelta(firstPortfolio, cashAdjustment);
        portfolioRepository.updateCashBalance(firstPortfolio.getId(), firstPortfolio.getCashBalance());

        Transaction updated = transactionRepository.update(normalized);
        recalculateHoldingFromTransactions(normalized.getHoldingId());
        portfolioSnapshotService.captureToday(portf.getId());

        return updated;
    }

    @Transactional
    public int deleteById(int id) {
        Transaction before = requireExistingTransaction(id);
        Holding assetHolding = requireAssetHolding(before.getHoldingId());
        portfolio portf = requirePortfolio(assetHolding.getPortfolioId());

        portfolio firstPortfolio = requirePortfolio(1);
        applyCashDelta(firstPortfolio, cashDeltaFor(before).negate());
        portfolioRepository.updateCashBalance(firstPortfolio.getId(), firstPortfolio.getCashBalance());

        int rows = transactionRepository.deleteById(id);
        if (rows > 0) {
            recalculateHoldingFromTransactions(before.getHoldingId());
            portfolioSnapshotService.captureToday(portf.getId());
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
            holding.setAveragePrice(ZERO);
        } else {
            BigDecimal avgPrice = totalCost.divide(quantity, 4, RoundingMode.HALF_UP);
            holding.setQuantity(quantity.setScale(4, RoundingMode.HALF_UP));
            holding.setAveragePrice(avgPrice);
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

    private Transaction toTransaction(TradeRequest request, Integer holdingId, String type) {
        Transaction tx = new Transaction();
        tx.setPortfolioId(null);
        tx.setHoldingId(holdingId);
        tx.setType(type);
        tx.setQuantity(request.getQuantity());
        tx.setPrice(request.getPrice());
        tx.setTradeDate(request.getTradeDate());
        return tx;
    }

    private Holding resolveOrCreateHoldingForBuy(int portfolioId, TradeRequest request) {
        if (request.getHoldingId() != null) {
            ensureHoldingBelongsToPortfolio(request.getHoldingId(), portfolioId);
            return requireAssetHolding(request.getHoldingId());
        }

        String ticker = normalizeTicker(request.getTicker());
        AssetType assetType = normalizeAssetType(request.getAssetType());
        String currency = normalizeCurrency(request.getCurrency());

        Holding existing = holdingRepository.findByPortfolioTickerAndAssetType(portfolioId, ticker, assetType);
        if (existing != null) {
            return existing;
        }

        Holding newHolding = new Holding();
        newHolding.setPortfolioId(portfolioId);
        newHolding.setAssetType(assetType);
        newHolding.setTicker(ticker);
        newHolding.setQuantity(ZERO.setScale(4, RoundingMode.HALF_UP));
        newHolding.setAveragePrice(ZERO.setScale(4, RoundingMode.HALF_UP));
        newHolding.setCurrentPrice(request.getPrice() == null ? ZERO.setScale(4, RoundingMode.HALF_UP) : request.getPrice().setScale(4, RoundingMode.HALF_UP));
        newHolding.setCurrency(currency);
        LocalDate tradeDate = request.getTradeDate() == null ? LocalDate.now() : request.getTradeDate().toLocalDate();
        newHolding.setPurchasedata(tradeDate);

        Holding saved = holdingRepository.save(newHolding);
        if (saved == null || saved.getId() == null) {
            throw new IllegalArgumentException("Failed to create holding for ticker " + ticker + ".");
        }
        return saved;
    }

    private String normalizeTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker is required when holdingId is not provided.");
        }
        return ticker.trim().toUpperCase(Locale.ROOT);
    }

    private AssetType normalizeAssetType(String assetType) {
        String normalized = assetType == null ? "STOCK" : assetType.trim().toUpperCase(Locale.ROOT);
        if ("CASH".equals(normalized)) {
            throw new IllegalArgumentException("CASH holdings are not supported for BUY.");
        }
        try {
            return AssetType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("assetType must be STOCK or BOND.");
        }
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "USD";
        }
        return currency.trim().toUpperCase(Locale.ROOT);
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

    private void ensureHoldingBelongsToPortfolio(Integer holdingId, int portfolioId) {
        if (holdingId == null) {
            throw new IllegalArgumentException("holdingId is required.");
        }
        Holding holding = requireAssetHolding(holdingId);
        if (holding.getPortfolioId() == null || holding.getPortfolioId() != portfolioId) {
            throw new IllegalArgumentException("Holding does not belong to the target portfolio.");
        }
    }

    private portfolio requirePortfolio(int portfolioId) {
        portfolio p = portfolioRepository.getPortfolioById(portfolioId);
        if (p == null) {
            throw new IllegalArgumentException("Portfolio not found: " + portfolioId);
        }
        return p;
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

    private void applyCashDelta(portfolio portf, BigDecimal delta) {
        BigDecimal current = portf.getCashBalance() == null ? ZERO : portf.getCashBalance();
        BigDecimal next = current.add(delta);
        if (next.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient cash balance. Available: " + current + ", required change: " + delta.abs());
        }
        portf.setCashBalance(next.setScale(4, RoundingMode.HALF_UP));
    }
}

