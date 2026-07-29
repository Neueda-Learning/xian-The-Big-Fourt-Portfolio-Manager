package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.AssetType;
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

@Service
public class CashBalanceService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;

    public CashBalanceService(HoldingRepository holdingRepository,
                              PortfolioRepository portfolioRepository,
                              TransactionRepository transactionRepository) {
        this.holdingRepository = holdingRepository;
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public BigDecimal applySharedCashDelta(BigDecimal delta) {
        Holding cashHolding = ensureSharedCashHolding();
        BigDecimal current = normalizeAmount(cashHolding.getQuantity());
        BigDecimal change = normalizeSignedAmount(delta);
        BigDecimal next = current.add(change);
        if (next.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient cash balance. Available: " + current + ", required change: " + next.abs());
        }
        if (change.compareTo(ZERO) != 0) {
            updateCashHolding(cashHolding, next);
            recordCashTransaction(cashHolding, change);
        }
        return next;
    }

    @Transactional
    public BigDecimal setSharedCashBalance(BigDecimal balance) {
        BigDecimal next = normalizeAmount(balance);
        Holding cashHolding = ensureSharedCashHolding();
        BigDecimal current = normalizeAmount(cashHolding.getQuantity());
        BigDecimal change = next.subtract(current);
        if (change.compareTo(ZERO) != 0) {
            updateCashHolding(cashHolding, next);
            recordCashTransaction(cashHolding, change);
        }
        return next;
    }

    public Holding getSharedCashHolding() {
        return ensureSharedCashHolding();
    }

    public BigDecimal getSharedCashBalance() {
        return normalizeAmount(ensureSharedCashHolding().getQuantity());
    }

    public List<Holding> getSharedCashHoldingsForDisplay() {
        return Collections.singletonList(ensureSharedCashHolding());
    }

    public Holding requireCashHolding(int portfolioId) {
        return ensureSharedCashHolding();
    }

    private Holding ensureSharedCashHolding() {
        List<portfolio> portfolios = portfolioRepository.getAllPortfolios();
        if (portfolios.isEmpty()) {
            throw new IllegalArgumentException("No portfolios found for shared cash balance.");
        }

        List<Holding> cashHoldings = new ArrayList<>();
        for (portfolio item : portfolios) {
            for (Holding holding : holdingRepository.getHoldingsByPortfolioId(item.getId())) {
                if (isCashHolding(holding)) {
                    cashHoldings.add(holding);
                }
            }
        }

        if (!cashHoldings.isEmpty()) {
            Holding canonical = consolidateCashHoldings(cashHoldings);
            ensureCashTransactionsMatchBalance(canonical);
            return canonical;
        }

        portfolio owner = portfolios.get(0);
        Holding cashHolding = new Holding();
        cashHolding.setPortfolioId(owner.getId());
        cashHolding.setAssetType(AssetType.CASH);
        cashHolding.setTicker("CASH");
        cashHolding.setQuantity(ZERO.setScale(4, RoundingMode.HALF_UP));
        cashHolding.setPurchasePrice(ONE);
        cashHolding.setPurchasedata(LocalDate.now());
        cashHolding.setCurrency("USD");
        Holding saved = holdingRepository.save(cashHolding);
        Holding canonical = saved == null ? cashHolding : saved;
        ensureCashTransactionsMatchBalance(canonical);
        return canonical;
    }

    private void updateCashHolding(Holding cashHolding, BigDecimal balance) {
        BigDecimal normalized = normalizeAmount(balance);
        cashHolding.setAssetType(AssetType.CASH);
        cashHolding.setTicker("CASH");
        cashHolding.setQuantity(normalized);
        cashHolding.setPurchasePrice(ONE);
        if (cashHolding.getPurchasedata() == null) {
            cashHolding.setPurchasedata(LocalDate.now());
        }
        if (cashHolding.getCurrency() == null || cashHolding.getCurrency().isBlank()) {
            cashHolding.setCurrency("USD");
        }
        holdingRepository.update(cashHolding);
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        BigDecimal amount = value == null ? ZERO : value;
        if (amount.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Cash balance cannot be negative.");
        }
        return amount.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeSignedAmount(BigDecimal value) {
        BigDecimal amount = value == null ? ZERO : value;
        return amount.setScale(4, RoundingMode.HALF_UP);
    }

    private Holding consolidateCashHoldings(List<Holding> cashHoldings) {
        cashHoldings.sort(Comparator.comparing(Holding::getId));
        Holding canonical = cashHoldings.get(0);
        BigDecimal total = ZERO.setScale(4, RoundingMode.HALF_UP);
        for (Holding holding : cashHoldings) {
            total = total.add(normalizeSignedAmount(holding.getQuantity()));
        }

        if (needsCashHoldingUpdate(canonical, total, cashHoldings.size() > 1)) {
            updateCashHolding(canonical, total);
        }

        for (Holding holding : cashHoldings) {
            if (holding.getId() == null || canonical.getId() == null || canonical.getId().equals(holding.getId())) {
                continue;
            }
            transactionRepository.reassignHoldingId(holding.getId(), canonical.getId());
            holdingRepository.deleteById(holding.getId());
        }

        Holding refreshed = canonical.getId() == null ? canonical : holdingRepository.getHoldingById(canonical.getId());
        return refreshed == null ? canonical : refreshed;
    }

    private void ensureCashTransactionsMatchBalance(Holding cashHolding) {
        if (cashHolding == null || cashHolding.getId() == null) {
            return;
        }

        BigDecimal transactionBalance = ZERO.setScale(4, RoundingMode.HALF_UP);
        for (Transaction transaction : transactionRepository.getTransactionsByHoldingId(cashHolding.getId())) {
            if (transaction == null || transaction.getQuantity() == null) {
                continue;
            }
            BigDecimal price = transaction.getPrice() == null ? ONE : transaction.getPrice();
            BigDecimal amount = transaction.getQuantity().multiply(price);
            if ("SELL".equalsIgnoreCase(transaction.getType())) {
                transactionBalance = transactionBalance.subtract(amount);
            } else {
                transactionBalance = transactionBalance.add(amount);
            }
        }

        BigDecimal balance = normalizeAmount(cashHolding.getQuantity());
        BigDecimal difference = balance.subtract(transactionBalance).setScale(4, RoundingMode.HALF_UP);
        if (difference.compareTo(ZERO) != 0) {
            recordCashTransaction(cashHolding, difference);
        }
    }

    private void recordCashTransaction(Holding cashHolding, BigDecimal delta) {
        if (cashHolding == null || cashHolding.getId() == null || delta == null || delta.compareTo(ZERO) == 0) {
            return;
        }

        Transaction transaction = new Transaction();
        transaction.setHoldingId(cashHolding.getId());
        transaction.setType(delta.compareTo(ZERO) > 0 ? "BUY" : "SELL");
        transaction.setQuantity(delta.abs().setScale(4, RoundingMode.HALF_UP));
        transaction.setPrice(ONE);
        transaction.setTradeDate(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    private boolean needsCashHoldingUpdate(Holding holding, BigDecimal balance, boolean hasDuplicates) {
        if (holding == null) {
            return true;
        }

        if (hasDuplicates || holding.getAssetType() != AssetType.CASH) {
            return true;
        }

        BigDecimal quantity = holding.getQuantity() == null ? ZERO : holding.getQuantity().setScale(4, RoundingMode.HALF_UP);
        BigDecimal normalizedBalance = balance == null ? ZERO : balance.setScale(4, RoundingMode.HALF_UP);
        if (quantity.compareTo(normalizedBalance) != 0) {
            return true;
        }

        if (holding.getTicker() == null || !"CASH".equalsIgnoreCase(holding.getTicker())) {
            return true;
        }
        if (holding.getPurchasePrice() == null || holding.getPurchasePrice().compareTo(ONE) != 0) {
            return true;
        }
        if (holding.getPurchasedata() == null) {
            return true;
        }
        return holding.getCurrency() == null || holding.getCurrency().isBlank();
    }

    private boolean isCashHolding(Holding holding) {
        return holding.getAssetType() == AssetType.CASH || (holding.getTicker() != null && "CASH".equalsIgnoreCase(holding.getTicker()));
    }
}
