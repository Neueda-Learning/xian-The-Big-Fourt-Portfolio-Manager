package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.AssetType;
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CashBalanceService cashBalanceService;

    @InjectMocks
    private HoldingService holdingService;

    @Test
    void updatingCashUsesSharedCashBalanceOnly() {
        Holding existing = cashHolding(7, 1, new BigDecimal("50000.0000"));
        Holding request = cashHolding(7, 1, new BigDecimal("52500.0000"));

        when(holdingRepository.getHoldingById(7)).thenReturn(existing, cashHolding(7, 1, new BigDecimal("52500.0000")));

        Holding updated = holdingService.update(request);

        verify(cashBalanceService).setSharedCashBalance(new BigDecimal("52500.0000"));
        verify(holdingRepository, never()).save(org.mockito.ArgumentMatchers.any(Holding.class));
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
        assertEquals(new BigDecimal("52500.0000"), updated.getQuantity());
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
}
