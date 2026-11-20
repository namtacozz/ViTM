import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class DB {
    public static final String ATM_ID = "VB-DN-086";
    public static final int MAX_PIN_SAI = 3;
    public static final long HAN_MUC_LAN = 5_000_000L;
    public static final long HAN_MUC_NGAY = 20_000_000L;
    public static final long HAN_MUC_CK_LAN = 5_000_000L;
    public static final long HAN_MUC_CK_NGAY = 20_000_000L;
    public static final long HAN_MUC_NAP_LAN = 50_000_000L;
    public static final long BOI_SO = 50_000L;
    public static final int TIMEOUT_GIAY = 30;

    private static String URL = "jdbc:mysql://localhost:3306/atm_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&connectTimeout=3000&socketTimeout=5000";
    private static String USER = "root";
    private static String PASS = "";

    static {
        loadConfig();
    }

    private static void loadConfig() {
        if (System.getenv("DB_URL") != null) URL = System.getenv("DB_URL");
        if (System.getenv("DB_USER") != null) USER = System.getenv("DB_USER");
        if (System.getenv("DB_PASS") != null) PASS = System.getenv("DB_PASS");

        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                Properties p = new Properties();
                p.load(fis);
                if (p.getProperty("db.url") != null) URL = p.getProperty("db.url");
                if (p.getProperty("db.user") != null) USER = p.getProperty("db.user");
                if (p.getProperty("db.password") != null) PASS = p.getProperty("db.password");
            } catch (Exception ignored) {}
        }
    }

    public static String getUser() { return USER; }
    public static void setUser(String u) { USER = u; }
    public static String getPass() { return PASS; }
    public static void setPass(String p) { PASS = p; }

    public static class UserSession {
        public boolean success;
        public boolean locked;
        public int remainingAttempts;
        public int accId;
        public String tenChuTk;
        public String soTaiKhoan;
        public long soDu;
    }

    public static class GiaoDichResult {
        public boolean thanhCong;
        public String maLoi;
        public long soDuMoi;
        public String thoiGian;
        public String maGiaoDich;
    }

    public static class CashBreakdown {
        public int to500k;
        public int to200k;
        public int to100k;
        public int to50k;
        public int tongTo() {
            return to500k + to200k + to100k + to50k;
        }
    }

    public static CashBreakdown chiaCoSoToTien(long soTien) {
        CashBreakdown b = new CashBreakdown();
        long rem = soTien;
        b.to500k = (int) (rem / 500_000L); rem %= 500_000L;
        b.to200k = (int) (rem / 200_000L); rem %= 200_000L;
        b.to100k = (int) (rem / 100_000L); rem %= 100_000L;
        b.to50k  = (int) (rem / 50_000L);  rem %= 50_000L;
        return b;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static boolean kiemTraKetNoi() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    public static String dinhDangTien(long st) {
        return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(st) + " VNĐ";
    }

    public static String hashPin(String soThe, String pin) {
        String soTheThuan = soThe.replaceAll("\\s+", "");
        String raw = soTheThuan + ":" + pin.trim();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String kiemTraRut(long soTien, long daRutHomNay, long soDu) {
        if (soTien <= 0) return "SO_TIEN_KHONG_HOP_LE";
        if (soTien % BOI_SO != 0) return "BOI_SO";
        if (soTien > HAN_MUC_LAN) return "HAN_MUC_LAN";
        if (daRutHomNay + soTien > HAN_MUC_NGAY) return "HAN_MUC_NGAY";
        if (soTien > soDu) return "KHONG_DU_SO_DU";
        return null;
    }

    public static String kiemTraThe(String soThe) {
        String soTheThuan = soThe.replaceAll("\\s+", "");
        String sql = "SELECT trang_thai FROM cards WHERE so_the = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, soTheThuan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("trang_thai");
            }
            return "NOT_FOUND";
        } catch (SQLException e) {
            return "DB_ERROR:" + e.getMessage();
        }
    }

    public static String khoiTaoCSDL(String user, String pass) {
        String rootUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&connectTimeout=3000";
        try (Connection conn = DriverManager.getConnection(rootUrl, user, pass);
             Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS atm_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            st.executeUpdate("USE atm_db");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS accounts (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "so_tai_khoan VARCHAR(20) UNIQUE NOT NULL," +
                    "ten_chu_tk VARCHAR(100) NOT NULL," +
                    "so_du BIGINT NOT NULL DEFAULT 0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS cards (" +
                    "so_the VARCHAR(16) PRIMARY KEY," +
                    "pin VARCHAR(64) NOT NULL," +
                    "account_id INT NOT NULL," +
                    "so_lan_sai INT DEFAULT 0," +
                    "trang_thai VARCHAR(10) DEFAULT 'ACTIVE'," +
                    "FOREIGN KEY (account_id) REFERENCES accounts(id))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "account_id INT NOT NULL," +
                    "loai VARCHAR(20) NOT NULL," +
                    "so_tien BIGINT DEFAULT 0," +
                    "so_du_sau BIGINT," +
                    "tk_doi_tac VARCHAR(20)," +
                    "thoi_gian DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (account_id) REFERENCES accounts(id))");

            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM accounts")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    st.executeUpdate("INSERT INTO accounts (id, so_tai_khoan, ten_chu_tk, so_du) VALUES " +
                            "(1, '1234567890', 'NGUYỄN VĂN AN', 50000000)," +
                            "(2, '0987654321', 'TRẦN THỊ BÍCH', 1000000)," +
                            "(3, '1111222233', 'LÊ HOÀNG MINH', 30000000)");
                    st.executeUpdate("INSERT INTO cards (so_the, pin, account_id, so_lan_sai, trang_thai) VALUES " +
                            "('9704001234567890', '67590a0da7df033b72ae36cbf36b2965ea1c497b4635c36c2bdb2a1867c7b457', 1, 0, 'ACTIVE')," +
                            "('9704009876543210', 'd5a7c9f79076a0a24e97831badaf73be1b38d2614143578f8dfc90a2ca461280', 2, 0, 'ACTIVE')," +
                            "('9704001111222233', '7e88a746a766befa474f0585e08d5275eab7b30f6ba0ecd8f0071226d35f49f9', 3, 0, 'LOCKED')");
                }
            }
            USER = user;
            PASS = pass;
            try (FileOutputStream fos = new FileOutputStream("db.properties")) {
                Properties p = new Properties();
                p.setProperty("db.url", URL);
                p.setProperty("db.user", USER);
                p.setProperty("db.password", PASS);
                p.store(fos, "VietBank ATM DB Config");
            } catch (Exception ignored) {}
            return "OK";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    public static UserSession dangNhap(String soThe, String pin) {
        String soTheThuan = soThe.replaceAll("\\s+", "");
        String sql = "SELECT c.pin, c.so_lan_sai, c.trang_thai, a.id, a.ten_chu_tk, a.so_tai_khoan, a.so_du " +
                     "FROM cards c JOIN accounts a ON c.account_id = a.id WHERE c.so_the = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, soTheThuan);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                UserSession s = new UserSession();
                String trangThai = rs.getString("trang_thai");
                if ("LOCKED".equalsIgnoreCase(trangThai)) {
                    s.locked = true;
                    return s;
                }

                String dbPin = rs.getString("pin");
                String hashedInput = hashPin(soTheThuan, pin);

                if (dbPin.equalsIgnoreCase(hashedInput)) {
                    try (PreparedStatement upd = conn.prepareStatement("UPDATE cards SET so_lan_sai = 0 WHERE so_the = ?")) {
                        upd.setString(1, soTheThuan);
                        upd.executeUpdate();
                    }
                    s.success = true;
                    s.accId = rs.getInt("id");
                    s.tenChuTk = rs.getString("ten_chu_tk");
                    s.soTaiKhoan = rs.getString("so_tai_khoan");
                    s.soDu = rs.getLong("so_du");
                    return s;
                } else {
                    int newSai = 0;
                    try (PreparedStatement updInc = conn.prepareStatement("UPDATE cards SET so_lan_sai = so_lan_sai + 1 WHERE so_the = ?")) {
                        updInc.setString(1, soTheThuan);
                        updInc.executeUpdate();
                    }
                    try (PreparedStatement psRead = conn.prepareStatement("SELECT so_lan_sai FROM cards WHERE so_the = ?")) {
                        psRead.setString(1, soTheThuan);
                        try (ResultSet rsSai = psRead.executeQuery()) {
                            if (rsSai.next()) newSai = rsSai.getInt("so_lan_sai");
                        }
                    }

                    if (newSai >= MAX_PIN_SAI) {
                        try (PreparedStatement updLock = conn.prepareStatement("UPDATE cards SET trang_thai = 'LOCKED' WHERE so_the = ?")) {
                            updLock.setString(1, soTheThuan);
                            updLock.executeUpdate();
                        }
                        s.locked = true;
                        s.remainingAttempts = 0;
                    } else {
                        s.remainingAttempts = MAX_PIN_SAI - newSai;
                    }
                    return s;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static long laySoDu(int accId) {
        String sql = "SELECT so_du FROM accounts WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("so_du");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long tongRutHomNay(Connection conn, int accId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(so_tien), 0) AS tong FROM transactions WHERE account_id = ? AND loai = 'RUT' AND DATE(thoi_gian) = CURDATE()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("tong");
            }
        }
        return 0;
    }

    public static long tongChuyenHomNay(Connection conn, int accId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(so_tien), 0) AS tong FROM transactions WHERE account_id = ? AND loai = 'CHUYEN' AND DATE(thoi_gian) = CURDATE()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("tong");
            }
        }
        return 0;
    }

    public static GiaoDichResult rutTien(int accId, long soTien) {
        GiaoDichResult res = new GiaoDichResult();
        String sel = "SELECT so_du FROM accounts WHERE id = ? FOR UPDATE";
        String upd = "UPDATE accounts SET so_du = so_du - ? WHERE id = ?";
        String ins = "INSERT INTO transactions (account_id, loai, so_tien, so_du_sau) VALUES (?, 'RUT', ?, ?)";
        String selTime = "SELECT DATE_FORMAT(thoi_gian, '%d/%m/%Y %H:%i:%s') AS tg, id FROM transactions WHERE id = LAST_INSERT_ID()";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                long soDu = 0;
                try (PreparedStatement ps = conn.prepareStatement(sel)) {
                    ps.setInt(1, accId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            res.maLoi = "LOI_HE_THONG";
                            return res;
                        }
                        soDu = rs.getLong("so_du");
                    }
                }

                long daRutHomNay = tongRutHomNay(conn, accId);
                String loi = kiemTraRut(soTien, daRutHomNay, soDu);
                if (loi != null) {
                    conn.rollback();
                    res.maLoi = loi;
                    return res;
                }

                long soDuMoi = soDu - soTien;
                try (PreparedStatement ps = conn.prepareStatement(upd)) {
                    ps.setLong(1, soTien);
                    ps.setInt(2, accId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(ins)) {
                    ps.setInt(1, accId);
                    ps.setLong(2, soTien);
                    ps.setLong(3, soDuMoi);
                    ps.executeUpdate();
                }

                String tg = "";
                int txnId = 0;
                try (PreparedStatement ps = conn.prepareStatement(selTime);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tg = rs.getString("tg");
                        txnId = rs.getInt("id");
                    }
                }

                conn.commit();
                res.thanhCong = true;
                res.soDuMoi = soDuMoi;
                res.thoiGian = tg;
                res.maGiaoDich = "TXN-" + String.format("%06d", txnId);
                return res;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            res.maLoi = "LOI_KET_NOI";
            return res;
        }
    }

    public static GiaoDichResult napTien(int accId, long soTien) {
        GiaoDichResult res = new GiaoDichResult();
        if (soTien <= 0) { res.maLoi = "SO_TIEN_KHONG_HOP_LE"; return res; }
        if (soTien % BOI_SO != 0) { res.maLoi = "BOI_SO"; return res; }
        if (soTien > HAN_MUC_NAP_LAN) { res.maLoi = "HAN_MUC_LAN"; return res; }

        String sel = "SELECT so_du FROM accounts WHERE id = ? FOR UPDATE";
        String upd = "UPDATE accounts SET so_du = so_du + ? WHERE id = ?";
        String ins = "INSERT INTO transactions (account_id, loai, so_tien, so_du_sau) VALUES (?, 'NAP', ?, ?)";
        String selTime = "SELECT DATE_FORMAT(thoi_gian, '%d/%m/%Y %H:%i:%s') AS tg, id FROM transactions WHERE id = LAST_INSERT_ID()";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                long soDu = 0;
                try (PreparedStatement ps = conn.prepareStatement(sel)) {
                    ps.setInt(1, accId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            res.maLoi = "LOI_HE_THONG";
                            return res;
                        }
                        soDu = rs.getLong("so_du");
                    }
                }

                long soDuMoi = soDu + soTien;
                try (PreparedStatement ps = conn.prepareStatement(upd)) {
                    ps.setLong(1, soTien);
                    ps.setInt(2, accId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(ins)) {
                    ps.setInt(1, accId);
                    ps.setLong(2, soTien);
                    ps.setLong(3, soDuMoi);
                    ps.executeUpdate();
                }

                String tg = "";
                int txnId = 0;
                try (PreparedStatement ps = conn.prepareStatement(selTime);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tg = rs.getString("tg");
                        txnId = rs.getInt("id");
                    }
                }

                conn.commit();
                res.thanhCong = true;
                res.soDuMoi = soDuMoi;
                res.thoiGian = tg;
                res.maGiaoDich = "TXN-" + String.format("%06d", txnId);
                return res;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            res.maLoi = "LOI_KET_NOI";
            return res;
        }
    }

    public static String tenNguoiNhan(String soTk) {
        String sql = "SELECT ten_chu_tk FROM accounts WHERE so_tai_khoan = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, soTk.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("ten_chu_tk");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static GiaoDichResult chuyenKhoan(int accId, String soTkNhan, long soTien) {
        GiaoDichResult res = new GiaoDichResult();
        if (soTien <= 0) { res.maLoi = "SO_TIEN_KHONG_HOP_LE"; return res; }
        if (soTien > HAN_MUC_CK_LAN) { res.maLoi = "HAN_MUC_LAN"; return res; }

        String selNhan = "SELECT id FROM accounts WHERE so_tai_khoan = ?";
        String selLock = "SELECT id, so_du, so_tai_khoan FROM accounts WHERE id = ? FOR UPDATE";
        String updGui = "UPDATE accounts SET so_du = so_du - ? WHERE id = ?";
        String updNhan = "UPDATE accounts SET so_du = so_du + ? WHERE id = ?";
        String ins = "INSERT INTO transactions (account_id, loai, so_tien, so_du_sau, tk_doi_tac) VALUES (?, ?, ?, ?, ?)";
        String selTime = "SELECT DATE_FORMAT(thoi_gian, '%d/%m/%Y %H:%i:%s') AS tg, id FROM transactions WHERE id = LAST_INSERT_ID()";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int idNhan = 0;
                try (PreparedStatement ps = conn.prepareStatement(selNhan)) {
                    ps.setString(1, soTkNhan.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            res.maLoi = "TK_KHONG_TON_TAI";
                            return res;
                        }
                        idNhan = rs.getInt("id");
                    }
                }
                if (idNhan == accId) {
                    conn.rollback();
                    res.maLoi = "TRUNG_TAI_KHOAN";
                    return res;
                }

                long daChuyenHomNay = tongChuyenHomNay(conn, accId);
                if (daChuyenHomNay + soTien > HAN_MUC_CK_NGAY) {
                    conn.rollback();
                    res.maLoi = "HAN_MUC_NGAY";
                    return res;
                }

                int firstId = Math.min(accId, idNhan);
                int secondId = Math.max(accId, idNhan);
                long soDuGui = 0, soDuNhan = 0;
                String soTkGui = "";

                try (PreparedStatement ps = conn.prepareStatement(selLock)) {
                    ps.setInt(1, firstId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            if (firstId == accId) {
                                soDuGui = rs.getLong("so_du");
                                soTkGui = rs.getString("so_tai_khoan");
                            } else {
                                soDuNhan = rs.getLong("so_du");
                            }
                        }
                    }
                    ps.setInt(1, secondId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            if (secondId == accId) {
                                soDuGui = rs.getLong("so_du");
                                soTkGui = rs.getString("so_tai_khoan");
                            } else {
                                soDuNhan = rs.getLong("so_du");
                            }
                        }
                    }
                }

                if (soDuGui < soTien) {
                    conn.rollback();
                    res.maLoi = "KHONG_DU_SO_DU";
                    return res;
                }

                long soDuGuiMoi = soDuGui - soTien;
                long soDuNhanMoi = soDuNhan + soTien;

                try (PreparedStatement ps = conn.prepareStatement(updGui)) {
                    ps.setLong(1, soTien);
                    ps.setInt(2, accId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(updNhan)) {
                    ps.setLong(1, soTien);
                    ps.setInt(2, idNhan);
                    ps.executeUpdate();
                }
                int txnId = 0;
                try (PreparedStatement ps = conn.prepareStatement(ins)) {
                    ps.setInt(1, accId);
                    ps.setString(2, "CHUYEN");
                    ps.setLong(3, soTien);
                    ps.setLong(4, soDuGuiMoi);
                    ps.setString(5, soTkNhan.trim());
                    ps.executeUpdate();

                    ps.setInt(1, idNhan);
                    ps.setString(2, "NHAN");
                    ps.setLong(3, soTien);
                    ps.setLong(4, soDuNhanMoi);
                    ps.setString(5, soTkGui);
                    ps.executeUpdate();
                }

                String tg = "";
                try (PreparedStatement ps = conn.prepareStatement(selTime);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tg = rs.getString("tg");
                        txnId = rs.getInt("id");
                    }
                }

                conn.commit();
                res.thanhCong = true;
                res.soDuMoi = soDuGuiMoi;
                res.thoiGian = tg;
                res.maGiaoDich = "TXN-" + String.format("%06d", txnId);
                return res;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            res.maLoi = "LOI_KET_NOI";
            return res;
        }
    }

    public static GiaoDichResult doiPin(String soThe, String pinCu, String pinMoi) {
        GiaoDichResult res = new GiaoDichResult();
        String soTheThuan = soThe.replaceAll("\\s+", "");
        String sel = "SELECT pin, account_id FROM cards WHERE so_the = ? AND trang_thai = 'ACTIVE'";
        String upd = "UPDATE cards SET pin = ? WHERE so_the = ?";
        String selDu = "SELECT so_du FROM accounts WHERE id = ?";
        String ins = "INSERT INTO transactions (account_id, loai, so_tien, so_du_sau) VALUES (?, 'DOI_PIN', 0, ?)";
        String selTime = "SELECT DATE_FORMAT(thoi_gian, '%d/%m/%Y %H:%i:%s') AS tg, id FROM transactions WHERE id = LAST_INSERT_ID()";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int accId = 0;
                try (PreparedStatement ps = conn.prepareStatement(sel)) {
                    ps.setString(1, soTheThuan);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !rs.getString("pin").equalsIgnoreCase(hashPin(soTheThuan, pinCu))) {
                            conn.rollback();
                            res.maLoi = "PIN_CU_KHONG_DUNG";
                            return res;
                        }
                        accId = rs.getInt("account_id");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(upd)) {
                    ps.setString(1, hashPin(soTheThuan, pinMoi));
                    ps.setString(2, soTheThuan);
                    ps.executeUpdate();
                }

                long soDu = 0;
                try (PreparedStatement ps = conn.prepareStatement(selDu)) {
                    ps.setInt(1, accId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) soDu = rs.getLong("so_du");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(ins)) {
                    ps.setInt(1, accId);
                    ps.setLong(2, soDu);
                    ps.executeUpdate();
                }

                String tg = "";
                int txnId = 0;
                try (PreparedStatement ps = conn.prepareStatement(selTime);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tg = rs.getString("tg");
                        txnId = rs.getInt("id");
                    }
                }

                conn.commit();
                res.thanhCong = true;
                res.soDuMoi = soDu;
                res.thoiGian = tg;
                res.maGiaoDich = "TXN-" + String.format("%06d", txnId);
                return res;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            res.maLoi = "LOI_KET_NOI";
            return res;
        }
    }

    public static List<String[]> saoKeMini(int accId) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT thoi_gian, loai, so_tien, so_du_sau, tk_doi_tac FROM transactions WHERE account_id = ? ORDER BY thoi_gian DESC, id DESC LIMIT 10";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accId);
            try (ResultSet rs = ps.executeQuery()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                while (rs.next()) {
                    Timestamp tg = rs.getTimestamp("thoi_gian");
                    String tgStr = tg != null ? sdf.format(tg) : "";
                    String loaiRaw = rs.getString("loai");
                    long st = rs.getLong("so_tien");
                    long sds = rs.getLong("so_du_sau");
                    String dt = rs.getString("tk_doi_tac");

                    String loaiHienThi;
                    String soTienHienThi;
                    if ("RUT".equalsIgnoreCase(loaiRaw)) {
                        loaiHienThi = "Rút tiền";
                        soTienHienThi = "-" + dinhDangTien(st);
                    } else if ("NAP".equalsIgnoreCase(loaiRaw)) {
                        loaiHienThi = "Nạp tiền";
                        soTienHienThi = "+" + dinhDangTien(st);
                    } else if ("CHUYEN".equalsIgnoreCase(loaiRaw)) {
                        loaiHienThi = "Chuyển tiền";
                        soTienHienThi = "-" + dinhDangTien(st);
                    } else if ("NHAN".equalsIgnoreCase(loaiRaw)) {
                        loaiHienThi = "Nhận tiền";
                        soTienHienThi = "+" + dinhDangTien(st);
                    } else if ("DOI_PIN".equalsIgnoreCase(loaiRaw)) {
                        loaiHienThi = "Đổi mã PIN";
                        soTienHienThi = "-";
                    } else {
                        loaiHienThi = loaiRaw;
                        soTienHienThi = dinhDangTien(st);
                    }

                    list.add(new String[]{
                        tgStr,
                        loaiHienThi,
                        soTienHienThi,
                        dinhDangTien(sds),
                        dt == null || dt.isEmpty() ? "-" : dt
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
