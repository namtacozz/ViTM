package com.vietbank.atm.service;

import com.vietbank.atm.config.AppConfig;
import com.vietbank.atm.model.Account;
import com.vietbank.atm.model.CashVault;
import com.vietbank.atm.model.TransactionRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ATMSystemService extends BaseService {
    public static final String ATM_ID = AppConfig.getString("atm.id", "VITB-DN-086");
    public static final String ATM_LOCATION = AppConfig.getString("atm.location", "128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng");

    public static String generateRefCode() {
        return "VITB" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    public String getAtmStatus() {
        return withConn(c -> cashDao.getConfig(c, "atm_status", "ONLINE"), "DB_ERROR");
    }

    public boolean setAtmStatus(String status) {
        return withConn(c -> {
            cashDao.setConfig(c, "atm_status", status);
            auditDao.log(c, ATM_ID, null, null, "ATM_STATUS_CHANGE", "Status=" + status, "OK");
            return true;
        }, false);
    }

    public CashVault getVaultStatus() {
        return withConn(cashDao::loadVault, null);
    }

    public long getBalance(int accId) {
        return withConn(c -> {
            Account a = accountDao.findById(c, accId);
            return a != null ? a.soDu : -1L;
        }, -1L);
    }

    public int countTransactions(int accId) {
        return withConn(c -> txnDao.countByAccount(c, accId), 0);
    }

    public List<TransactionRecord> getPagedStatement(int accId, int page, int pageSize) {
        return withConn(c -> txnDao.findPagedByAccount(c, accId, pageSize, Math.max(0, (page - 1) * pageSize)), List.of());
    }
}
