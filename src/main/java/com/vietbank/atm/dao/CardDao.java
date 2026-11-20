package com.vietbank.atm.dao;

import com.vietbank.atm.model.Card;
import java.sql.*;

public class CardDao extends BaseDao {
    public Card findByCardNumber(Connection c, String cardNo) throws SQLException {
        return queryOne(c, "SELECT so_the, pin, account_id, so_lan_sai, trang_thai FROM cards WHERE so_the = ?",
            rs -> new Card(rs.getString("so_the"), rs.getString("pin"), rs.getInt("account_id"), rs.getInt("so_lan_sai"), rs.getString("trang_thai")), cardNo);
    }

    public void updateFailedAttempts(Connection c, String cardNo, int attempts, String status) throws SQLException {
        update(c, "UPDATE cards SET so_lan_sai = ?, trang_thai = ? WHERE so_the = ?", attempts, status, cardNo);
    }

    public void resetFailedAttempts(Connection c, String cardNo) throws SQLException {
        update(c, "UPDATE cards SET so_lan_sai = 0 WHERE so_the = ?", cardNo);
    }

    public void updatePin(Connection c, String cardNo, String newPinHash) throws SQLException {
        update(c, "UPDATE cards SET pin = ? WHERE so_the = ?", newPinHash, cardNo);
    }
}
