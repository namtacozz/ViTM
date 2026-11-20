package com.vietbank.atm.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {
    private final TransferService transferService = new TransferService();

    @Test
    void shouldRejectSelfTransfer() {
        assertEquals("err.transfer_self",
            transferService.validateTransferLogic(500_000L, 0L, 0L, 5_000_000L, "1234567890", "1234567890"));
    }

    @Test
    void shouldIncludeFeeInBalanceCheck() {
        assertEquals("err.insufficient_balance",
            transferService.validateTransferLogic(1_000_000L, 5_500L, 0L, 1_000_000L, "1234567890", "0011001234567"));
    }

    @Test
    void shouldRejectExceedingDailyLimit() {
        assertEquals("err.transfer_limit_day",
            transferService.validateTransferLogic(5_000_000L, 0L, 28_000_000L, 50_000_000L, "1234567890", "0987654321"));
    }
}
