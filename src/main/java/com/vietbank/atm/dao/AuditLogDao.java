package com.vietbank.atm.dao;

import java.sql.*;

public class AuditLogDao extends BaseDao {
    public void log(Connection c, String atmId, String cardNo, Integer accId, String action, String detail, String result) {
        try {
            update(c, "INSERT INTO audit_logs (atm_id, so_the, account_id, hanh_dong, chi_tiet, ket_qua) VALUES (?, ?, ?, ?, ?, ?)",
                atmId, cardNo, accId, action, detail, result);
        } catch (SQLException ignored) {}
    }
}
