package com.vietbank.atm.service;

import com.vietbank.atm.model.Account;
import com.vietbank.atm.model.Card;
import com.vietbank.atm.model.TransactionResult;
import com.vietbank.atm.util.Hasher;

public class PinService extends BaseService {
    public String validatePinChangeInputs(String oldPin, String newPin, String confirmPin) {
        if (!Hasher.isValidPinFormat(oldPin) || !Hasher.isValidPinFormat(newPin)) return "err.pin_format";
        if (oldPin.equals(newPin)) return "err.pin_same_old";
        if (Hasher.isWeakPin(newPin)) return "err.pin_weak";
        return newPin.equals(confirmPin) ? null : "err.pin_mismatch";
    }

    public TransactionResult changePin(String cardNo, int accId, String oldPin, String newPin, String confirmPin) {
        String err = validatePinChangeInputs(oldPin, newPin, confirmPin);
        if (err != null) return fail(err);
        String cleanCard = cardNo.replaceAll("\\s+", "");
        String newHash = Hasher.hashPinPBKDF2(newPin);

        return inTransaction(c -> {
            Card card = cardDao.findByCardNumber(c, cleanCard);
            if (card == null || !Hasher.verifyPin(cleanCard, oldPin, card.pinHash)) return fail(c, "err.pin_old_wrong");
            Account acc = accountDao.findById(c, accId);
            cardDao.updatePin(c, cleanCard, newHash);
            return recordSuccess(c, ATMSystemService.generateRefCode(), accId, cleanCard, "DOI_PIN", 0L, 0L,
                acc != null ? acc.soDu : 0L, "-", "VietBank", "Đổi mã PIN thẻ ATM", "CHANGE_PIN", "", null);
        }, fail("err.db_error"));
    }
}
