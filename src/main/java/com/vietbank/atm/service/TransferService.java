package com.vietbank.atm.service;

import com.vietbank.atm.config.AppConfig;
import com.vietbank.atm.model.Account;
import com.vietbank.atm.model.BankInfo;
import com.vietbank.atm.model.TransactionResult;
import com.vietbank.atm.util.MoneyFormat;

import java.util.List;

public class TransferService extends BaseService {
    public static final long TRANSFER_LIMIT_TXN = AppConfig.getLong("transfer.limit.txn", 10_000_000L);
    public static final long TRANSFER_LIMIT_DAY = AppConfig.getLong("transfer.limit.day", 30_000_000L);

    public List<BankInfo> getSupportedBanks() {
        return List.of(
            new BankInfo("VB", "VietBank (Nội bộ)", 0L),
            new BankInfo("VCB", "Vietcombank", 5_500L),
            new BankInfo("TCB", "Techcombank", 5_500L),
            new BankInfo("MB", "MB Bank", 5_500L),
            new BankInfo("BIDV", "BIDV", 5_500L),
            new BankInfo("CTG", "VietinBank", 5_500L)
        );
    }

    public String lookupBeneficiary(String bankCode, String accNo) {
        if (accNo == null || accNo.trim().isEmpty()) return null;
        return withConn(c -> {
            String raw = "VB".equalsIgnoreCase(bankCode)
                ? accountDao.findInternalHolderName(c, accNo.trim())
                : accountDao.findExternalHolderName(c, bankCode, accNo.trim());
            return raw != null ? MoneyFormat.toAsciiUpperName(raw) : null;
        }, null);
    }

    public String validateTransferLogic(long amount, long fee, long transferredToday, long balance, String srcAccNo, String destAccNo) {
        if (destAccNo == null || destAccNo.trim().isEmpty()) return "err.transfer_acc_empty";
        if (srcAccNo != null && srcAccNo.trim().equals(destAccNo.trim())) return "err.transfer_self";
        if (amount <= 0) return "err.amount_invalid";
        if (amount > TRANSFER_LIMIT_TXN) return "err.transfer_limit_txn";
        if (transferredToday + amount > TRANSFER_LIMIT_DAY) return "err.transfer_limit_day";
        if (amount + fee > balance) return "err.insufficient_balance";
        return null;
    }

    public TransactionResult transfer(int accId, String cardNo, BankInfo destBank, String destAccNo, long amount, String remark) {
        String cleanDest = destAccNo == null ? "" : destAccNo.trim();
        String cleanRemark = (remark == null || remark.trim().isEmpty()) ? "CHUYEN KHOAN ATM" : remark.trim();
        long fee = destBank != null ? destBank.fee : 0L;
        String bankCode = destBank != null ? destBank.code : "VB";
        String bankName = destBank != null ? destBank.name : "VietBank";

        return inTransaction(c -> {
            Account srcAcc, internalDest = null;
            if ("VB".equalsIgnoreCase(bankCode)) {
                Integer destId = accountDao.findIdByAccountNumber(c, cleanDest);
                if (destId == null) return fail(c, "err.transfer_acc_not_found");
                if (destId == accId) return fail(c, "err.transfer_self");
                if (accId < destId) {
                    srcAcc = accountDao.findByIdForUpdate(c, accId);
                    internalDest = accountDao.findByIdForUpdate(c, destId);
                } else {
                    internalDest = accountDao.findByIdForUpdate(c, destId);
                    srcAcc = accountDao.findByIdForUpdate(c, accId);
                }
            } else {
                srcAcc = accountDao.findByIdForUpdate(c, accId);
            }
            if (srcAcc == null) return fail(c, "err.db_error");

            String err = validateTransferLogic(amount, fee, txnDao.sumTodayByType(c, accId, "CHUYEN"), srcAcc.soDu,
                "VB".equalsIgnoreCase(bankCode) ? srcAcc.soTaiKhoan : "", cleanDest);
            if (err != null) return fail(c, err);
            if (!"VB".equalsIgnoreCase(bankCode) && accountDao.findExternalHolderName(c, bankCode, cleanDest) == null) {
                return fail(c, "err.transfer_acc_not_found");
            }

            long newSrcBalance = srcAcc.soDu - amount - fee;
            String ref = ATMSystemService.generateRefCode();
            accountDao.updateBalance(c, accId, newSrcBalance);
            if (internalDest != null) {
                long newDestBalance = internalDest.soDu + amount;
                accountDao.updateBalance(c, internalDest.id, newDestBalance);
                txnDao.insertTransaction(c, ref + "R", internalDest.id, "NHAN", amount, 0L, newDestBalance, srcAcc.soTaiKhoan, "VietBank", cleanRemark, ATMSystemService.ATM_LOCATION);
            }
            return recordSuccess(c, ref, accId, cardNo, "CHUYEN", amount, fee, newSrcBalance, cleanDest, bankName,
                cleanRemark, "TRANSFER", " To=" + bankCode + ":" + cleanDest + " Amount=" + amount + " Fee=" + fee, null);
        }, fail("err.db_error"));
    }
}
