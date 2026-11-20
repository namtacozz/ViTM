package com.vietbank.atm.service;

import com.vietbank.atm.model.CashVault;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CashDispenserTest {
    private final CashDispenser dispenser = new CashDispenser();

    @Test
    void shouldDispenseOptimalNotesWhenVaultIsFull() {
        CashVault fullVault = new CashVault(100, 100, 100, 100);
        CashDispenser.DispenseOutcome out = dispenser.calculateBreakdown(1_750_000L, fullVault, 40);
        assertNull(out.errorCode);
        assertEquals(3, out.breakdown.to500k);
        assertEquals(1, out.breakdown.to200k);
        assertEquals(0, out.breakdown.to100k);
        assertEquals(1, out.breakdown.to50k);
    }

    @Test
    void shouldFallbackWhen500kIsEmpty() {
        CashVault no500k = new CashVault(0, 20, 20, 20);
        CashDispenser.DispenseOutcome out = dispenser.calculateBreakdown(1_500_000L, no500k, 40);
        assertNull(out.errorCode);
        assertEquals(1_500_000L, out.breakdown.tongTien());
        assertEquals(7, out.breakdown.to200k);
        assertEquals(1, out.breakdown.to100k);
    }

    @Test
    void shouldFailWhenCannotMakeExactChange() {
        CashVault only500k = new CashVault(10, 0, 0, 0);
        CashDispenser.DispenseOutcome out = dispenser.calculateBreakdown(150_000L, only500k, 40);
        assertEquals("err.atm_insufficient_notes", out.errorCode);
    }

    @Test
    void shouldFailWhenExceedingMaxNotesPerTxn() {
        CashVault only50k = new CashVault(0, 0, 0, 200);
        CashDispenser.DispenseOutcome out = dispenser.calculateBreakdown(5_000_000L, only50k, 40);
        assertEquals("err.max_notes_exceeded", out.errorCode);
    }

    @Test
    void shouldValidateWithdrawLimitsAndBalance() {
        CashVault fullVault = new CashVault(100, 100, 100, 100);
        assertEquals("err.amount_multiple", dispenser.validateWithdrawRequest(120_000L, 0, 10_000_000L, fullVault));
        assertEquals("err.withdraw_limit_txn", dispenser.validateWithdrawRequest(6_000_000L, 0, 50_000_000L, fullVault));
        assertEquals("err.withdraw_limit_day", dispenser.validateWithdrawRequest(2_000_000L, 19_000_000L, 50_000_000L, fullVault));
        assertEquals("err.insufficient_balance", dispenser.validateWithdrawRequest(2_000_000L, 0, 1_500_000L, fullVault));
        assertNull(dispenser.validateWithdrawRequest(1_000_000L, 0, 5_000_000L, fullVault));
    }
}
