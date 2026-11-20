package com.vietbank.atm.service;

import com.vietbank.atm.model.Account;
import com.vietbank.atm.model.CashVault;
import com.vietbank.atm.model.TransactionResult;
import com.vietbank.atm.util.MoneyFormat;

public class WithdrawService extends BaseService {
    private final CashDispenser dispenser = new CashDispenser();

    public TransactionResult withdraw(int accId, String cardNo, long amount) {
        return inTransaction(c -> {
            if (!"ONLINE".equalsIgnoreCase(cashDao.getConfig(c, "atm_status", "ONLINE"))) return fail(c, "err.atm_maintenance");
            Account acc = accountDao.findByIdForUpdate(c, accId);
            if (acc == null) return fail(c, "err.db_error");
            CashVault vault = cashDao.loadVault(c);
            String err = dispenser.validateWithdrawRequest(amount, txnDao.sumTodayByType(c, accId, "RUT"), acc.soDu, vault);
            if (err != null) {
                auditDao.log(c, ATMSystemService.ATM_ID, MoneyFormat.maskCardNumber(cardNo), accId, "WITHDRAW_REJECTED", "Amount=" + amount + " Reason=" + err, "REJECTED");
                c.commit();
                return fail(err);
            }
            CashDispenser.DispenseOutcome outcome = dispenser.calculateBreakdown(amount, vault, CashDispenser.MAX_NOTES_PER_TXN);
            if (outcome.errorCode != null) return fail(c, outcome.errorCode);
            long newBalance = acc.soDu - amount;
            accountDao.updateBalance(c, accId, newBalance);
            cashDao.deductNotes(c, outcome.breakdown);
            return recordSuccess(c, ATMSystemService.generateRefCode(), accId, cardNo, "RUT", amount, 0L, newBalance, "-", "VITBANK",
                "Rút tiền mặt tại ViTM", "WITHDRAW", " Amount=" + amount, outcome.breakdown);
        }, fail("err.db_error"));
    }
}
