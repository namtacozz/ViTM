package com.vietbank.atm.service;

import com.vietbank.atm.config.DbConfig;
import com.vietbank.atm.dao.*;
import com.vietbank.atm.model.CashBreakdown;
import com.vietbank.atm.model.TransactionResult;
import com.vietbank.atm.util.MoneyFormat;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class BaseService {
    protected final AccountDao accountDao = new AccountDao();
    protected final CardDao cardDao = new CardDao();
    protected final TransactionDao txnDao = new TransactionDao();
    protected final CashDao cashDao = new CashDao();
    protected final AuditLogDao auditDao = new AuditLogDao();

    @FunctionalInterface
    protected interface SqlWork<T> {
        T run(Connection c) throws SQLException;
    }

    protected <T> T withConn(SqlWork<T> work, T onError) {
        try (Connection c = DbConfig.getConnection()) {
            return work.run(c);
        } catch (SQLException e) {
            return onError;
        }
    }

    protected <T> T inTransaction(SqlWork<T> work, T onError) {
        try (Connection c = DbConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                return work.run(c);
            } catch (SQLException ex) {
                try { c.rollback(); } catch (SQLException ignored) {}
                return onError;
            }
        } catch (SQLException e) {
            return onError;
        }
    }

    protected TransactionResult fail(String code) {
        TransactionResult r = new TransactionResult();
        r.maLoi = code;
        return r;
    }

    protected TransactionResult fail(Connection c, String code) throws SQLException {
        c.rollback();
        return fail(code);
    }

    protected TransactionResult recordSuccess(Connection c, String ref, int accId, String cardNo, String type, long amount,
                                            long fee, long newBal, String partner, String bank, String remark,
                                            String auditAct, String auditDetail, CashBreakdown bd) throws SQLException {
        String ts = txnDao.insertTransaction(c, ref, accId, type, amount, fee, newBal, partner, bank, remark, ATMSystemService.ATM_LOCATION);
        auditDao.log(c, ATMSystemService.ATM_ID, MoneyFormat.maskCardNumber(cardNo), accId, auditAct, "Ref=" + ref + auditDetail, "SUCCESS");
        c.commit();
        TransactionResult r = new TransactionResult();
        r.thanhCong = true;
        r.soDuMoi = newBal;
        r.phi = fee;
        r.maThamChieu = ref;
        r.thoiGian = ts;
        r.diaDiemAtm = ATMSystemService.ATM_LOCATION;
        r.breakdown = bd;
        return r;
    }
}
