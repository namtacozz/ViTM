package com.vietbank.atm.ui;

import com.vietbank.atm.config.DbConfig;
import com.vietbank.atm.model.*;
import com.vietbank.atm.service.*;
import com.vietbank.atm.state.ATMState;
import com.vietbank.atm.state.SessionManager;
import com.vietbank.atm.ui.component.ATMDialogs;
import com.vietbank.atm.ui.component.HardwareSlotPanel;
import com.vietbank.atm.ui.theme.Colors;
import com.vietbank.atm.ui.theme.Fonts;
import com.vietbank.atm.ui.theme.Theme;
import com.vietbank.atm.util.Hasher;
import com.vietbank.atm.util.I18n;
import com.vietbank.atm.util.MoneyFormat;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class MainFrame extends JFrame {
    private final ATMSystemService sysService = new ATMSystemService();
    private final AuthService authService = new AuthService();
    private final WithdrawService withdrawService = new WithdrawService();
    private final DepositService depositService = new DepositService();
    private final TransferService transferService = new TransferService();
    private final PinService pinService = new PinService();
    private final CashDispenser dispenser = new CashDispenser();
    private final SessionManager session = new SessionManager();

    private ATMState currentState = ATMState.WAIT;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screenRoot = new JPanel(cardLayout);
    private final HardwareSlotPanel hardwareSlots = new HardwareSlotPanel();
    private final List<Runnable> i18nUpdaters = new ArrayList<>();

    private final Timer sessionTimer;
    private Timer processingTimer, cashRetractTimer;

    private final JButton[] leftFdk = new JButton[4], rightFdk = new JButton[4];
    private final Runnable[] leftActions = new Runnable[4], rightActions = new Runnable[4];
    private Runnable enterAction, escAction;

    private JLabel lblHeaderTitle, lblHeaderClock, lblFooterCountdown;
    private JButton btnLangVi, btnLangEn;
    private JLabel lblWaitClock, lblWaitDate;

    private JTextField txtCardNumber;
    private JPasswordField txtPin;
    private JLabel lblMenuWelcome, lblMenuAccNo, lblMenuCardNo;
    private JLabel lblBalHolder, lblBalAccNo, lblBalCardNo, lblBalAvailable;
    private JTextField txtWithdrawCustom;
    private JLabel lblWConfirmAmount, lblWConfirmTotal;
    private long pendingWithdrawAmount;

    private final int[] depositCounts = new int[4];
    private final JLabel[] lblDepositCounts = new JLabel[4];
    private CashBreakdown pendingDepositNotes = new CashBreakdown();
    private JLabel lblDepCountDetail, lblDepCountTotal;

    private BankInfo selectedBank;
    private final List<BankInfo> supportedBanks;
    private JTextField txtTransAcc, txtTransAmount, txtTransContent;
    private JLabel lblTransReceiverVal, lblSelectedBankVal;
    private String pendingTransName = "", pendingTransAcc = "", pendingTransContent = "";
    private long pendingTransAmount;
    private JLabel lblTransConfBank, lblTransConfSender, lblTransConfReceiver, lblTransConfName;
    private JLabel lblTransConfAmount, lblTransConfFee, lblTransConfTotal, lblTransConfContent;

    private JPasswordField txtPinOld, txtPinNew, txtPinConf;
    private JLabel lblStmtSummary;
    private JTable tblStatement;
    private DefaultTableModel modelStatement;

    private JLabel lblProcNotice, lblAskRef, lblTakeCashAmount, lblTakeCashCountdown;
    private int cashRetractSeconds = 15;
    private JTextArea txtReceipt;
    private JLabel lblSysErrTitle, lblSysErrSub;
    private JButton btnSysErrRetry, btnSysErrMenu, btnSysErrEject;
    private String sysErrMode = "MAINTENANCE";

    private TransactionResult lastTxnResult;
    private String lastTxnType = "", lastTxnPartner = "-", lastTxnBank = "VietBank", lastTxnContent = "";
    private long lastTxnAmount;
    private boolean wantsReceipt;
    private JTextField activeInputField;

    public MainFrame() {
        supportedBanks = transferService.getSupportedBanks();
        selectedBank = supportedBanks.get(0);

        i18nUpdaters.add(() -> setTitle(I18n.get("app.title")));
        setTitle(I18n.get("app.title"));
        setSize(1024, 768);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel bezel = new JPanel(new BorderLayout());
        bezel.setBackground(Colors.BEZEL_BG);
        bezel.setBorder(BorderFactory.createEmptyBorder(14, 14, 6, 14));
        bezel.add(buildFdkColumn(true), BorderLayout.WEST);
        bezel.add(buildFdkColumn(false), BorderLayout.EAST);
        bezel.add(buildLcdContainer(), BorderLayout.CENTER);
        bezel.add(hardwareSlots, BorderLayout.SOUTH);

        setContentPane(bezel);
        registerGlobalKeyBindings();
        new Timer(1000, e -> updateClocks()).start();
        sessionTimer = new Timer(1000, e -> handleSessionTick());
        sessionTimer.start();

        updateClocks();
        updateLanguageToggleVisuals();
        navigateTo(ATMState.WAIT);
    }

    private JLabel i18nLbl(String key, int align, Font f, Color fg) {
        JLabel l = new JLabel(I18n.get(key), align);
        if (f != null) l.setFont(f);
        if (fg != null) l.setForeground(fg);
        i18nUpdaters.add(() -> l.setText(I18n.get(key)));
        return l;
    }

    private JLabel i18nHtml(String key, int width, Font f, Color fg) {
        JLabel l = new JLabel("", SwingConstants.CENTER);
        if (f != null) l.setFont(f);
        if (fg != null) l.setForeground(fg);
        Runnable r = () -> l.setText("<html><div style='text-align:center;width:" + width + "px;'>" + I18n.get(key) + "</div></html>");
        r.run();
        i18nUpdaters.add(r);
        return l;
    }

    private JButton i18nBtn(JButton b, String key) {
        b.setText(I18n.get(key));
        i18nUpdaters.add(() -> b.setText(I18n.get(key)));
        return b;
    }

    private JLabel valLbl(Color fg, Font f) {
        JLabel l = new JLabel("", SwingConstants.RIGHT);
        l.setFont(f != null ? f : new Font("Segoe UI", Font.BOLD, 15));
        l.setForeground(fg != null ? fg : Colors.TEXT_MAIN);
        return l;
    }

    private JPanel btnRow(JComponent... btns) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        p.setOpaque(false);
        for (JComponent b : btns) p.add(b);
        return p;
    }

    private JPanel topHeader(String titleKey, String subKey) {
        JPanel top = new JPanel(new GridLayout(2, 1, 0, 4));
        top.setOpaque(false);
        top.setPreferredSize(new Dimension(0, 60));
        top.add(i18nLbl(titleKey, SwingConstants.CENTER, Fonts.TITLE, Colors.TEXT_MAIN));
        top.add(i18nLbl(subKey, SwingConstants.CENTER, Fonts.SUB, Colors.TEXT_MUTED));
        return top;
    }

    private JPanel centeredCard(Color borderCol, int w, int h, JComponent... rows) {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Colors.SCREEN_BG);
        JPanel card = new JPanel(new GridLayout(rows.length, 1, 0, 14));
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(w, h));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderCol, 2, true), BorderFactory.createEmptyBorder(24, 34, 24, 34)));
        for (JComponent r : rows) card.add(r);
        outer.add(card);
        return outer;
    }

    private JPanel kvGrid(int hgap, int vgap, String[] keys, JLabel[] vals) {
        JPanel card = new JPanel(new GridLayout(keys.length, 2, hgap, vgap));
        card.setBackground(Color.WHITE);
        card.setBorder(Theme.BORDER_CARD);
        for (int i = 0; i < keys.length; i++) {
            card.add(i18nLbl(keys[i], SwingConstants.LEFT, Fonts.SUB, Colors.TEXT_MUTED));
            card.add(vals[i]);
        }
        return card;
    }

    private JPanel buildFdkColumn(boolean isLeft) {
        JPanel col = new JPanel(new BorderLayout());
        col.setBackground(Colors.BEZEL_BG);
        col.setPreferredSize(new Dimension(76, 0));
        col.setBorder(BorderFactory.createEmptyBorder(142, isLeft ? 4 : 10, 62, isLeft ? 10 : 4));
        JPanel grid = new JPanel(new GridLayout(4, 1, 0, 18));
        grid.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            JButton btn = new JButton(isLeft ? ">" : "<");
            btn.setFont(new Font("Consolas", Font.BOLD, 18));
            btn.setOpaque(true);
            btn.setFocusPainted(false);
            btn.setFocusable(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                resetSessionTimer();
                Runnable act = isLeft ? leftActions[idx] : rightActions[idx];
                if (act != null) act.run();
            });
            (isLeft ? leftFdk : rightFdk)[i] = btn;
            grid.add(btn);
        }
        col.add(grid, BorderLayout.CENTER);
        return col;
    }

    private JPanel buildLcdContainer() {
        JPanel lcd = new JPanel(new BorderLayout());
        lcd.setBackground(Colors.SCREEN_BG);
        lcd.setBorder(BorderFactory.createLineBorder(new Color(17, 19, 23), 5, true));
        lcd.add(buildHeader(), BorderLayout.NORTH);
        buildAllScreens();
        lcd.add(screenRoot, BorderLayout.CENTER);
        lcd.add(buildFooter(), BorderLayout.SOUTH);
        return lcd;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(Colors.CARD_BG);
        header.setPreferredSize(new Dimension(0, 56));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, Colors.PRIMARY), BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        left.setOpaque(false);
        JLabel badge = new JLabel("ATM");
        badge.setOpaque(true);
        badge.setBackground(Colors.PRIMARY);
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        left.add(badge);
        left.add(i18nLbl("app.bank_name", SwingConstants.LEFT, new Font("Segoe UI", Font.BOLD, 16), Colors.TEXT_MAIN));

        lblHeaderTitle = new JLabel("", SwingConstants.CENTER);
        lblHeaderTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblHeaderTitle.setForeground(Colors.PRIMARY);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        right.setOpaque(false);
        lblHeaderClock = new JLabel("00:00:00");
        lblHeaderClock.setFont(new Font("Consolas", Font.BOLD, 14));
        lblHeaderClock.setForeground(Colors.TEXT_MUTED);
        lblHeaderClock.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        btnLangVi = langBtn("VI", false);
        btnLangEn = langBtn("EN", true);
        right.add(lblHeaderClock);
        right.add(btnLangVi);
        right.add(btnLangEn);

        header.add(left, BorderLayout.WEST);
        header.add(lblHeaderTitle, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JButton langBtn(String text, boolean en) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(46, 30));
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> switchLanguage(en));
        return b;
    }

    private void updateLanguageToggleVisuals() {
        boolean en = I18n.isEnglish();
        JButton[] arr = {btnLangVi, btnLangEn};
        for (int i = 0; i < 2; i++) {
            boolean sel = (i == 1) == en;
            arr[i].setBackground(sel ? Colors.PRIMARY : Color.WHITE);
            arr[i].setForeground(sel ? Color.WHITE : Colors.TEXT_MAIN);
            arr[i].setBorder(BorderFactory.createLineBorder(sel ? Colors.PRIMARY : Colors.BORDER_DEFAULT, 1, true));
        }
    }

    private void switchLanguage(boolean english) {
        I18n.setEnglish(english);
        updateLanguageToggleVisuals();
        i18nUpdaters.forEach(Runnable::run);
        navigateTo(currentState);
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Colors.CARD_BG);
        footer.setPreferredSize(new Dimension(0, 28));
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.BORDER_DEFAULT), BorderFactory.createEmptyBorder(4, 16, 4, 16)));
        JLabel brand = new JLabel("VIETBANK 24/7");
        brand.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        brand.setForeground(Colors.TEXT_MUTED);
        lblFooterCountdown = new JLabel("", SwingConstants.RIGHT);
        lblFooterCountdown.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFooterCountdown.setForeground(Colors.DANGER);
        footer.add(brand, BorderLayout.WEST);
        footer.add(lblFooterCountdown, BorderLayout.EAST);
        return footer;
    }

    private void buildAllScreens() {
        screenRoot.add(buildWaitScreen(), ATMState.WAIT.name());
        screenRoot.add(buildCardInsertScreen(), ATMState.CARD_INSERT.name());
        screenRoot.add(buildLanguageSelectScreen(), ATMState.LANGUAGE_SELECT.name());
        screenRoot.add(buildPinEntryScreen(), ATMState.PIN_ENTRY.name());
        screenRoot.add(buildCardLockedScreen(), ATMState.CARD_LOCKED.name());
        screenRoot.add(buildMainMenuScreen(), ATMState.MAIN_MENU.name());
        screenRoot.add(buildBalanceScreen(), ATMState.BALANCE_INQUIRY.name());
        screenRoot.add(buildWithdrawMenuScreen(), ATMState.WITHDRAW_MENU.name());
        screenRoot.add(buildWithdrawConfirmScreen(), ATMState.WITHDRAW_CONFIRM.name());
        screenRoot.add(buildDepositTrayScreen(), ATMState.DEPOSIT_TRAY.name());
        screenRoot.add(buildDepositCountingScreen(), ATMState.DEPOSIT_COUNTING.name());
        screenRoot.add(buildTransferBankScreen(), ATMState.TRANSFER_BANK.name());
        screenRoot.add(buildTransferInputScreen(), ATMState.TRANSFER_INPUT.name());
        screenRoot.add(buildTransferConfirmScreen(), ATMState.TRANSFER_CONFIRM.name());
        screenRoot.add(buildPinChangeOldScreen(), ATMState.PIN_CHANGE_OLD.name());
        screenRoot.add(buildPinChangeNewScreen(), ATMState.PIN_CHANGE_NEW.name());
        screenRoot.add(buildPinChangeConfirmScreen(), ATMState.PIN_CHANGE_CONFIRM.name());
        screenRoot.add(buildPinChangeSuccessScreen(), ATMState.PIN_CHANGE_SUCCESS.name());
        screenRoot.add(buildMiniStatementScreen(), ATMState.MINI_STATEMENT.name());
        screenRoot.add(buildProcessingScreen(), ATMState.PROCESSING.name());
        screenRoot.add(buildAskReceiptScreen(), ATMState.ASK_RECEIPT.name());
        screenRoot.add(buildTakeCardScreen(), ATMState.TAKE_CARD.name());
        screenRoot.add(buildTakeCashScreen(), ATMState.TAKE_CASH.name());
        screenRoot.add(buildReceiptViewScreen(), ATMState.RECEIPT_VIEW.name());
        screenRoot.add(buildAnotherTxnScreen(), ATMState.ANOTHER_TXN.name());
        screenRoot.add(buildSystemErrorScreen(), ATMState.SYSTEM_ERROR.name());
    }

    private JPanel buildWaitScreen() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Colors.DARK_SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(36, 40, 24, 40));

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 8));
        top.setOpaque(false);
        top.add(i18nLbl("wait.welcome", SwingConstants.CENTER, new Font("Segoe UI", Font.BOLD, 24), Colors.PRIMARY));
        top.add(i18nLbl("wait.slogan", SwingConstants.CENTER, Fonts.SUB, new Color(228, 228, 231)));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.insets = new Insets(6, 0, 6, 0);

        lblWaitClock = new JLabel("00:00:00", SwingConstants.CENTER);
        lblWaitClock.setFont(new Font("Consolas", Font.BOLD, 54));
        lblWaitClock.setForeground(Color.WHITE);
        g.gridy = 0; center.add(lblWaitClock, g);

        lblWaitDate = new JLabel("", SwingConstants.CENTER);
        lblWaitDate.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblWaitDate.setForeground(new Color(161, 161, 170));
        g.gridy = 1; center.add(lblWaitDate, g);

        JButton btnInsert = i18nBtn(Theme.createPrimaryButton(""), "wait.btn_insert");
        btnInsert.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnInsert.setPreferredSize(new Dimension(280, 50));
        btnInsert.addActionListener(e -> onStartCardInsert());
        g.gridy = 2; g.insets = new Insets(24, 0, 8, 0); center.add(btnInsert, g);

        JPanel bottom = new JPanel(new GridLayout(2, 1, 0, 4));
        bottom.setOpaque(false);
        bottom.add(i18nLbl("wait.instruction", SwingConstants.CENTER, new Font("Segoe UI", Font.PLAIN, 13), new Color(212, 212, 216)));
        JLabel arrow = new JLabel("▼  ▼  ▼", SwingConstants.CENTER);
        arrow.setFont(new Font("Segoe UI", Font.BOLD, 18));
        arrow.setForeground(Colors.PRIMARY);
        bottom.add(arrow);

        p.add(top, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildCardInsertScreen() {
        JPanel p = new JPanel(new BorderLayout(16, 16));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(28, 44, 28, 44));

        JPanel box = new JPanel(new GridBagLayout());
        box.setBackground(Color.WHITE);
        box.setBorder(Theme.BORDER_CARD);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 8, 8, 8);

        g.gridx = 0; g.gridy = 0; g.gridwidth = 3;
        box.add(i18nLbl("card.quick_label", SwingConstants.LEFT, new Font("Segoe UI", Font.BOLD, 14), Colors.TEXT_MAIN), g);

        String[][] demoCards = {
            {"Thẻ 1: Nguyễn Văn An (9704 •••• 7890)", "9704001234567890"},
            {"Thẻ 2: Trần Thị Bích (9704 •••• 3210)", "9704009876543210"},
            {"Thẻ 3: Lê Hoàng Minh (LOCKED)", "9704001111222233"}
        };
        g.gridy = 1; g.gridwidth = 1;
        for (int i = 0; i < 3; i++) {
            String full = demoCards[i][1];
            JButton b = Theme.createSecondaryButton("<html><div style='text-align:center;'>" + demoCards[i][0] + "</div></html>");
            b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            b.setPreferredSize(new Dimension(210, 56));
            b.addActionListener(e -> { txtCardNumber.setText(MoneyFormat.formatCardInput(full)); submitCardNumber(); });
            g.gridx = i; box.add(b, g);
        }

        g.gridx = 0; g.gridy = 2; g.gridwidth = 3; g.insets = new Insets(18, 8, 4, 8);
        box.add(i18nLbl("card.input_label", SwingConstants.LEFT, new Font("Segoe UI", Font.BOLD, 14), Colors.TEXT_MAIN), g);

        txtCardNumber = new JTextField();
        txtCardNumber.setFont(new Font("Consolas", Font.BOLD, 22));
        txtCardNumber.setHorizontalAlignment(SwingConstants.CENTER);
        txtCardNumber.setPreferredSize(new Dimension(380, 46));
        Theme.applyFocusStyle(txtCardNumber, true);
        g.gridy = 3; g.insets = new Insets(4, 8, 8, 8);
        box.add(txtCardNumber, g);

        JButton back = i18nBtn(Theme.createSecondaryButton(""), "card.btn_back");
        JButton cont = i18nBtn(Theme.createPrimaryButton(""), "card.btn_continue");
        back.addActionListener(e -> navigateTo(ATMState.WAIT));
        cont.addActionListener(e -> submitCardNumber());

        p.add(topHeader("card.title", "card.subtitle"), BorderLayout.NORTH);
        p.add(box, BorderLayout.CENTER);
        p.add(btnRow(back, cont), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildLanguageSelectScreen() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JPanel grid = new JPanel(new GridLayout(4, 2, 24, 18));
        grid.setOpaque(false);

        JButton vi = i18nBtn(Theme.createAtmMenuButton("", false), "lang.vi");
        JButton en = i18nBtn(Theme.createAtmMenuButton("", false), "lang.en");
        JButton cancel = i18nBtn(Theme.createAtmMenuButton("", true), "lang.cancel");
        vi.addActionListener(e -> { switchLanguage(false); navigateTo(ATMState.PIN_ENTRY); });
        en.addActionListener(e -> { switchLanguage(true); navigateTo(ATMState.PIN_ENTRY); });
        cancel.addActionListener(e -> ejectCardAndFinish());

        grid.add(new JLabel()); grid.add(vi);
        grid.add(new JLabel()); grid.add(en);
        grid.add(new JLabel()); grid.add(new JLabel());
        grid.add(cancel);       grid.add(new JLabel());

        p.add(topHeader("lang.title", "lang.subtitle"), BorderLayout.NORTH);
        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildPinEntryScreen() {
        JPanel p = new JPanel(new BorderLayout(20, 16));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(26, 40, 26, 40));

        JPanel leftBox = new JPanel(new GridBagLayout());
        leftBox.setBackground(Color.WHITE);
        leftBox.setBorder(Theme.BORDER_CARD);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(8, 8, 8, 8);

        g.gridy = 0; leftBox.add(i18nLbl("pin.title", SwingConstants.CENTER, Fonts.TITLE, Colors.PRIMARY), g);
        g.gridy = 1; leftBox.add(i18nHtml("pin.subtitle", 300, Fonts.SUB, Colors.TEXT_MAIN), g);

        txtPin = new JPasswordField(6);
        txtPin.setFont(new Font("Consolas", Font.BOLD, 28));
        txtPin.setHorizontalAlignment(SwingConstants.CENTER);
        txtPin.setPreferredSize(new Dimension(260, 52));
        Theme.applyFocusStyle(txtPin, true);
        g.gridy = 2; g.insets = new Insets(14, 20, 14, 20); leftBox.add(txtPin, g);

        g.gridy = 3; g.insets = new Insets(8, 8, 8, 8);
        leftBox.add(i18nHtml("pin.instruction", 300, new Font("Segoe UI", Font.PLAIN, 13), Colors.TEXT_MUTED), g);

        JButton cancel = i18nBtn(Theme.createDangerButton(""), "pin.btn_cancel");
        JButton clear = i18nBtn(Theme.createSecondaryButton(""), "pin.btn_clear");
        JButton ok = i18nBtn(Theme.createPrimaryButton(""), "pin.btn_confirm");
        cancel.addActionListener(e -> ejectCardAndFinish());
        clear.addActionListener(e -> txtPin.setText(""));
        ok.addActionListener(e -> submitPin());
        g.gridy = 4; g.insets = new Insets(14, 8, 4, 8); leftBox.add(btnRow(cancel, clear, ok), g);

        p.add(leftBox, BorderLayout.CENTER);
        p.add(buildOnScreenNumpad(() -> txtPin, this::submitPin), BorderLayout.EAST);
        return p;
    }

    private JPanel buildCardLockedScreen() {
        JButton fin = i18nBtn(Theme.createDangerButton(""), "locked.btn_finish");
        fin.addActionListener(e -> resetToWait());
        return centeredCard(Colors.DANGER, 560, 290,
            i18nLbl("locked.title", SwingConstants.CENTER, Fonts.TITLE, Colors.DANGER),
            i18nHtml("locked.desc", 460, Fonts.SUB, Colors.TEXT_MAIN),
            i18nHtml("locked.guide", 460, new Font("Segoe UI", Font.BOLD, 14), Colors.TEXT_MUTED),
            btnRow(fin));
    }

    private JPanel buildMainMenuScreen() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel infoBox = new JPanel(new GridLayout(2, 2, 12, 2));
        infoBox.setBackground(Colors.ACCENT_BG);
        infoBox.setPreferredSize(new Dimension(0, 64));
        infoBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Colors.ACCENT_BORDER, 1, true), BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        lblMenuWelcome = new JLabel();
        lblMenuWelcome.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblMenuWelcome.setForeground(Colors.PRIMARY);
        lblMenuAccNo = valLbl(Colors.TEXT_MAIN, new Font("Consolas", Font.BOLD, 14));
        lblMenuCardNo = valLbl(Colors.TEXT_MUTED, new Font("Consolas", Font.PLAIN, 13));

        infoBox.add(lblMenuWelcome);
        infoBox.add(lblMenuAccNo);
        infoBox.add(i18nLbl("menu.hint", SwingConstants.LEFT, new Font("Segoe UI", Font.PLAIN, 13), Colors.TEXT_MUTED));
        infoBox.add(lblMenuCardNo);

        JPanel grid = new JPanel(new GridLayout(4, 2, 18, 18));
        grid.setOpaque(false);

        JButton bW = i18nBtn(Theme.createAtmMenuButton("", true), "menu.withdraw");
        JButton bD = i18nBtn(Theme.createAtmMenuButton("", true), "menu.deposit");
        JButton bT = i18nBtn(Theme.createAtmMenuButton("", true), "menu.transfer");
        JButton bB = i18nBtn(Theme.createAtmMenuButton("", true), "menu.balance");
        JButton bP = i18nBtn(Theme.createAtmMenuButton("", false), "menu.pin");
        JButton bS = i18nBtn(Theme.createAtmMenuButton("", false), "menu.statement");
        JButton bQ = i18nBtn(Theme.createAtmMenuButton("", false), "menu.quick_withdraw");
        JButton bE = i18nBtn(Theme.createAtmMenuButton("", false), "menu.exit");
        bE.setForeground(Colors.DANGER);

        bW.addActionListener(e -> navigateTo(ATMState.WITHDRAW_MENU));
        bD.addActionListener(e -> navigateTo(ATMState.DEPOSIT_TRAY));
        bT.addActionListener(e -> navigateTo(ATMState.TRANSFER_BANK));
        bB.addActionListener(e -> openBalanceInquiry());
        bP.addActionListener(e -> navigateTo(ATMState.PIN_CHANGE_OLD));
        bS.addActionListener(e -> { loadStatement(); navigateTo(ATMState.MINI_STATEMENT); });
        bQ.addActionListener(e -> prepareWithdrawConfirm(1_000_000L));
        bE.addActionListener(e -> ejectCardAndFinish());

        for (JButton btn : new JButton[]{bW, bP, bD, bS, bT, bQ, bB, bE}) grid.add(btn);
        p.add(infoBox, BorderLayout.NORTH);
        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildBalanceScreen() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        lblBalHolder = valLbl(Colors.TEXT_MAIN, null);
        lblBalAccNo = valLbl(Colors.TEXT_MAIN, null);
        lblBalCardNo = valLbl(Colors.TEXT_MAIN, null);
        lblBalAvailable = valLbl(Colors.PRIMARY, new Font("Segoe UI", Font.BOLD, 24));

        JPanel card = kvGrid(12, 16,
            new String[]{"balance.holder", "balance.acc_no", "balance.card_no", "balance.available"},
            new JLabel[]{lblBalHolder, lblBalAccNo, lblBalCardNo, lblBalAvailable});
        JPanel wrap = new JPanel(new BorderLayout(0, 12));
        wrap.setBackground(Color.WHITE);
        wrap.add(card, BorderLayout.CENTER);
        wrap.add(i18nLbl("balance.note", SwingConstants.CENTER, new Font("Segoe UI", Font.ITALIC, 13), Colors.TEXT_MUTED), BorderLayout.SOUTH);

        JButton back = i18nBtn(Theme.createPrimaryButton(""), "balance.btn_back");
        JButton exit = i18nBtn(Theme.createDangerButton(""), "balance.btn_exit");
        back.addActionListener(e -> navigateTo(ATMState.MAIN_MENU));
        exit.addActionListener(e -> ejectCardAndFinish());

        p.add(wrap, BorderLayout.CENTER);
        p.add(btnRow(back, exit), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildWithdrawMenuScreen() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel grid = new JPanel(new GridLayout(4, 2, 18, 16));
        grid.setOpaque(false);
        long[] amts = {100_000L, 1_000_000L, 200_000L, 2_000_000L, 500_000L, 5_000_000L};
        for (int i = 0; i < amts.length; i++) {
            long val = amts[i];
            JButton b = Theme.createAtmMenuButton(MoneyFormat.formatMoney(val), i % 2 == 0);
            b.addActionListener(e -> prepareWithdrawConfirm(val));
            grid.add(b);
        }
        JButton back = i18nBtn(Theme.createAtmMenuButton("", true), "withdraw.btn_back");
        back.addActionListener(e -> navigateTo(ATMState.MAIN_MENU));
        grid.add(back);
        grid.add(new JLabel());

        JPanel customBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        customBox.setBackground(Color.WHITE);
        customBox.setBorder(BorderFactory.createLineBorder(Colors.ACCENT_BORDER, 1, true));
        txtWithdrawCustom = new JTextField(13);
        txtWithdrawCustom.setFont(Fonts.INPUT);
        txtWithdrawCustom.setHorizontalAlignment(SwingConstants.RIGHT);
        txtWithdrawCustom.setPreferredSize(new Dimension(210, 42));
        Theme.applyFocusStyle(txtWithdrawCustom, true);
        attachMoneyFormatter(txtWithdrawCustom);

        JButton btnConf = i18nBtn(Theme.createPrimaryButton(""), "withdraw.btn_confirm");
        btnConf.setPreferredSize(new Dimension(140, 42));
        btnConf.addActionListener(e -> submitCustomWithdraw());

        customBox.add(i18nLbl("withdraw.custom_label", SwingConstants.LEFT, new Font("Segoe UI", Font.BOLD, 14), Colors.TEXT_MAIN));
        customBox.add(txtWithdrawCustom);
        customBox.add(btnConf);

        p.add(topHeader("withdraw.subtitle", "withdraw.limit_hint"), BorderLayout.NORTH);
        p.add(grid, BorderLayout.CENTER);
        p.add(customBox, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildWithdrawConfirmScreen() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(40, 80, 40, 80));

        lblWConfirmAmount = valLbl(Colors.TEXT_MAIN, null);
        lblWConfirmTotal = valLbl(Colors.PRIMARY, new Font("Segoe UI", Font.BOLD, 20));
        JButton back = i18nBtn(Theme.createSecondaryButton(""), "withdraw.btn_back");
        JButton exec = i18nBtn(Theme.createPrimaryButton(""), "withdraw.btn_execute");
        back.addActionListener(e -> navigateTo(ATMState.WITHDRAW_MENU));
        exec.addActionListener(e -> executeWithdraw());

        JLabel feeVal = valLbl(Colors.TEXT_MAIN, null);
        feeVal.setText("0 VNĐ");
        p.add(i18nLbl("withdraw.confirm_title", SwingConstants.CENTER, Fonts.TITLE, Colors.PRIMARY), BorderLayout.NORTH);
        p.add(kvGrid(12, 16,
            new String[]{"withdraw.confirm_amount", "withdraw.confirm_fee", "withdraw.confirm_total"},
            new JLabel[]{lblWConfirmAmount, feeVal, lblWConfirmTotal}), BorderLayout.CENTER);
        p.add(btnRow(back, exec), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildDepositTrayScreen() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 56, 24, 56));

        JPanel tray = new JPanel(new GridLayout(4, 2, 16, 12));
        tray.setBackground(Color.WHITE);
        tray.setBorder(Theme.BORDER_CARD);
        String[] labels = {"Mệnh giá 500.000 VNĐ (số tờ):", "Mệnh giá 200.000 VNĐ (số tờ):", "Mệnh giá 100.000 VNĐ (số tờ):", "Mệnh giá  50.000 VNĐ (số tờ):"};
        for (int i = 0; i < 4; i++) {
            JLabel kl = new JLabel(labels[i]);
            kl.setFont(Fonts.SUB);
            kl.setForeground(Colors.TEXT_MUTED);
            tray.add(kl);
            tray.add(createTouchCounter(i));
        }

        JButton back = i18nBtn(Theme.createSecondaryButton(""), "deposit.btn_back");
        JButton count = i18nBtn(Theme.createPrimaryButton(""), "deposit.btn_count");
        back.addActionListener(e -> navigateTo(ATMState.MAIN_MENU));
        count.addActionListener(e -> countDepositTray());

        p.add(topHeader("deposit.subtitle", "deposit.note"), BorderLayout.NORTH);
        p.add(tray, BorderLayout.CENTER);
        p.add(btnRow(back, count), BorderLayout.SOUTH);
        return p;
    }

    private JPanel createTouchCounter(int idx) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        JButton minus = Theme.createSecondaryButton("-");
        JButton plus = Theme.createPrimaryButton("+");
        for (JButton b : new JButton[]{minus, plus}) {
            b.setPreferredSize(new Dimension(54, 42));
            b.setFont(new Font("Segoe UI", Font.BOLD, 20));
        }
        JLabel val = new JLabel("0", SwingConstants.CENTER);
        val.setFont(new Font("Consolas", Font.BOLD, 22));
        val.setOpaque(true);
        val.setBackground(Colors.ACCENT_BG);
        val.setBorder(BorderFactory.createLineBorder(Colors.ACCENT_BORDER, 1, true));
        lblDepositCounts[idx] = val;

        minus.addActionListener(e -> { if (depositCounts[idx] > 0) val.setText(String.valueOf(--depositCounts[idx])); });
        plus.addActionListener(e -> { if (depositCounts[idx] < 200) val.setText(String.valueOf(++depositCounts[idx])); });
        row.add(minus, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        row.add(plus, BorderLayout.EAST);
        return row;
    }

    private JPanel buildDepositCountingScreen() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(30, 70, 30, 70));

        JPanel box = new JPanel(new BorderLayout(0, 12));
        box.setBackground(Color.WHITE);
        box.setBorder(Theme.BORDER_CARD);
        lblDepCountDetail = new JLabel("", SwingConstants.CENTER);
        lblDepCountDetail.setFont(Fonts.MONO);
        lblDepCountTotal = new JLabel("", SwingConstants.CENTER);
        lblDepCountTotal.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblDepCountTotal.setForeground(Colors.PRIMARY);
        box.add(lblDepCountDetail, BorderLayout.CENTER);
        box.add(lblDepCountTotal, BorderLayout.SOUTH);

        JButton recount = i18nBtn(Theme.createSecondaryButton(""), "deposit.btn_recount");
        JButton confirm = i18nBtn(Theme.createPrimaryButton(""), "deposit.btn_confirm");
        recount.addActionListener(e -> navigateTo(ATMState.DEPOSIT_TRAY));
        confirm.addActionListener(e -> executeDeposit());

        p.add(topHeader("deposit.counting_title", "deposit.counting_sub"), BorderLayout.NORTH);
        p.add(box, BorderLayout.CENTER);
        p.add(btnRow(recount, confirm), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildTransferBankScreen() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel grid = new JPanel(new GridLayout(4, 2, 18, 16));
        grid.setOpaque(false);
        for (int i = 0; i < supportedBanks.size() && i < 6; i++) {
            BankInfo b = supportedBanks.get(i);
            String feeTxt = b.fee == 0 ? "Miễn phí" : ("Phí " + MoneyFormat.formatMoney(b.fee));
            JButton btn = Theme.createAtmMenuButton(b.name + " (" + feeTxt + ")", i % 2 == 0);
            btn.addActionListener(e -> selectBankForTransfer(b));
            grid.add(btn);
        }
        JButton back = i18nBtn(Theme.createAtmMenuButton("", true), "withdraw.btn_back");
        back.addActionListener(e -> navigateTo(ATMState.MAIN_MENU));
        grid.add(back);
        grid.add(new JLabel());

        p.add(topHeader("transfer.bank_title", "transfer.bank_sub"), BorderLayout.NORTH);
        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    private void selectBankForTransfer(BankInfo b) {
        selectedBank = b;
        pendingTransName = "";
        lblTransReceiverVal.setText("---");
        txtTransAcc.setText("");
        txtTransAmount.setText("");
        txtTransContent.setText(I18n.get("transfer.default_content"));
        navigateTo(ATMState.TRANSFER_INPUT);
    }

    private JPanel buildTransferInputScreen() {
        JPanel p = new JPanel(new BorderLayout(16, 12));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(Theme.BORDER_CARD);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        lblSelectedBankVal = valLbl(Colors.TEXT_MAIN, null);
        txtTransAcc = new JTextField(16);
        txtTransAcc.setFont(Fonts.INPUT);
        JButton lookup = i18nBtn(Theme.createPrimaryButton(""), "transfer.btn_lookup");
        lookup.addActionListener(e -> lookupBeneficiaryName());
        lblTransReceiverVal = valLbl(Colors.PRIMARY, null);
        lblTransReceiverVal.setText("---");
        txtTransAmount = new JTextField(16);
        txtTransAmount.setFont(Fonts.INPUT);
        attachMoneyFormatter(txtTransAmount);
        txtTransContent = new JTextField(I18n.get("transfer.default_content"));
        txtTransContent.setFont(Fonts.SUB);

        bindFocusTracker(txtTransAcc, true);
        bindFocusTracker(txtTransAmount, false);
        bindFocusTracker(txtTransContent, false);

        g.gridx = 0; g.gridy = 0; form.add(i18nLbl("transfer.bank_label", SwingConstants.LEFT, Fonts.SUB, Colors.TEXT_MUTED), g);
        g.gridx = 1; g.gridwidth = 2; form.add(lblSelectedBankVal, g);
        g.gridwidth = 1; g.gridx = 0; g.gridy = 1; form.add(i18nLbl("transfer.acc_label", SwingConstants.LEFT, Fonts.SUB, Colors.TEXT_MUTED), g);
        g.gridx = 1; form.add(txtTransAcc, g);
        g.gridx = 2; form.add(lookup, g);
        g.gridx = 0; g.gridy = 2; form.add(i18nLbl("transfer.receiver_label", SwingConstants.LEFT, Fonts.SUB, Colors.TEXT_MUTED), g);
        g.gridx = 1; g.gridwidth = 2; form.add(lblTransReceiverVal, g);
        g.gridwidth = 1; g.gridx = 0; g.gridy = 3; form.add(i18nLbl("transfer.amount_label", SwingConstants.LEFT, Fonts.SUB, Colors.TEXT_MUTED), g);
        g.gridx = 1; g.gridwidth = 2; form.add(txtTransAmount, g);
        g.gridwidth = 1; g.gridx = 0; g.gridy = 4; form.add(i18nLbl("transfer.content_label", SwingConstants.LEFT, Fonts.SUB, Colors.TEXT_MUTED), g);
        g.gridx = 1; g.gridwidth = 2; form.add(txtTransContent, g);

        JButton back = i18nBtn(Theme.createSecondaryButton(""), "withdraw.btn_back");
        JButton cont = i18nBtn(Theme.createPrimaryButton(""), "transfer.btn_continue");
        back.addActionListener(e -> navigateTo(ATMState.TRANSFER_BANK));
        cont.addActionListener(e -> prepareTransferConfirm());

        p.add(form, BorderLayout.CENTER);
        p.add(btnRow(back, cont), BorderLayout.SOUTH);
        return p;
    }

    private void bindFocusTracker(JTextField field, boolean initialFocus) {
        Theme.applyFocusStyle(field, initialFocus);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                activeInputField = field;
                updateTransferFocusBorders();
            }
        });
    }

    private void updateTransferFocusBorders() {
        for (JTextField f : new JTextField[]{txtTransAcc, txtTransAmount, txtTransContent}) {
            if (f != null) Theme.applyFocusStyle(f, activeInputField == f);
        }
    }

    private JPanel buildTransferConfirmScreen() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(26, 70, 26, 70));

        lblTransConfBank = valLbl(null, null);
        lblTransConfSender = valLbl(null, null);
        lblTransConfReceiver = valLbl(null, null);
        lblTransConfName = valLbl(Colors.PRIMARY, null);
        lblTransConfAmount = valLbl(null, null);
        lblTransConfFee = valLbl(null, null);
        lblTransConfTotal = valLbl(Colors.PRIMARY, new Font("Segoe UI", Font.BOLD, 17));
        lblTransConfContent = valLbl(null, null);

        JButton back = i18nBtn(Theme.createSecondaryButton(""), "withdraw.btn_back");
        JButton exec = i18nBtn(Theme.createPrimaryButton(""), "transfer.btn_confirm");
        back.addActionListener(e -> navigateTo(ATMState.TRANSFER_INPUT));
        exec.addActionListener(e -> executeTransfer());

        p.add(kvGrid(12, 8,
            new String[]{"transfer.confirm_bank", "transfer.confirm_sender", "transfer.confirm_receiver", "transfer.confirm_name",
                "transfer.confirm_amount", "transfer.confirm_fee", "transfer.confirm_total", "transfer.confirm_content"},
            new JLabel[]{lblTransConfBank, lblTransConfSender, lblTransConfReceiver, lblTransConfName,
                lblTransConfAmount, lblTransConfFee, lblTransConfTotal, lblTransConfContent}), BorderLayout.CENTER);
        p.add(btnRow(back, exec), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildPinChangeOldScreen() {
        txtPinOld = new JPasswordField(6);
        return buildSinglePinStepPanel("pin_change.step1_title", "pin_change.step1_sub", txtPinOld, "card.btn_continue",
            () -> navigateTo(ATMState.MAIN_MENU), () -> {
                if (!Hasher.isValidPinFormat(new String(txtPinOld.getPassword()))) {
                    ATMDialogs.showErrorModal(this, I18n.get("err.pin_format"));
                    txtPinOld.setText("");
                } else navigateTo(ATMState.PIN_CHANGE_NEW);
            });
    }

    private JPanel buildPinChangeNewScreen() {
        txtPinNew = new JPasswordField(6);
        return buildSinglePinStepPanel("pin_change.step2_title", "pin_change.step2_sub", txtPinNew, "card.btn_continue",
            () -> navigateTo(ATMState.PIN_CHANGE_OLD), () -> {
                String oldP = new String(txtPinOld.getPassword()), newP = new String(txtPinNew.getPassword());
                String err = !Hasher.isValidPinFormat(newP) ? "err.pin_format"
                    : newP.equals(oldP) ? "err.pin_same_old" : Hasher.isWeakPin(newP) ? "err.pin_weak" : null;
                if (err != null) { ATMDialogs.showErrorModal(this, I18n.get(err)); txtPinNew.setText(""); }
                else navigateTo(ATMState.PIN_CHANGE_CONFIRM);
            });
    }

    private JPanel buildPinChangeConfirmScreen() {
        txtPinConf = new JPasswordField(6);
        return buildSinglePinStepPanel("pin_change.step3_title", "pin_change.step3_sub", txtPinConf, "pin.btn_confirm",
            () -> navigateTo(ATMState.PIN_CHANGE_NEW), this::executeChangePin);
    }

    private JPanel buildSinglePinStepPanel(String titleKey, String subKey, JPasswordField field, String nextKey, Runnable onBack, Runnable onNext) {
        JPanel p = new JPanel(new BorderLayout(20, 16));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));

        JPanel box = new JPanel(new GridBagLayout());
        box.setBackground(Color.WHITE);
        box.setBorder(Theme.BORDER_CARD);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.insets = new Insets(10, 10, 10, 10);

        field.setFont(new Font("Consolas", Font.BOLD, 28));
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setPreferredSize(new Dimension(240, 52));
        Theme.applyFocusStyle(field, true);

        JButton backBtn = i18nBtn(Theme.createSecondaryButton(""), "card.btn_back");
        JButton clearBtn = i18nBtn(Theme.createSecondaryButton(""), "pin.btn_clear");
        JButton nextBtn = i18nBtn(Theme.createPrimaryButton(""), nextKey);
        backBtn.addActionListener(e -> onBack.run());
        clearBtn.addActionListener(e -> field.setText(""));
        nextBtn.addActionListener(e -> onNext.run());

        g.gridy = 0; box.add(i18nLbl(titleKey, SwingConstants.CENTER, Fonts.TITLE, Colors.PRIMARY), g);
        g.gridy = 1; box.add(i18nHtml(subKey, 340, Fonts.SUB, Colors.TEXT_MAIN), g);
        g.gridy = 2; box.add(field, g);
        g.gridy = 3; box.add(btnRow(backBtn, clearBtn, nextBtn), g);

        p.add(box, BorderLayout.CENTER);
        p.add(buildOnScreenNumpad(() -> field, onNext), BorderLayout.EAST);
        return p;
    }

    private JPanel buildPinChangeSuccessScreen() {
        JButton menu = i18nBtn(Theme.createPrimaryButton(""), "pin_change.btn_menu");
        menu.addActionListener(e -> navigateTo(ATMState.MAIN_MENU));
        return centeredCard(Colors.SUCCESS, 540, 250,
            i18nLbl("pin_change.success_title", SwingConstants.CENTER, Fonts.TITLE, Colors.SUCCESS),
            i18nHtml("pin_change.success_sub", 440, Fonts.SUB, Colors.TEXT_MAIN),
            btnRow(menu));
    }

    private JPanel buildMiniStatementScreen() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        lblStmtSummary = new JLabel("", SwingConstants.LEFT);
        lblStmtSummary.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStmtSummary.setForeground(Colors.TEXT_MAIN);

        String[] colKeys = {"stmt.col_time", "stmt.col_ref", "stmt.col_type", "stmt.col_amount", "stmt.col_balance", "stmt.col_partner"};
        modelStatement = new DefaultTableModel(Arrays.stream(colKeys).map(I18n::get).toArray(String[]::new), 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tblStatement = new JTable(modelStatement);
        tblStatement.setRowHeight(38);
        tblStatement.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblStatement.setFillsViewportHeight(true);
        int[] widths = {120, 118, 68, 120, 125, 235};
        for (int i = 0; i < widths.length; i++) tblStatement.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        i18nUpdaters.add(() -> {
            for (int i = 0; i < colKeys.length; i++) tblStatement.getColumnModel().getColumn(i).setHeaderValue(I18n.get(colKeys[i]));
            tblStatement.getTableHeader().repaint();
        });

        tblStatement.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            { setOpaque(true); }
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                setBackground(Colors.PRIMARY);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setHorizontalAlignment((c == 3 || c == 4) ? RIGHT : CENTER);
                setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                return this;
            }
        });
        tblStatement.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                String text = v == null ? "" : v.toString();
                setHorizontalAlignment((c == 3 || c == 4) ? RIGHT : LEFT);
                setForeground((c == 3 && text.startsWith("-") && text.length() > 1) ? Colors.DANGER
                    : (c == 3 && text.startsWith("+")) ? Colors.SUCCESS : Colors.TEXT_MAIN);
                setBackground(r % 2 == 0 ? Color.WHITE : Colors.SCREEN_BG);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(tblStatement);
        scroll.setBorder(BorderFactory.createLineBorder(Colors.ACCENT_BORDER, 1));

        JButton exp = i18nBtn(Theme.createSecondaryButton(""), "stmt.btn_export");
        JButton back = i18nBtn(Theme.createPrimaryButton(""), "stmt.btn_back");
        exp.addActionListener(e -> exportStatementToFile());
        back.addActionListener(e -> navigateTo(ATMState.MAIN_MENU));

        p.add(lblStmtSummary, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        p.add(btnRow(exp, back), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildProcessingScreen() {
        JPanel barWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        barWrap.setOpaque(false);
        barWrap.add(Theme.createOrangeProgressBar());
        lblProcNotice = new JLabel(I18n.get("proc.notice_default"), SwingConstants.CENTER);
        lblProcNotice.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblProcNotice.setForeground(Colors.TEXT_MUTED);
        return centeredCard(Colors.ACCENT_BORDER, 540, 240,
            i18nLbl("proc.title", SwingConstants.CENTER, Fonts.TITLE, Colors.PRIMARY),
            i18nLbl("proc.status", SwingConstants.CENTER, Fonts.SUB, Colors.TEXT_MAIN),
            barWrap, lblProcNotice);
    }

    private JPanel buildAskReceiptScreen() {
        lblAskRef = new JLabel("", SwingConstants.CENTER);
        lblAskRef.setFont(new Font("Consolas", Font.BOLD, 14));
        lblAskRef.setForeground(Colors.TEXT_MUTED);
        JButton yes = i18nBtn(Theme.createPrimaryButton(""), "ask.btn_yes");
        JButton no = i18nBtn(Theme.createSecondaryButton(""), "ask.btn_no");
        yes.setPreferredSize(new Dimension(210, 46));
        no.setPreferredSize(new Dimension(210, 46));
        yes.addActionListener(e -> { wantsReceipt = true; navigateTo(ATMState.TAKE_CARD); });
        no.addActionListener(e -> { wantsReceipt = false; navigateTo(ATMState.TAKE_CARD); });
        return centeredCard(Colors.SUCCESS, 560, 290,
            i18nLbl("ask.title", SwingConstants.CENTER, new Font("Segoe UI", Font.BOLD, 24), Colors.SUCCESS),
            i18nLbl("ask.sub", SwingConstants.CENTER, Fonts.SUB, Colors.TEXT_MAIN),
            lblAskRef, btnRow(no, yes));
    }

    private JPanel buildTakeCardScreen() {
        JButton done = i18nBtn(Theme.createPrimaryButton(""), "take_card.btn_taken");
        done.setPreferredSize(new Dimension(220, 46));
        done.addActionListener(e -> onTakeCardDone());
        return centeredCard(Colors.PRIMARY, 580, 280,
            i18nLbl("take_card.title", SwingConstants.CENTER, Fonts.TITLE, Colors.PRIMARY),
            i18nLbl("take_card.sub", SwingConstants.CENTER, Fonts.SUB, Colors.TEXT_MAIN),
            i18nLbl("take_card.warn", SwingConstants.CENTER, new Font("Segoe UI", Font.BOLD, 14), Colors.DANGER),
            btnRow(done));
    }

    private void onTakeCardDone() {
        if ("RUT".equals(lastTxnType)) navigateTo(ATMState.TAKE_CASH);
        else if (wantsReceipt) prepareAndShowReceipt();
        else navigateTo(ATMState.ANOTHER_TXN);
    }

    private JPanel buildTakeCashScreen() {
        lblTakeCashAmount = new JLabel("", SwingConstants.CENTER);
        lblTakeCashAmount.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTakeCashAmount.setForeground(Colors.PRIMARY);
        lblTakeCashCountdown = new JLabel("", SwingConstants.CENTER);
        lblTakeCashCountdown.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTakeCashCountdown.setForeground(Colors.DANGER);
        JButton done = i18nBtn(Theme.createPrimaryButton(""), "take_cash.btn_taken");
        done.setPreferredSize(new Dimension(220, 46));
        done.addActionListener(e -> onTakeCashDone());
        return centeredCard(Colors.SUCCESS, 580, 310,
            i18nLbl("take_cash.title", SwingConstants.CENTER, Fonts.TITLE, Colors.SUCCESS),
            i18nLbl("take_cash.sub", SwingConstants.CENTER, Fonts.SUB, Colors.TEXT_MAIN),
            lblTakeCashAmount, lblTakeCashCountdown, btnRow(done));
    }

    private void onTakeCashDone() {
        stopCashRetractTimer();
        if (wantsReceipt) prepareAndShowReceipt();
        else navigateTo(ATMState.ANOTHER_TXN);
    }

    private JPanel buildReceiptViewScreen() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Colors.SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(18, 90, 18, 90));

        txtReceipt = new JTextArea();
        txtReceipt.setEditable(false);
        txtReceipt.setFont(Fonts.MONO);
        txtReceipt.setBackground(Color.WHITE);
        txtReceipt.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JScrollPane sp = new JScrollPane(txtReceipt);
        sp.setBorder(BorderFactory.createLineBorder(Colors.ACCENT_BORDER, 1));

        JButton save = i18nBtn(Theme.createSecondaryButton(""), "receipt.btn_save");
        JButton cont = i18nBtn(Theme.createPrimaryButton(""), "receipt.btn_continue");
        save.addActionListener(e -> saveReceiptToFile());
        cont.addActionListener(e -> navigateTo(ATMState.ANOTHER_TXN));

        p.add(topHeader("receipt.title", "receipt.sub"), BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        p.add(btnRow(save, cont), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildAnotherTxnScreen() {
        JButton no = i18nBtn(Theme.createSecondaryButton(""), "another.btn_no");
        JButton yes = i18nBtn(Theme.createPrimaryButton(""), "another.btn_yes");
        no.addActionListener(e -> resetToWait());
        yes.addActionListener(e -> onStartCardInsert());
        return centeredCard(Colors.ACCENT_BORDER, 540, 240,
            i18nLbl("another.title", SwingConstants.CENTER, Fonts.TITLE, Colors.PRIMARY),
            i18nLbl("another.sub", SwingConstants.CENTER, Fonts.SUB, Colors.TEXT_MAIN),
            btnRow(no, yes));
    }

    private JPanel buildSystemErrorScreen() {
        lblSysErrTitle = new JLabel("", SwingConstants.CENTER);
        lblSysErrTitle.setFont(Fonts.TITLE);
        lblSysErrTitle.setForeground(Colors.DANGER);
        lblSysErrSub = new JLabel("", SwingConstants.CENTER);
        lblSysErrSub.setFont(Fonts.SUB);
        btnSysErrRetry = i18nBtn(Theme.createPrimaryButton(""), "sys_err.btn_retry");
        btnSysErrMenu = i18nBtn(Theme.createPrimaryButton(""), "sys_err.btn_menu");
        btnSysErrEject = i18nBtn(Theme.createDangerButton(""), "sys_err.btn_eject");
        btnSysErrRetry.addActionListener(e -> resetToWait());
        btnSysErrMenu.addActionListener(e -> navigateTo(ATMState.MAIN_MENU));
        btnSysErrEject.addActionListener(e -> ejectCardAndFinish());
        return centeredCard(Colors.WARNING, 580, 280, lblSysErrTitle, lblSysErrSub, btnRow(btnSysErrRetry, btnSysErrMenu, btnSysErrEject));
    }

    private JPanel buildOnScreenNumpad(Supplier<JTextField> targetSupplier, Runnable onEnter) {
        JPanel pad = new JPanel(new GridLayout(4, 3, 8, 8));
        pad.setBackground(Color.WHITE);
        pad.setBorder(Theme.BORDER_CARD);
        pad.setPreferredSize(new Dimension(240, 280));
        for (String k : new String[]{"1","2","3","4","5","6","7","8","9","C","0","OK"}) {
            JButton b = new JButton(k);
            b.setFont(new Font("Segoe UI", Font.BOLD, 16));
            b.setOpaque(true);
            b.setFocusPainted(false);
            b.setFocusable(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setBackground("OK".equals(k) ? Colors.PRIMARY : "C".equals(k) ? new Color(254, 226, 226) : Colors.ACCENT_BG);
            b.setForeground("OK".equals(k) ? Color.WHITE : "C".equals(k) ? Colors.DANGER : Colors.TEXT_MAIN);
            b.addActionListener(e -> {
                resetSessionTimer();
                JTextField tf = targetSupplier.get();
                if ("OK".equals(k)) { onEnter.run(); return; }
                String cur = getText(tf);
                if ("C".equals(k) && !cur.isEmpty()) tf.setText(cur.substring(0, cur.length() - 1));
                else if (!"C".equals(k) && (!(tf instanceof JPasswordField) || cur.length() < 6)) tf.setText(cur + k);
                tf.setCaretPosition(getText(tf).length());
            });
            pad.add(b);
        }
        return pad;
    }

    private String getText(JTextField tf) {
        return tf instanceof JPasswordField ? new String(((JPasswordField) tf).getPassword()) : tf.getText();
    }

    private void navigateTo(ATMState state) {
        currentState = state;
        resetSessionTimer();
        Arrays.fill(leftActions, null);
        Arrays.fill(rightActions, null);
        enterAction = escAction = null;

        switch (state) {
            case WAIT -> {
                session.setActive(false);
                lblHeaderTitle.setText("");
                lblHeaderClock.setVisible(false);
                lblFooterCountdown.setText("");
                hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.CARD);
                enterAction = this::onStartCardInsert;
            }
            case CARD_INSERT -> {
                session.setActive(false);
                lblHeaderTitle.setText(I18n.get("card.title"));
                lblHeaderClock.setVisible(true);
                hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.CARD);
                txtCardNumber.setText("");
                activeInputField = txtCardNumber;
                enterAction = this::submitCardNumber;
                escAction = () -> navigateTo(ATMState.WAIT);
            }
            case LANGUAGE_SELECT -> {
                lblHeaderTitle.setText(I18n.get("lang.title"));
                lblHeaderClock.setVisible(true);
                hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.NONE);
                rightActions[0] = () -> { switchLanguage(false); navigateTo(ATMState.PIN_ENTRY); };
                rightActions[1] = () -> { switchLanguage(true); navigateTo(ATMState.PIN_ENTRY); };
                leftActions[3] = escAction = this::ejectCardAndFinish;
            }
            case PIN_ENTRY -> {
                lblHeaderTitle.setText(I18n.get("pin.title"));
                lblHeaderClock.setVisible(true);
                txtPin.setText("");
                activeInputField = txtPin;
                enterAction = this::submitPin;
                escAction = this::ejectCardAndFinish;
                SwingUtilities.invokeLater(() -> txtPin.requestFocusInWindow());
            }
            case CARD_LOCKED -> {
                session.setActive(false);
                lblHeaderTitle.setText(I18n.get("locked.title"));
                hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.NONE);
                enterAction = this::resetToWait;
            }
            case MAIN_MENU -> {
                session.setActive(true);
                lblHeaderTitle.setText(I18n.get("menu.title"));
                lblHeaderClock.setVisible(true);
                hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.NONE);
                String name = I18n.isEnglish() ? MoneyFormat.toAsciiUpperName(session.getHolderName()) : session.getHolderName();
                lblMenuWelcome.setText(I18n.get("menu.welcome", name));
                lblMenuAccNo.setText(I18n.get("menu.acc_no", MoneyFormat.maskAccountNumber(session.getAccountNo())));
                lblMenuCardNo.setText(I18n.get("menu.card_no", MoneyFormat.maskCardNumber(session.getCardNo())));
                leftActions[0] = () -> navigateTo(ATMState.WITHDRAW_MENU);
                leftActions[1] = () -> navigateTo(ATMState.DEPOSIT_TRAY);
                leftActions[2] = () -> navigateTo(ATMState.TRANSFER_BANK);
                leftActions[3] = this::openBalanceInquiry;
                rightActions[0] = () -> navigateTo(ATMState.PIN_CHANGE_OLD);
                rightActions[1] = () -> { loadStatement(); navigateTo(ATMState.MINI_STATEMENT); };
                rightActions[2] = () -> prepareWithdrawConfirm(1_000_000L);
                rightActions[3] = escAction = this::ejectCardAndFinish;
            }
            case BALANCE_INQUIRY -> { lblHeaderTitle.setText(I18n.get("balance.title")); escAction = () -> navigateTo(ATMState.MAIN_MENU); }
            case WITHDRAW_MENU -> {
                lblHeaderTitle.setText(I18n.get("withdraw.title"));
                txtWithdrawCustom.setText("");
                activeInputField = txtWithdrawCustom;
                leftActions[0] = () -> prepareWithdrawConfirm(100_000L);
                leftActions[1] = () -> prepareWithdrawConfirm(200_000L);
                leftActions[2] = () -> prepareWithdrawConfirm(500_000L);
                leftActions[3] = escAction = () -> navigateTo(ATMState.MAIN_MENU);
                rightActions[0] = () -> prepareWithdrawConfirm(1_000_000L);
                rightActions[1] = () -> prepareWithdrawConfirm(2_000_000L);
                rightActions[2] = () -> prepareWithdrawConfirm(5_000_000L);
                enterAction = this::submitCustomWithdraw;
            }
            case WITHDRAW_CONFIRM -> {
                lblHeaderTitle.setText(I18n.get("withdraw.confirm_title"));
                enterAction = this::executeWithdraw;
                escAction = () -> navigateTo(ATMState.WITHDRAW_MENU);
            }
            case DEPOSIT_TRAY -> {
                lblHeaderTitle.setText(I18n.get("deposit.title"));
                hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.CASH);
                Arrays.fill(depositCounts, 0);
                for (JLabel l : lblDepositCounts) if (l != null) l.setText("0");
                escAction = () -> navigateTo(ATMState.MAIN_MENU);
            }
            case DEPOSIT_COUNTING -> {
                lblHeaderTitle.setText(I18n.get("deposit.counting_title"));
                hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.NONE);
                enterAction = this::executeDeposit;
                escAction = () -> navigateTo(ATMState.DEPOSIT_TRAY);
            }
            case TRANSFER_BANK -> {
                lblHeaderTitle.setText(I18n.get("transfer.bank_title"));
                for (int i = 0; i < supportedBanks.size() && i < 6; i++) {
                    BankInfo b = supportedBanks.get(i);
                    (i % 2 == 0 ? leftActions : rightActions)[i / 2] = () -> selectBankForTransfer(b);
                }
                leftActions[3] = escAction = () -> navigateTo(ATMState.MAIN_MENU);
            }
            case TRANSFER_INPUT -> {
                lblHeaderTitle.setText(I18n.get("transfer.input_title"));
                lblSelectedBankVal.setText(selectedBank.name);
                activeInputField = txtTransAcc;
                updateTransferFocusBorders();
                SwingUtilities.invokeLater(() -> txtTransAcc.requestFocusInWindow());
                escAction = () -> navigateTo(ATMState.TRANSFER_BANK);
            }
            case TRANSFER_CONFIRM -> {
                lblHeaderTitle.setText(I18n.get("transfer.confirm_title"));
                enterAction = this::executeTransfer;
                escAction = () -> navigateTo(ATMState.TRANSFER_INPUT);
            }
            case PIN_CHANGE_OLD -> focusPinStep("pin_change.step1_title", txtPinOld, ATMState.MAIN_MENU);
            case PIN_CHANGE_NEW -> focusPinStep("pin_change.step2_title", txtPinNew, ATMState.PIN_CHANGE_OLD);
            case PIN_CHANGE_CONFIRM -> focusPinStep("pin_change.step3_title", txtPinConf, ATMState.PIN_CHANGE_NEW);
            case PIN_CHANGE_SUCCESS -> { lblHeaderTitle.setText(I18n.get("pin_change.success_title")); enterAction = () -> navigateTo(ATMState.MAIN_MENU); }
            case MINI_STATEMENT -> { lblHeaderTitle.setText(I18n.get("stmt.title")); escAction = () -> navigateTo(ATMState.MAIN_MENU); }
            case PROCESSING -> lblHeaderTitle.setText(I18n.get("proc.title"));
            case ASK_RECEIPT -> {
                lblHeaderTitle.setText(I18n.get("ask.title"));
                lblAskRef.setText(I18n.get("ask.ref", lastTxnResult != null ? lastTxnResult.maThamChieu : "-"));
            }
            case TAKE_CARD -> {
                session.setActive(false);
                lblFooterCountdown.setText("");
                lblHeaderTitle.setText(I18n.get("take_card.title"));
                hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.CARD);
                enterAction = this::onTakeCardDone;
            }
            case TAKE_CASH -> {
                lblHeaderTitle.setText(I18n.get("take_cash.title"));
                hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.CASH);
                lblTakeCashAmount.setText(I18n.get("take_cash.amount", MoneyFormat.formatMoney(lastTxnAmount)));
                startCashRetractTimer();
                enterAction = this::onTakeCashDone;
            }
            case RECEIPT_VIEW -> {
                lblHeaderTitle.setText(I18n.get("receipt.title"));
                hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.RECEIPT);
                enterAction = () -> navigateTo(ATMState.ANOTHER_TXN);
            }
            case ANOTHER_TXN -> { lblHeaderTitle.setText(I18n.get("another.title")); hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.NONE); }
            case SYSTEM_ERROR -> {
                lblHeaderTitle.setText(I18n.get("sys_err.header_title"));
                hardwareSlots.setActiveSlot(HardwareSlotPanel.ActiveSlot.NONE);
                applySystemErrorLabels();
            }
        }
        updateFdkVisuals();
        cardLayout.show(screenRoot, state.name());
    }

    private void focusPinStep(String titleKey, JPasswordField field, ATMState backState) {
        lblHeaderTitle.setText(I18n.get(titleKey));
        field.setText("");
        activeInputField = field;
        SwingUtilities.invokeLater(field::requestFocusInWindow);
        escAction = () -> navigateTo(backState);
    }

    private void updateFdkVisuals() {
        for (int i = 0; i < 4; i++) {
            styleFdkButton(leftFdk[i], leftActions[i] != null);
            styleFdkButton(rightFdk[i], rightActions[i] != null);
        }
    }

    private void styleFdkButton(JButton btn, boolean active) {
        if (btn == null) return;
        btn.setEnabled(active);
        btn.setBackground(active ? Colors.FDK_KEY_BG : new Color(36, 38, 44));
        btn.setForeground(active ? Color.WHITE : new Color(75, 80, 90));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(active ? new Color(115, 120, 132) : new Color(50, 53, 60), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    private void onStartCardInsert() {
        if (!DbConfig.testConnection()) { sysErrMode = "DB_ERROR"; navigateTo(ATMState.SYSTEM_ERROR); return; }
        if ("MAINTENANCE".equalsIgnoreCase(sysService.getAtmStatus())) { sysErrMode = "MAINTENANCE"; navigateTo(ATMState.SYSTEM_ERROR); return; }
        navigateTo(ATMState.CARD_INSERT);
    }

    private void submitCardNumber() {
        String raw = txtCardNumber.getText().replaceAll("\\s+", "");
        if (!raw.matches("\\d{16}")) { ATMDialogs.showErrorModal(this, I18n.get("err.card_invalid")); return; }
        String status = authService.checkCard(raw);
        if ("NOT_FOUND".equals(status)) { ATMDialogs.showErrorModal(this, I18n.get("err.card_not_found")); return; }
        if ("DB_ERROR".equals(status)) { sysErrMode = "DB_ERROR"; navigateTo(ATMState.SYSTEM_ERROR); return; }
        session.setCardNo(raw);
        navigateTo(("LOCKED".equalsIgnoreCase(status) || "RETAINED".equalsIgnoreCase(status)) ? ATMState.CARD_LOCKED : ATMState.LANGUAGE_SELECT);
    }

    private void submitPin() {
        String pin = new String(txtPin.getPassword());
        if (!pin.matches("\\d{6}")) { ATMDialogs.showErrorModal(this, I18n.get("err.pin_format")); txtPin.setText(""); return; }
        LoginResult res = authService.login(session.getCardNo(), pin);
        if (res.success) {
            session.startSession(session.getCardNo(), res.accId, res.tenChuTk, res.soTaiKhoan);
            navigateTo(ATMState.MAIN_MENU);
        } else if (res.locked) {
            navigateTo(ATMState.CARD_LOCKED);
        } else {
            txtPin.setText("");
            ATMDialogs.showErrorModal(this, "err.pin_wrong".equals(res.errorCode) ? I18n.get("err.pin_wrong", res.remainingAttempts) : I18n.get(res.errorCode));
        }
    }

    private void openBalanceInquiry() {
        long bal = sysService.getBalance(session.getAccId());
        if (bal < 0) { ATMDialogs.showErrorModal(this, I18n.get("err.db_error")); return; }
        lblBalHolder.setText(I18n.isEnglish() ? MoneyFormat.toAsciiUpperName(session.getHolderName()) : session.getHolderName());
        lblBalAccNo.setText(MoneyFormat.maskAccountNumber(session.getAccountNo()));
        lblBalCardNo.setText(MoneyFormat.maskCardNumber(session.getCardNo()));
        lblBalAvailable.setText(MoneyFormat.formatMoney(bal));
        navigateTo(ATMState.BALANCE_INQUIRY);
    }

    private void submitCustomWithdraw() {
        String raw = txtWithdrawCustom.getText().replaceAll("[^\\d]", "");
        if (raw.isEmpty()) { ATMDialogs.showErrorModal(this, I18n.get("err.amount_invalid")); return; }
        try { prepareWithdrawConfirm(Long.parseLong(raw)); }
        catch (NumberFormatException ex) { ATMDialogs.showErrorModal(this, I18n.get("err.amount_invalid")); }
    }

    private void prepareWithdrawConfirm(long amount) {
        CashVault vault = sysService.getVaultStatus();
        if (vault == null || vault.tongTien() < CashDispenser.NOTE_UNIT) { sysErrMode = "OUT_OF_CASH"; navigateTo(ATMState.SYSTEM_ERROR); return; }
        String err = dispenser.validateWithdrawRequest(amount, 0L, sysService.getBalance(session.getAccId()), vault);
        if (err != null) { ATMDialogs.showErrorModal(this, resolveErrorMessage(err)); return; }
        pendingWithdrawAmount = amount;
        lblWConfirmAmount.setText(MoneyFormat.formatMoney(amount));
        lblWConfirmTotal.setText(MoneyFormat.formatMoney(amount));
        navigateTo(ATMState.WITHDRAW_CONFIRM);
    }

    private void executeWithdraw() {
        runProcessing(I18n.get("proc.notice_withdraw"), () -> finishTxn(
            withdrawService.withdraw(session.getAccId(), session.getCardNo(), pendingWithdrawAmount),
            "RUT", pendingWithdrawAmount, "-", "VietBank", "Rút tiền mặt tại ATM", ATMState.WITHDRAW_MENU));
    }

    private void countDepositTray() {
        CashBreakdown b = new CashBreakdown(depositCounts[0], depositCounts[1], depositCounts[2], depositCounts[3]);
        if (b.tongTo() <= 0 || b.tongTien() <= 0) { ATMDialogs.showErrorModal(this, I18n.get("err.deposit_empty")); return; }
        if (b.tongTien() > DepositService.DEPOSIT_LIMIT_TXN) {
            ATMDialogs.showErrorModal(this, I18n.get("err.deposit_limit", MoneyFormat.formatMoney(DepositService.DEPOSIT_LIMIT_TXN)));
            return;
        }
        pendingDepositNotes = b;
        lblDepCountDetail.setText(String.format("<html><div style='text-align:center;'>500.000đ x %d tờ &nbsp;|&nbsp; 200.000đ x %d tờ<br>100.000đ x %d tờ &nbsp;|&nbsp;  50.000đ x %d tờ</div></html>",
            depositCounts[0], depositCounts[1], depositCounts[2], depositCounts[3]));
        lblDepCountTotal.setText(I18n.get("deposit.total_label") + " " + MoneyFormat.formatMoney(b.tongTien()));
        navigateTo(ATMState.DEPOSIT_COUNTING);
    }

    private void executeDeposit() {
        runProcessing(I18n.get("proc.notice_deposit"), () -> finishTxn(
            depositService.deposit(session.getAccId(), session.getCardNo(), pendingDepositNotes),
            "NAP", pendingDepositNotes.tongTien(), "-", "VietBank", "Nộp tiền mặt tại ATM", ATMState.DEPOSIT_TRAY));
    }

    private void lookupBeneficiaryName() {
        String dest = txtTransAcc.getText().trim();
        if (dest.isEmpty()) { ATMDialogs.showErrorModal(this, I18n.get("err.transfer_acc_empty")); return; }
        if ("VB".equals(selectedBank.code) && dest.equals(session.getAccountNo())) { ATMDialogs.showErrorModal(this, I18n.get("err.transfer_self")); return; }
        String name = transferService.lookupBeneficiary(selectedBank.code, dest);
        if (name == null) {
            pendingTransName = "";
            lblTransReceiverVal.setText("---");
            ATMDialogs.showErrorModal(this, I18n.get("err.transfer_acc_not_found"));
            return;
        }
        pendingTransName = name;
        pendingTransAcc = dest;
        lblTransReceiverVal.setText(name);
        activeInputField = txtTransAmount;
        updateTransferFocusBorders();
        txtTransAmount.requestFocusInWindow();
    }

    private void prepareTransferConfirm() {
        String dest = txtTransAcc.getText().trim();
        if (pendingTransName.isEmpty() || !dest.equals(pendingTransAcc)) { lookupBeneficiaryName(); if (pendingTransName.isEmpty()) return; }
        String rawAmt = txtTransAmount.getText().replaceAll("[^\\d]", "");
        if (rawAmt.isEmpty()) { ATMDialogs.showErrorModal(this, I18n.get("err.amount_invalid")); return; }
        long amt;
        try { amt = Long.parseLong(rawAmt); } catch (NumberFormatException ex) { ATMDialogs.showErrorModal(this, I18n.get("err.amount_invalid")); return; }
        String err = transferService.validateTransferLogic(amt, selectedBank.fee, 0L, sysService.getBalance(session.getAccId()),
            "VB".equals(selectedBank.code) ? session.getAccountNo() : "", dest);
        if (err != null) { ATMDialogs.showErrorModal(this, resolveErrorMessage(err)); return; }

        pendingTransAmount = amt;
        pendingTransContent = txtTransContent.getText().trim();
        if (pendingTransContent.isEmpty()) pendingTransContent = I18n.get("transfer.default_content");

        lblTransConfBank.setText(selectedBank.name);
        lblTransConfSender.setText(MoneyFormat.maskAccountNumber(session.getAccountNo()));
        lblTransConfReceiver.setText(MoneyFormat.maskAccountNumber(pendingTransAcc));
        lblTransConfName.setText(pendingTransName);
        lblTransConfAmount.setText(MoneyFormat.formatMoney(amt));
        lblTransConfFee.setText(MoneyFormat.formatMoney(selectedBank.fee));
        lblTransConfTotal.setText(MoneyFormat.formatMoney(amt + selectedBank.fee));
        lblTransConfContent.setText(pendingTransContent);
        navigateTo(ATMState.TRANSFER_CONFIRM);
    }

    private void executeTransfer() {
        runProcessing(I18n.get("proc.notice_transfer"), () -> finishTxn(
            transferService.transfer(session.getAccId(), session.getCardNo(), selectedBank, pendingTransAcc, pendingTransAmount, pendingTransContent),
            "CHUYEN", pendingTransAmount, pendingTransAcc + " (" + pendingTransName + ")", selectedBank.name, pendingTransContent, ATMState.TRANSFER_INPUT));
    }

    private void finishTxn(TransactionResult res, String type, long amt, String partner, String bank, String remark, ATMState errState) {
        if (res.thanhCong) {
            lastTxnResult = res;
            lastTxnType = type;
            lastTxnAmount = amt;
            lastTxnPartner = partner;
            lastTxnBank = bank;
            lastTxnContent = remark;
            navigateTo(ATMState.ASK_RECEIPT);
        } else {
            navigateTo(errState);
            ATMDialogs.showErrorModal(this, resolveErrorMessage(res.maLoi));
        }
    }

    private void executeChangePin() {
        String oldP = new String(txtPinOld.getPassword()), newP = new String(txtPinNew.getPassword()), confP = new String(txtPinConf.getPassword());
        if (!newP.equals(confP)) { ATMDialogs.showErrorModal(this, I18n.get("err.pin_mismatch")); txtPinConf.setText(""); return; }
        runProcessing(I18n.get("proc.notice_default"), () -> {
            TransactionResult res = pinService.changePin(session.getCardNo(), session.getAccId(), oldP, newP, confP);
            if (res.thanhCong) navigateTo(ATMState.PIN_CHANGE_SUCCESS);
            else { navigateTo(ATMState.PIN_CHANGE_OLD); ATMDialogs.showErrorModal(this, resolveErrorMessage(res.maLoi)); }
        });
    }

    private void loadStatement() {
        modelStatement.setRowCount(0);
        List<TransactionRecord> list = sysService.getPagedStatement(session.getAccId(), 1, 10);
        lblStmtSummary.setText(I18n.get("stmt.summary", MoneyFormat.maskAccountNumber(session.getAccountNo()), list.size()));
        for (TransactionRecord r : list) {
            String amt = ("RUT".equals(r.loai) || "CHUYEN".equals(r.loai)) ? "-" + MoneyFormat.formatMoney(r.soTien + r.phi)
                : ("NAP".equals(r.loai) || "NHAN".equals(r.loai)) ? "+" + MoneyFormat.formatMoney(r.soTien) : "-";
            String partner = ("-".equals(r.tkDoiTac) ? "" : MoneyFormat.maskAccountNumber(r.tkDoiTac) + " - ") + (r.noiDung == null ? "" : r.noiDung);
            modelStatement.addRow(new Object[]{r.thoiGian, r.maThamChieu, r.loai, amt, MoneyFormat.formatMoney(r.soDuSau), partner});
        }
    }

    private void exportStatementToFile() {
        String file = "SaoKe_" + MoneyFormat.maskAccountNumber(session.getAccountNo()).replace("•", "x") + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
            fw.write("VIETBANK ATM - MINI STATEMENT\nAccount: " + MoneyFormat.maskAccountNumber(session.getAccountNo()) + " | Holder: " + session.getHolderName() + "\n--------------------------------------------------------------------------------\n");
            for (int i = 0; i < modelStatement.getRowCount(); i++) {
                fw.write(String.format("%s | %s | %-8s | %15s | %15s | %s\n",
                    modelStatement.getValueAt(i, 0), modelStatement.getValueAt(i, 1), modelStatement.getValueAt(i, 2),
                    modelStatement.getValueAt(i, 3), modelStatement.getValueAt(i, 4), modelStatement.getValueAt(i, 5)));
            }
            JOptionPane.showMessageDialog(this, "Đã xuất sao kê ra file: " + file);
        } catch (Exception ex) { ATMDialogs.showErrorModal(this, ex.getMessage()); }
    }

    private void runProcessing(String notice, Runnable afterDelay) {
        lblProcNotice.setText(notice);
        navigateTo(ATMState.PROCESSING);
        if (processingTimer != null) processingTimer.stop();
        processingTimer = new Timer(1200, e -> { processingTimer.stop(); afterDelay.run(); });
        processingTimer.setRepeats(false);
        processingTimer.start();
    }

    private void prepareAndShowReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================\n            VIETBANK ATM RECEIPT            \n============================================\n");
        sb.append("ATM ID       : ").append(ATMSystemService.ATM_ID).append("\n");
        sb.append("Địa điểm     : ").append(lastTxnResult != null && lastTxnResult.diaDiemAtm != null ? lastTxnResult.diaDiemAtm : ATMSystemService.ATM_LOCATION).append("\n");
        sb.append("Thời gian    : ").append(lastTxnResult != null ? lastTxnResult.thoiGian : "").append("\n");
        sb.append("Mã tham chiếu: ").append(lastTxnResult != null ? lastTxnResult.maThamChieu : "").append("\n--------------------------------------------\n");
        sb.append("Số thẻ       : ").append(MoneyFormat.maskCardNumber(session.getCardNo())).append("\n");
        sb.append("Số tài khoản : ").append(MoneyFormat.maskAccountNumber(session.getAccountNo())).append("\n");
        sb.append("Chủ tài khoản: ").append(MoneyFormat.toAsciiUpperName(session.getHolderName())).append("\n");
        sb.append("Loại GD      : ").append(lastTxnType).append("\n");
        sb.append("Số tiền      : ").append(MoneyFormat.formatMoney(lastTxnAmount)).append("\n");
        sb.append("Phí giao dịch: ").append(MoneyFormat.formatMoney(lastTxnResult != null ? lastTxnResult.phi : 0L)).append("\n");
        if ("CHUYEN".equals(lastTxnType)) sb.append("Ngân hàng    : ").append(lastTxnBank).append("\nNgười nhận   : ").append(lastTxnPartner).append("\n");
        sb.append("Nội dung     : ").append(lastTxnContent == null || lastTxnContent.isEmpty() ? "-" : lastTxnContent).append("\n");
        sb.append("Số dư còn lại: ").append(MoneyFormat.formatMoney(lastTxnResult != null ? lastTxnResult.soDuMoi : 0)).append("\n");
        sb.append("============================================\n  HOTLINE: 1900 5555 88  |  VIETBANK 24/7  \n    CẢM ƠN QUÝ KHÁCH ĐÃ SỬ DỤNG VIETBANK!   \n");
        txtReceipt.setText(sb.toString());
        navigateTo(ATMState.RECEIPT_VIEW);
    }

    private void saveReceiptToFile() {
        String file = "BienLai_" + (lastTxnResult != null ? lastTxnResult.maThamChieu : "TXN") + ".txt";
        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
            fw.write(txtReceipt.getText());
            JOptionPane.showMessageDialog(this, "Đã lưu biên lai: " + file);
        } catch (Exception ex) { ATMDialogs.showErrorModal(this, ex.getMessage()); }
    }

    private void attachMoneyFormatter(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                String digits = field.getText().replaceAll("[^\\d]", "");
                if (digits.isEmpty()) { if (!field.getText().isEmpty()) field.setText(""); return; }
                try {
                    String formatted = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(Long.parseLong(digits));
                    if (!formatted.equals(field.getText())) { field.setText(formatted); field.setCaretPosition(formatted.length()); }
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    private void startCashRetractTimer() {
        stopCashRetractTimer();
        cashRetractSeconds = 15;
        lblTakeCashCountdown.setText(I18n.get("take_cash.countdown", cashRetractSeconds));
        cashRetractTimer = new Timer(1000, e -> {
            if (--cashRetractSeconds <= 0) {
                stopCashRetractTimer();
                ATMDialogs.showErrorModal(this, I18n.get("take_cash.retracted"));
                if (wantsReceipt) prepareAndShowReceipt(); else navigateTo(ATMState.ANOTHER_TXN);
            } else lblTakeCashCountdown.setText(I18n.get("take_cash.countdown", cashRetractSeconds));
        });
        cashRetractTimer.start();
    }

    private void stopCashRetractTimer() {
        if (cashRetractTimer != null) { cashRetractTimer.stop(); cashRetractTimer = null; }
    }

    private void ejectCardAndFinish() { navigateTo(ATMState.TAKE_CARD); }

    private void resetToWait() {
        stopCashRetractTimer();
        session.clearSession();
        lastTxnResult = null;
        lastTxnType = "";
        navigateTo(ATMState.WAIT);
    }

    private void handleSessionTick() {
        if (!session.isActive() || currentState == ATMState.PROCESSING || currentState == ATMState.TAKE_CARD || currentState == ATMState.TAKE_CASH) {
            lblFooterCountdown.setText("");
            return;
        }
        int left = session.tick();
        lblFooterCountdown.setText(left <= 10 && left > 8 ? I18n.get("header.session_timer", left) : "");
        if (left <= 8) {
            sessionTimer.stop();
            boolean cont = ATMDialogs.showTimeoutWarning(this, left, s -> lblFooterCountdown.setText(s > 0 ? I18n.get("header.session_timer", s) : ""));
            if (cont) resetSessionTimer(); else { session.setActive(false); ejectCardAndFinish(); }
            sessionTimer.start();
        }
    }

    private void resetSessionTimer() { session.resetTimer(); if (lblFooterCountdown != null) lblFooterCountdown.setText(""); }

    private void updateClocks() {
        Date now = new Date();
        String timeStr = new SimpleDateFormat("HH:mm:ss").format(now);
        String dateStr = new SimpleDateFormat("EEEE, dd/MM/yyyy", I18n.isEnglish() ? Locale.ENGLISH : Locale.forLanguageTag("vi-VN")).format(now);
        if (lblHeaderClock != null) lblHeaderClock.setText(timeStr);
        if (lblWaitClock != null) lblWaitClock.setText(timeStr);
        if (lblWaitDate != null) lblWaitDate.setText(dateStr);
    }

    private void applySystemErrorLabels() {
        boolean ooc = "OUT_OF_CASH".equals(sysErrMode), db = "DB_ERROR".equals(sysErrMode);
        if (!ooc) session.setActive(false);
        lblSysErrTitle.setText(I18n.get(ooc ? "sys_err.out_of_cash_title" : db ? "sys_err.db_title" : "sys_err.maintenance_title"));
        lblSysErrSub.setText("<html><div style='text-align:center;width:400px;'>" + I18n.get(ooc ? "sys_err.out_of_cash_sub" : db ? "sys_err.db_sub" : "sys_err.maintenance_sub") + "</div></html>");
        btnSysErrRetry.setVisible(!ooc);
        btnSysErrMenu.setVisible(ooc && session.isActive());
        btnSysErrEject.setVisible(ooc);
    }

    private String resolveErrorMessage(String code) {
        if (code == null) return I18n.get("err.db_error");
        return switch (code) {
            case "err.withdraw_limit_txn" -> I18n.get(code, MoneyFormat.formatMoney(CashDispenser.WITHDRAW_LIMIT_TXN));
            case "err.withdraw_limit_day" -> I18n.get(code, MoneyFormat.formatMoney(CashDispenser.WITHDRAW_LIMIT_DAY));
            case "err.max_notes_exceeded" -> I18n.get(code, CashDispenser.MAX_NOTES_PER_TXN);
            case "err.deposit_limit" -> I18n.get(code, MoneyFormat.formatMoney(DepositService.DEPOSIT_LIMIT_TXN));
            case "err.transfer_limit_txn" -> I18n.get(code, MoneyFormat.formatMoney(TransferService.TRANSFER_LIMIT_TXN));
            case "err.transfer_limit_day" -> I18n.get(code, MoneyFormat.formatMoney(TransferService.TRANSFER_LIMIT_DAY));
            default -> I18n.get(code);
        };
    }

    private void bindKey(InputMap im, ActionMap am, KeyStroke ks, String id, Runnable action) {
        im.put(ks, id);
        am.put(id, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void registerGlobalKeyBindings() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "devMode",
            () -> ATMDialogs.showDevModeDialog(this, sysService, null));
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "quitApp",
            () -> System.exit(0));
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter",
            () -> { resetSessionTimer(); if (enterAction != null) enterAction.run(); });
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc",
            () -> { resetSessionTimer(); if (escAction != null) escAction.run(); });
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "bs", () -> {
            resetSessionTimer();
            Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (owner instanceof JTextField || activeInputField == null) return;
            String t = getText(activeInputField);
            if (!t.isEmpty()) activeInputField.setText(t.substring(0, t.length() - 1));
        });
        for (int d = 0; d <= 9; d++) {
            final String digit = String.valueOf(d);
            Runnable r = () -> {
                resetSessionTimer();
                Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (owner instanceof JTextField || activeInputField == null) return;
                String cur = getText(activeInputField);
                if (!(activeInputField instanceof JPasswordField) || cur.length() < 6) activeInputField.setText(cur + digit);
            };
            bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_0 + d, 0), "num_" + d, r);
            bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + d, 0), "pad_" + d, r);
        }
    }
}
