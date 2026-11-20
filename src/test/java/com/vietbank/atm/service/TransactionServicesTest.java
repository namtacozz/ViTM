package com.vietbank.atm.service;

import com.vietbank.atm.config.DbConfig;
import com.vietbank.atm.model.BankInfo;
import com.vietbank.atm.model.CashBreakdown;
import com.vietbank.atm.model.LoginResult;
import com.vietbank.atm.model.TransactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServicesTest {
    private final AuthService authService = new AuthService();
    private final WithdrawService withdrawService = new WithdrawService();
    private final DepositService depositService = new DepositService();
    private final TransferService transferService = new TransferService();
    private final ATMSystemService sysService = new ATMSystemService();

    @BeforeEach
    void setupInMemoryDb() throws Exception {
        DbConfig.setTestConnection("jdbc:h2:mem:atm_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection conn = DbConfig.getConnection(); Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS audit_logs");
            st.execute("DROP TABLE IF EXISTS transactions");
            st.execute("DROP TABLE IF EXISTS cards");
            st.execute("DROP TABLE IF EXISTS accounts");
            st.execute("DROP TABLE IF EXISTS external_accounts");
            st.execute("DROP TABLE IF EXISTS atm_vault");
            st.execute("DROP TABLE IF EXISTS atm_config");

            st.execute("CREATE TABLE accounts (id INT AUTO_INCREMENT PRIMARY KEY, so_tai_khoan VARCHAR(20) UNIQUE NOT NULL, ten_chu_tk VARCHAR(100) NOT NULL, so_du BIGINT NOT NULL DEFAULT 0)");
            st.execute("CREATE TABLE external_accounts (id INT AUTO_INCREMENT PRIMARY KEY, ma_ngan_hang VARCHAR(20) NOT NULL, ten_ngan_hang VARCHAR(100) NOT NULL, so_tai_khoan VARCHAR(20) NOT NULL, ten_chu_tk VARCHAR(100) NOT NULL)");
            st.execute("CREATE TABLE cards (so_the VARCHAR(16) PRIMARY KEY, pin VARCHAR(64) NOT NULL, account_id INT NOT NULL, so_lan_sai INT DEFAULT 0, trang_thai VARCHAR(15) DEFAULT 'ACTIVE')");
            st.execute("CREATE TABLE transactions (id INT AUTO_INCREMENT PRIMARY KEY, ma_tham_chieu VARCHAR(32) UNIQUE NOT NULL, account_id INT NOT NULL, loai VARCHAR(20) NOT NULL, so_tien BIGINT DEFAULT 0, phi BIGINT DEFAULT 0, so_du_sau BIGINT, tk_doi_tac VARCHAR(30), ngan_hang_nhan VARCHAR(50), noi_dung VARCHAR(255), dia_diem_atm VARCHAR(150), thoi_gian DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE atm_vault (menh_gia INT PRIMARY KEY, so_to INT NOT NULL DEFAULT 0)");
            st.execute("CREATE TABLE atm_config (config_key VARCHAR(50) PRIMARY KEY, config_value VARCHAR(200) NOT NULL)");
            st.execute("CREATE TABLE audit_logs (id INT AUTO_INCREMENT PRIMARY KEY, thoi_gian DATETIME DEFAULT CURRENT_TIMESTAMP, atm_id VARCHAR(30) NOT NULL, so_the VARCHAR(20), account_id INT, hanh_dong VARCHAR(50) NOT NULL, chi_tiet VARCHAR(255), ket_qua VARCHAR(30) NOT NULL)");

            st.execute("INSERT INTO atm_config VALUES ('atm_status', 'ONLINE')");
            st.execute("INSERT INTO atm_vault VALUES (500000, 100), (200000, 100), (100000, 100), (50000, 100)");
            st.execute("INSERT INTO accounts (id, so_tai_khoan, ten_chu_tk, so_du) VALUES (1, '1234567890', 'NGUYEN VAN AN', 3000000), (2, '0987654321', 'TRAN THI BICH', 1000000)");
            st.execute("INSERT INTO external_accounts (ma_ngan_hang, ten_ngan_hang, so_tai_khoan, ten_chu_tk) VALUES ('VCB', 'Vietcombank', '0011001234567', 'PHAM MINH TUAN')");
            st.execute("INSERT INTO cards VALUES ('9704001234567890', '67590a0da7df033b72ae36cbf36b2965ea1c497b4635c36c2bdb2a1867c7b457', 1, 0, 'ACTIVE')");
        }
    }

    @Test
    void authServiceShouldLockCardAfterThreeWrongAttempts() {
        LoginResult r1 = authService.login("9704001234567890", "000000");
        assertFalse(r1.success);
        assertFalse(r1.locked);
        assertEquals(2, r1.remainingAttempts);

        LoginResult r2 = authService.login("9704001234567890", "000000");
        assertFalse(r2.success);
        assertEquals(1, r2.remainingAttempts);

        LoginResult r3 = authService.login("9704001234567890", "000000");
        assertFalse(r3.success);
        assertTrue(r3.locked);
        assertEquals("LOCKED", authService.checkCard("9704001234567890"));
    }

    @Test
    void withdrawAndDepositShouldUpdateBalanceAndVaultAtomically() {
        TransactionResult w = withdrawService.withdraw(1, "9704001234567890", 1_500_000L);
        assertTrue(w.thanhCong);
        assertEquals(1_500_000L, sysService.getBalance(1));

        TransactionResult d = depositService.deposit(1, "9704001234567890", new CashBreakdown(2, 0, 0, 0));
        assertTrue(d.thanhCong);
        assertEquals(2_500_000L, sysService.getBalance(1));
    }

    @Test
    void transferShouldRollbackWhenBeneficiaryNotFound() {
        TransactionResult res = transferService.transfer(1, "9704001234567890", new BankInfo("VB", "VietBank", 0L), "9999999999", 500_000L, "TEST");
        assertFalse(res.thanhCong);
        assertEquals("err.transfer_acc_not_found", res.maLoi);
        assertEquals(3_000_000L, sysService.getBalance(1));
    }

    @Test
    void concurrentWithdrawalsShouldPreventNegativeBalance() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<TransactionResult> f1 = pool.submit(() -> {
            ready.countDown();
            start.await();
            return withdrawService.withdraw(1, "9704001234567890", 2_000_000L);
        });
        Future<TransactionResult> f2 = pool.submit(() -> {
            ready.countDown();
            start.await();
            return withdrawService.withdraw(1, "9704001234567890", 2_000_000L);
        });

        ready.await();
        start.countDown();

        TransactionResult r1 = f1.get();
        TransactionResult r2 = f2.get();
        pool.shutdown();

        int successCount = (r1.thanhCong ? 1 : 0) + (r2.thanhCong ? 1 : 0);
        assertEquals(1, successCount);
        assertEquals(1_000_000L, sysService.getBalance(1));
    }

    @Test
    void concurrentCrossTransferAToBAndBToAShouldNotDeadlock() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        BankInfo vb = new BankInfo("VB", "VietBank", 0L);

        Future<TransactionResult> f1 = pool.submit(() -> {
            ready.countDown();
            start.await();
            return transferService.transfer(1, "9704001234567890", vb, "0987654321", 500_000L, "A_TO_B");
        });
        Future<TransactionResult> f2 = pool.submit(() -> {
            ready.countDown();
            start.await();
            return transferService.transfer(2, "9704009876543210", vb, "1234567890", 300_000L, "B_TO_A");
        });

        ready.await();
        start.countDown();

        TransactionResult r1 = f1.get();
        TransactionResult r2 = f2.get();
        pool.shutdown();

        assertTrue(r1.thanhCong);
        assertTrue(r2.thanhCong);
        assertEquals(2_800_000L, sysService.getBalance(1));
        assertEquals(1_200_000L, sysService.getBalance(2));
    }
}
