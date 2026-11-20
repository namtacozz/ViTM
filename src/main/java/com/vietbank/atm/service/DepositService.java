package com.vietbank.atm.service;

import com.vietbank.atm.config.AppConfig;
import com.vietbank.atm.model.Account;
import com.vietbank.atm.model.CashBreakdown;
import com.vietbank.atm.model.TransactionResult;

public class DepositService extends BaseService {
    public static final long DEPOSIT_LIMIT_TXN = AppConfig.getLong("deposit.limit.txn", 50_000_000L);

    public TransactionResult deposit(int accId, String cardNo, CashBreakdown countedNotes) {
        if (countedNotes == null || countedNotes.tongTo() <= 0 || countedNotes.tongTien() <= 0) return fail("err.deposit_empty");
        long amount = countedNotes.tongTien();
        if (amount > DEPOSIT_LIMIT_TXN) return fail("err.deposit_limit");

        return inTransaction(c -> {
            Account acc = accountDao.findByIdForUpdate(c, accId);
            if (acc == null) return fail(c, "err.db_error");
            long newBalance = acc.soDu + amount;
            accountDao.updateBalance(c, accId, newBalance);
            cashDao.addNotes(c, countedNotes);
            return recordSuccess(c, ATMSystemService.generateRefCode(), accId, cardNo, "NAP", amount, 0L, newBalance, "-", "VietBank",
                "Nộp tiền mặt tại ATM", "DEPOSIT", " Amount=" + amount, countedNotes);
        }, fail("err.db_error"));
    }
}
