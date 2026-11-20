package com.vietbank.atm.service;

import com.vietbank.atm.model.Account;
import com.vietbank.atm.model.Card;
import com.vietbank.atm.model.LoginResult;
import com.vietbank.atm.util.Hasher;
import com.vietbank.atm.util.MoneyFormat;

public class AuthService extends BaseService {
    public static final int MAX_PIN_WRONG = 3;

    public String checkCard(String cardNo) {
        String clean = cardNo == null ? "" : cardNo.replaceAll("\\s+", "");
        if (!clean.matches("\\d{16}")) return "INVALID_FORMAT";
        return withConn(c -> {
            Card card = cardDao.findByCardNumber(c, clean);
            return card == null ? "NOT_FOUND" : card.trangThai;
        }, "DB_ERROR");
    }

    public LoginResult login(String cardNo, String pin) {
        LoginResult errRes = new LoginResult();
        errRes.errorCode = "err.db_error";
        String cleanCard = cardNo == null ? "" : cardNo.replaceAll("\\s+", "");
        if (!Hasher.isValidPinFormat(pin)) {
            errRes.errorCode = "err.pin_format";
            return errRes;
        }
        return inTransaction(c -> {
            LoginResult res = new LoginResult();
            Card card = cardDao.findByCardNumber(c, cleanCard);
            if (card == null) {
                res.errorCode = "err.card_not_found";
                c.rollback();
                return res;
            }
            if (!"ACTIVE".equalsIgnoreCase(card.trangThai)) {
                res.locked = true;
                res.errorCode = "err.card_locked";
                c.rollback();
                return res;
            }
            if (Hasher.verifyPin(cleanCard, pin, card.pinHash)) {
                cardDao.resetFailedAttempts(c, cleanCard);
                Account acc = accountDao.findById(c, card.accountId);
                auditDao.log(c, ATMSystemService.ATM_ID, MoneyFormat.maskCardNumber(cleanCard), acc.id, "LOGIN", "PIN verified", "SUCCESS");
                c.commit();
                res.success = true;
                res.accId = acc.id;
                res.tenChuTk = acc.tenChuTk;
                res.soTaiKhoan = acc.soTaiKhoan;
                res.soDu = acc.soDu;
                return res;
            }
            int nextWrong = card.soLanSai + 1;
            String nextStatus = nextWrong >= MAX_PIN_WRONG ? "LOCKED" : "ACTIVE";
            cardDao.updateFailedAttempts(c, cleanCard, nextWrong, nextStatus);
            auditDao.log(c, ATMSystemService.ATM_ID, MoneyFormat.maskCardNumber(cleanCard), card.accountId,
                "LOGIN_FAILED", "Attempt " + nextWrong + "/" + MAX_PIN_WRONG, nextStatus);
            c.commit();
            res.locked = "LOCKED".equals(nextStatus);
            res.remainingAttempts = Math.max(0, MAX_PIN_WRONG - nextWrong);
            res.errorCode = res.locked ? "err.card_locked" : "err.pin_wrong";
            return res;
        }, errRes);
    }
}
