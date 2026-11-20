package com.vietbank.atm.dao;

import java.sql.*;
import java.util.*;

public abstract class BaseDao {
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected <T> T queryOne(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, params); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapper.map(rs) : null;
        }
    }

    protected <T> List<T> queryList(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> list = new ArrayList<>();
        try (PreparedStatement ps = prepare(c, sql, params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapper.map(rs));
        }
        return list;
    }

    protected int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, params)) {
            return ps.executeUpdate();
        }
    }

    protected PreparedStatement prepare(Connection c, String sql, Object... params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        bind(ps, params);
        return ps;
    }

    protected void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }
}
