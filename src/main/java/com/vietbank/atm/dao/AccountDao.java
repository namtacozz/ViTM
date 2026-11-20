package com.vietbank.atm.dao;

import com.vietbank.atm.model.Account;
import java.sql.*;

public class AccountDao extends BaseDao {
    private static final RowMapper<Account> MAPPER = rs -> new Account(
        rs.getInt("id"), rs.getString("so_tai_khoan"), rs.getString("ten_chu_tk"), rs.getLong("so_du")
    );

    public Account findById(Connection c, int accId) throws SQLException {
        return queryOne(c, "SELECT id, so_tai_khoan, ten_chu_tk, so_du FROM accounts WHERE id = ?", MAPPER, accId);
    }

    public Account findByIdForUpdate(Connection c, int accId) throws SQLException {
        return queryOne(c, "SELECT id, so_tai_khoan, ten_chu_tk, so_du FROM accounts WHERE id = ? FOR UPDATE", MAPPER, accId);
    }

    public Integer findIdByAccountNumber(Connection c, String accNo) throws SQLException {
        return queryOne(c, "SELECT id FROM accounts WHERE so_tai_khoan = ?", rs -> rs.getInt("id"), accNo);
    }

    public Account findByAccountNumberForUpdate(Connection c, String accNo) throws SQLException {
        return queryOne(c, "SELECT id, so_tai_khoan, ten_chu_tk, so_du FROM accounts WHERE so_tai_khoan = ? FOR UPDATE", MAPPER, accNo);
    }

    public String findInternalHolderName(Connection c, String accNo) throws SQLException {
        return queryOne(c, "SELECT ten_chu_tk FROM accounts WHERE so_tai_khoan = ?", rs -> rs.getString("ten_chu_tk"), accNo);
    }

    public String findExternalHolderName(Connection c, String bankCode, String accNo) throws SQLException {
        return queryOne(c, "SELECT ten_chu_tk FROM external_accounts WHERE ma_ngan_hang = ? AND so_tai_khoan = ?", rs -> rs.getString("ten_chu_tk"), bankCode, accNo);
    }

    public void updateBalance(Connection c, int accId, long newBalance) throws SQLException {
        update(c, "UPDATE accounts SET so_du = ? WHERE id = ?", newBalance, accId);
    }
}
