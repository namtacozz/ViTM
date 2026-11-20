package com.vietbank.atm.service;

import com.vietbank.atm.config.AppConfig;
import com.vietbank.atm.model.CashBreakdown;
import com.vietbank.atm.model.CashVault;

public class CashDispenser {
    public static final long NOTE_UNIT = 50_000L;
    public static final int MAX_NOTES_PER_TXN = AppConfig.getInt("withdraw.max.notes", 40);
    public static final long WITHDRAW_LIMIT_TXN = AppConfig.getLong("withdraw.limit.txn", 5_000_000L);
    public static final long WITHDRAW_LIMIT_DAY = AppConfig.getLong("withdraw.limit.day", 20_000_000L);

    public static class DispenseOutcome {
        public CashBreakdown breakdown;
        public String errorCode;
    }

    public DispenseOutcome calculateBreakdown(long amount, CashVault vault, int maxNotes) {
        DispenseOutcome out = new DispenseOutcome();
        if (amount <= 0 || amount % NOTE_UNIT != 0) {
            out.errorCode = "err.amount_multiple";
            return out;
        }
        if (vault == null || vault.tongTien() < amount) {
            out.errorCode = "err.atm_insufficient_notes";
            return out;
        }

        int bestTotalNotes = Integer.MAX_VALUE;
        CashBreakdown best = null;
        boolean foundIgnoringMaxNotes = false;

        int max500 = (int) Math.min(vault.to500k, amount / 500_000L);
        for (int n500 = max500; n500 >= 0; n500--) {
            long rem500 = amount - n500 * 500_000L;
            int max200 = (int) Math.min(vault.to200k, rem500 / 200_000L);
            for (int n200 = max200; n200 >= 0; n200--) {
                long rem200 = rem500 - n200 * 200_000L;
                int max100 = (int) Math.min(vault.to100k, rem200 / 100_000L);
                for (int n100 = max100; n100 >= 0; n100--) {
                    long rem100 = rem200 - n100 * 100_000L;
                    int n50 = (int) (rem100 / 50_000L);
                    if (n50 * 50_000L != rem100 || n50 > vault.to50k) continue;

                    int totalNotes = n500 + n200 + n100 + n50;
                    foundIgnoringMaxNotes = true;
                    if (totalNotes <= maxNotes && totalNotes < bestTotalNotes) {
                        bestTotalNotes = totalNotes;
                        best = new CashBreakdown(n500, n200, n100, n50);
                        if (totalNotes == amount / 500_000L) {
                            out.breakdown = best;
                            return out;
                        }
                    }
                }
            }
        }

        if (best != null) {
            out.breakdown = best;
        } else if (foundIgnoringMaxNotes) {
            out.errorCode = "err.max_notes_exceeded";
        } else {
            out.errorCode = "err.atm_insufficient_notes";
        }
        return out;
    }

    public String validateWithdrawRequest(long amount, long withdrawnToday, long balance, CashVault vault) {
        if (amount <= 0) return "err.amount_invalid";
        if (amount % NOTE_UNIT != 0) return "err.amount_multiple";
        if (amount > WITHDRAW_LIMIT_TXN) return "err.withdraw_limit_txn";
        if (withdrawnToday + amount > WITHDRAW_LIMIT_DAY) return "err.withdraw_limit_day";
        if (amount > balance) return "err.insufficient_balance";
        DispenseOutcome outcome = calculateBreakdown(amount, vault, MAX_NOTES_PER_TXN);
        return outcome.errorCode;
    }
}
