package com.vietbank.atm.dao;

import com.vietbank.atm.model.TransactionRecord;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;

public class TransactionDao extends BaseDao {
    public String insertTransaction(Connection c, String refCode, int accId, String type, long amount,
                                    long fee, long balanceAfter, String partnerAcc, String partnerBank,
                                    String remark, String atmLocation) throws SQLException {
        String sql = "INSERT INTO transactions (ma_tham_chieu, account_id, loai, so_tien, phi, so_du_sau, tk_doi_tac, ngan_hang_nhan, noi_dung, dia_diem_atm, thoi_gian) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, refCode, accId, type, amount, fee, balanceAfter, partnerAcc, partnerBank, remark, atmLocation);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    Timestamp ts = queryOne(c, "SELECT thoi_gian FROM transactions WHERE id = ?", rs -> rs.getTimestamp("thoi_gian"), keys.getLong(1));
                    if (ts != null) return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(ts);
                }
            }
        }
        return "";
    }

    public long sumTodayByType(Connection c, int accId, String type) throws SQLException {
        Long sum = queryOne(c, "SELECT COALESCE(SUM(so_tien), 0) AS total FROM transactions WHERE account_id = ? AND loai = ? AND thoi_gian >= ?",
            rs -> rs.getLong("total"), accId, type, Timestamp.valueOf(LocalDate.now().atStartOfDay()));
        return sum != null ? sum : 0L;
    }

    public int countByAccount(Connection c, int accId) throws SQLException {
        Integer cnt = queryOne(c, "SELECT COUNT(*) AS cnt FROM transactions WHERE account_id = ?", rs -> rs.getInt("cnt"), accId);
        return cnt != null ? cnt : 0;
    }

    public List<TransactionRecord> findPagedByAccount(Connection c, int accId, int limit, int offset) throws SQLException {
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return queryList(c,
            "SELECT id, ma_tham_chieu, account_id, loai, so_tien, phi, so_du_sau, tk_doi_tac, ngan_hang_nhan, noi_dung, dia_diem_atm, thoi_gian FROM transactions WHERE account_id = ? ORDER BY thoi_gian DESC, id DESC LIMIT ? OFFSET ?",
            rs -> {
                TransactionRecord r = new TransactionRecord();
                r.id = rs.getInt("id");
                r.maThamChieu = rs.getString("ma_tham_chieu");
                r.accountId = rs.getInt("account_id");
                r.loai = rs.getString("loai");
                r.soTien = rs.getLong("so_tien");
                r.phi = rs.getLong("phi");
                r.soDuSau = rs.getLong("so_du_sau");
                r.tkDoiTac = rs.getString("tk_doi_tac");
                r.nganHangNhan = rs.getString("ngan_hang_nhan");
                r.noiDung = rs.getString("noi_dung");
                r.diaDiemAtm = rs.getString("dia_diem_atm");
                Timestamp ts = rs.getTimestamp("thoi_gian");
                r.thoiGian = ts != null ? fmt.format(ts) : "";
                return r;
            }, accId, limit, offset);
    }
}
