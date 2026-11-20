package com.vietbank.atm.ui.component;

import com.vietbank.atm.config.DbConfig;
import com.vietbank.atm.model.CashVault;
import com.vietbank.atm.service.ATMSystemService;
import com.vietbank.atm.ui.theme.Colors;
import com.vietbank.atm.ui.theme.Fonts;
import com.vietbank.atm.ui.theme.Theme;
import com.vietbank.atm.util.I18n;
import com.vietbank.atm.util.MoneyFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;

public class ATMDialogs {
    private static JDialog createModal(JFrame parent, String title, Color borderCol) {
        JDialog dlg = new JDialog(parent, title, true);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.setResizable(false);
        JPanel root = new JPanel(new BorderLayout(14, 16));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderCol, 2), BorderFactory.createEmptyBorder(20, 24, 20, 24)));
        dlg.setContentPane(root);
        return dlg;
    }

    public static void showErrorModal(JFrame parent, String message) {
        JDialog dlg = createModal(parent, I18n.get("modal.error_title"), Colors.DANGER);
        JPanel root = (JPanel) dlg.getContentPane();

        JLabel icon = new JLabel("!", SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(new Color(254, 226, 226));
        icon.setForeground(Colors.DANGER);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 28));
        icon.setPreferredSize(new Dimension(52, 52));
        icon.setBorder(BorderFactory.createLineBorder(Colors.DANGER, 2, true));

        JLabel msg = new JLabel("<html><div style='width:300px; font-family:Segoe UI; font-size:13pt;'>" + message + "</div></html>");
        msg.setForeground(Colors.TEXT_MAIN);

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);
        top.add(icon, BorderLayout.WEST);
        top.add(msg, BorderLayout.CENTER);

        JButton btnOk = Theme.createDangerButton(I18n.get("modal.ok"));
        btnOk.addActionListener(e -> dlg.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(btnOk);

        root.add(top, BorderLayout.CENTER);
        root.add(btnPanel, BorderLayout.SOUTH);
        for (int key : new int[]{KeyEvent.VK_ESCAPE, KeyEvent.VK_ENTER}) {
            dlg.getRootPane().registerKeyboardAction(e -> dlg.dispose(), KeyStroke.getKeyStroke(key, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        dlg.pack();
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    public static boolean showTimeoutWarning(JFrame parent, int initialSeconds, IntConsumer onTick) {
        boolean[] cont = {false};
        int[] remain = {initialSeconds > 0 ? initialSeconds : 10};
        JDialog dlg = createModal(parent, I18n.get("timeout.title"), Colors.WARNING);
        JPanel root = (JPanel) dlg.getContentPane();

        JLabel lblMsg = new JLabel("<html><div style='width:340px; font-family:Segoe UI; font-size:13pt;'>" + I18n.get("timeout.message") + "</div></html>");
        lblMsg.setForeground(Colors.TEXT_MAIN);

        JButton btnContinue = Theme.createPrimaryButton(I18n.get("timeout.btn_continue", remain[0]));
        JButton btnEnd = Theme.createSecondaryButton(I18n.get("timeout.btn_end"));
        if (onTick != null) onTick.accept(remain[0]);

        Timer t = new Timer(1000, e -> {
            remain[0]--;
            if (onTick != null) onTick.accept(Math.max(0, remain[0]));
            if (remain[0] <= 0) dlg.dispose();
            else btnContinue.setText(I18n.get("timeout.btn_continue", remain[0]));
        });

        btnContinue.addActionListener(e -> { cont[0] = true; t.stop(); dlg.dispose(); });
        btnEnd.addActionListener(e -> { t.stop(); dlg.dispose(); });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        btns.add(btnEnd);
        btns.add(btnContinue);

        root.add(lblMsg, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(parent);
        t.start();
        dlg.setVisible(true);
        t.stop();
        return cont[0];
    }

    public static void showDevModeDialog(JFrame parent, ATMSystemService sysService, Runnable onRefresh) {
        JDialog dlg = new JDialog(parent, "Developer & Diagnostics Mode (Ctrl+Shift+D)", true);
        dlg.setSize(560, 480);
        dlg.setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(Colors.SCREEN_BG);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel infoPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(Theme.BORDER_CARD);

        boolean connected = DbConfig.testConnection();
        JLabel lblDb = new JLabel("CSDL MySQL: " + (connected ? "SAN SANG (atm_db)" : "CHUA KET NOI"));
        lblDb.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDb.setForeground(connected ? Colors.SUCCESS : Colors.DANGER);

        CashVault vault = connected ? sysService.getVaultStatus() : null;
        String vaultText = vault == null ? "Ton quy: Khong doc duoc" : String.format(
            "Ton quy: 500k(%d) | 200k(%d) | 100k(%d) | 50k(%d) = %s",
            vault.to500k, vault.to200k, vault.to100k, vault.to50k, MoneyFormat.formatMoney(vault.tongTien()));

        JLabel lblCards = new JLabel("<html>The mau kiem thu:<br>• 9704001234567890 | PIN: 123456 (ACTIVE - Nguyen Van An)<br>• 9704009876543210 | PIN: 654321 (ACTIVE - Tran Thi Bich)<br>• 9704001111222233 | PIN: 111111 (LOCKED - Le Hoang Minh)</html>");
        lblCards.setFont(Fonts.MONO);

        infoPanel.add(lblDb);
        infoPanel.add(new JLabel("Chuan bao mat: PCI-DSS v4.0 (PBKDF2 PIN + Audit Log) | ATM ID: " + ATMSystemService.ATM_ID));
        infoPanel.add(new JLabel(vaultText));
        infoPanel.add(lblCards);

        JPanel dbConfig = new JPanel(new GridLayout(2, 2, 8, 8));
        dbConfig.setBackground(Color.WHITE);
        dbConfig.setBorder(BorderFactory.createTitledBorder("Cau hinh MySQL"));
        JTextField txtUser = new JTextField(DbConfig.getUser());
        JPasswordField txtPass = new JPasswordField(DbConfig.getPass());
        dbConfig.add(new JLabel("MySQL User:")); dbConfig.add(txtUser);
        dbConfig.add(new JLabel("MySQL Password:")); dbConfig.add(txtPass);

        JButton btnSave = Theme.createPrimaryButton("Luu & Ket noi");
        btnSave.addActionListener(e -> {
            DbConfig.saveConfig(txtUser.getText().trim(), new String(txtPass.getPassword()));
            boolean ok = DbConfig.testConnection();
            lblDb.setText("CSDL MySQL: " + (ok ? "SAN SANG (atm_db)" : "KET NOI THAT BAI"));
            lblDb.setForeground(ok ? Colors.SUCCESS : Colors.DANGER);
            if (onRefresh != null) onRefresh.run();
        });

        JButton btnInit = Theme.createSecondaryButton("Nap schema + seed");
        btnInit.addActionListener(e -> {
            JOptionPane.showMessageDialog(dlg, "Ket qua: " + DbConfig.initDatabase(txtUser.getText().trim(), new String(txtPass.getPassword())));
            if (onRefresh != null) onRefresh.run();
        });

        JButton btnMaint = Theme.createSecondaryButton("Bat/Tat Bao tri");
        btnMaint.addActionListener(e -> {
            String next = "ONLINE".equals(sysService.getAtmStatus()) ? "MAINTENANCE" : "ONLINE";
            sysService.setAtmStatus(next);
            JOptionPane.showMessageDialog(dlg, "Trang thai ATM: " + next);
            if (onRefresh != null) onRefresh.run();
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        actions.setOpaque(false);
        actions.add(btnSave); actions.add(btnInit); actions.add(btnMaint);

        root.add(infoPanel, BorderLayout.NORTH);
        root.add(dbConfig, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }
}
