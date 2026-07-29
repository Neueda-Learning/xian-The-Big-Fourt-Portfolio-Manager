package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.AssetType;
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.example.xianthebigfourtportfoliomanager.repository.PortfolioRepository;
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashBalanceServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CashBalanceService cashBalanceService;

    @Test
    void applySharedCashDeltaUpdatesHoldingAndWritesCashTransaction() {
        portfolio portfolioOne = new portfolio(1, "One", "", null, null);
        Holding cashOne = cashHolding(1, 1, new BigDecimal("50000.0000"));

        when(portfolioRepository.getAllPortfolios()).thenReturn(List.of(portfolioOne));
        when(holdingRepository.getHoldingsByPortfolioId(1)).thenReturn(List.of(cashOne));
        when(holdingRepository.getHoldingById(1)).thenReturn(cashOne);
        when(holdingRepository.update(any(Holding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.getTransactionsByHoldingId(1)).thenReturn(List.of(cashTransaction(1, "BUY", new BigDecimal("50000.0000"))));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal next = cashBalanceService.applySharedCashDelta(new BigDecimal("-2500.0000"));

        assertEquals(new BigDecimal("47500.0000"), next);

        ArgumentCaptor<Holding> updateCaptor = ArgumentCaptor.forClass(Holding.class);
        verify(holdingRepository).update(updateCaptor.capture());
        assertEquals(new BigDecimal("47500.0000"), updateCaptor.getValue().getQuantity());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals("SELL", transactionCaptor.getValue().getType());
        assertEquals(new BigDecimal("2500.0000"), transactionCaptor.getValue().getQuantity());
        assertEquals(BigDecimal.ONE, transactionCaptor.getValue().getPrice());
        verify(holdingRepository, never()).save(any(Holding.class));
    }

    @Test
    void setSharedCashBalanceUpdatesHoldingAndWritesCashTransaction() {
        portfolio portfolioOne = new portfolio(1, "One", "", null, null);
        Holding cashOne = cashHolding(1, 1, new BigDecimal("50000.0000"));

        when(portfolioRepository.getAllPortfolios()).thenReturn(List.of(portfolioOne));
        when(holdingRepository.getHoldingsByPortfolioId(1)).thenReturn(List.of(cashOne));
        when(holdingRepository.getHoldingById(1)).thenReturn(cashOne);
        when(holdingRepository.update(any(Holding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.getTransactionsByHoldingId(1)).thenReturn(List.of(cashTransaction(1, "BUY", new BigDecimal("50000.0000"))));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal next = cashBalanceService.setSharedCashBalance(new BigDecimal("52500.0000"));

        assertEquals(new BigDecimal("52500.0000"), next);

        ArgumentCaptor<Holding> updateCaptor = ArgumentCaptor.forClass(Holding.class);
        verify(holdingRepository).update(updateCaptor.capture());
        assertEquals(new BigDecimal("52500.0000"), updateCaptor.getValue().getQuantity());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals("BUY", transactionCaptor.getValue().getType());
        assertEquals(new BigDecimal("2500.0000"), transactionCaptor.getValue().getQuantity());
        assertEquals(BigDecimal.ONE, transactionCaptor.getValue().getPrice());
        verify(holdingRepository, never()).save(any(Holding.class));
    }

    @Test
    void getSharedCashHoldingConsolidatesDuplicateCashRows() {
        portfolio portfolioOne = new portfolio(1, "One", "", null, null);
        portfolio portfolioTwo = new portfolio(2, "Two", "", null, null);
        Holding cashOne = cashHolding(1, 1, new BigDecimal("50000.0000"));
        Holding cashTwo = cashHolding(2, 2, new BigDecimal("1000.0000"));
        Holding consolidated = cashHolding(1, 1, new BigDecimal("51000.0000"));

        when(portfolioRepository.getAllPortfolios()).thenReturn(List.of(portfolioOne, portfolioTwo));
        when(holdingRepository.getHoldingsByPortfolioId(1)).thenReturn(List.of(cashOne));
        when(holdingRepository.getHoldingsByPortfolioId(2)).thenReturn(List.of(cashTwo));
        when(holdingRepository.update(any(Holding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(holdingRepository.getHoldingById(1)).thenReturn(consolidated);
        when(transactionRepository.getTransactionsByHoldingId(1)).thenReturn(List.of(cashTransaction(1, "BUY", new BigDecimal("51000.0000"))));

        Holding shared = cashBalanceService.getSharedCashHolding();

        assertEquals(new BigDecimal("51000.0000"), shared.getQuantity());
        verify(transactionRepository).reassignHoldingId(2, 1);
        verify(holdingRepository).deleteById(2);
        verify(holdingRepository, times(1)).update(any(Holding.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    private Holding cashHolding(int id, int portfolioId, BigDecimal quantity) {
        Holding holding = new Holding();
        holding.setId(id);
        holding.setPortfolioId(portfolioId);
        holding.setAssetType(AssetType.CASH);
        holding.setTicker("CASH");
        holding.setQuantity(quantity);
        holding.setPurchasePrice(BigDecimal.ONE);
        holding.setPurchasedata(LocalDate.of(2026, 1, 10));
        holding.setCurrency("USD");
        return holding;
    }

    private Transaction cashTransaction(int holdingId, String type, BigDecimal quantity) {
        Transaction transaction = new Transaction();
        transaction.setHoldingId(holdingId);
        transaction.setType(type);
        transaction.setQuantity(quantity);
        transaction.setPrice(BigDecimal.ONE);
        return transaction;
    }
}
