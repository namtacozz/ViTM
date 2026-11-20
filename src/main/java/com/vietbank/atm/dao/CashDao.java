package com.vietbank.atm.dao;

import com.vietbank.atm.model.CashBreakdown;
import com.vietbank.atm.model.CashVault;
import java.sql.*;

public class CashDao extends BaseDao {
    public CashVault loadVault(Connection c) throws SQLException {
        CashVault v = new CashVault();
        queryList(c, "SELECT menh_gia, so_to FROM atm_vault", rs -> {
            int d = rs.getInt("menh_gia"), n = rs.getInt("so_to");
            if (d == 500000) v.to500k = n;
            else if (d == 200000) v.to200k = n;
            else if (d == 100000) v.to100k = n;
            else if (d == 50000) v.to50k = n;
            return null;
        });
        return v;
    }

    public void deductNotes(Connection c, CashBreakdown b) throws SQLException { applyDelta(c, b, -1); }
    public void addNotes(Connection c, CashBreakdown b) throws SQLException { applyDelta(c, b, 1); }

    private void applyDelta(Connection c, CashBreakdown b, int sign) throws SQLException {
        int[][] pairs = {{500000, b.to500k}, {200000, b.to200k}, {100000, b.to100k}, {50000, b.to50k}};
        for (int[] p : pairs) {
            if (p[1] != 0) update(c, "UPDATE atm_vault SET so_to = so_to + ? WHERE menh_gia = ?", sign * p[1], p[0]);
        }
    }

    public String getConfig(Connection c, String key, String def) throws SQLException {
        String val = queryOne(c, "SELECT config_value FROM atm_config WHERE config_key = ?", rs -> rs.getString("config_value"), key);
        return val != null ? val : def;
    }

    public void setConfig(Connection c, String key, String value) throws SQLException {
        update(c, "INSERT INTO atm_config (config_key, config_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE config_value = ?", key, value, value);
    }
}
