import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class ATM extends JFrame {
    private static boolean isEnglish = false;

    private String soThe = "";
    private int accId = 0;
    private String tenChuTk = "";
    private String soTaiKhoan = "";
    private int giayConLai = DB.TIMEOUT_GIAY;

    private final CardLayout card = new CardLayout();
    private final JPanel root = new JPanel(card);
    private final Timer timer1s;
    private final Timer timerClock;
    private Timer timerProcessing;

    private JLabel lblHeaderBank;
    private JLabel lblHeaderTitle;
    private JLabel lblHeaderClock;
    private JButton btnLangToggle;

    private JLabel lblWaitClock;
    private JLabel lblWaitDate;
    private JLabel lblWaitSlogan;
    private JLabel lblWaitInstruction;
    private JButton btnInsertCard;

    private JTextField txtCardNumber;
    private JLabel lblCardError;

    private JPasswordField txtPin;
    private JLabel lblPinError;

    private JLabel lblMenuWelcome;
    private JLabel lblMenuAccName;
    private JLabel lblMenuAccNo;
    private JLabel lblMenuCardNo;
    private JLabel lblMenuHint;
    private JLabel lblMenuError;

    private JButton btnMenuWithdraw;
    private JButton btnMenuDeposit;
    private JButton btnMenuTransfer;
    private JButton btnMenuBalance;
    private JButton btnMenuPin;
    private JButton btnMenuStatement;
    private JButton btnMenuQuick;
    private JButton btnMenuExit;

    private JTextField txtWithdrawCustom;
    private JLabel lblWithdrawError;
    private JLabel lblWithdrawInstruction;

    private JTextField txtDepositCustom;
    private JLabel lblDepositError;
    private JLabel lblDepositInstruction;

    private JLabel lblProcTitle;
    private JLabel lblProcStatus;
    private JLabel lblProcNotice;
    private JProgressBar barProc;

    private JLabel lblAskTitle;
    private JLabel lblAskSub;
    private JLabel lblAskBreakdown;
    private JButton btnAskPrint;
    private JButton btnAskSkip;

    private JTextField txtTransferAcc;
    private JTextField txtTransferAmount;
    private JLabel lblReceiverName;
    private JLabel lblTransferError;
    private JLabel lblConfirmSender;
    private JLabel lblConfirmReceiver;
    private JLabel lblConfirmAmount;
    private JLabel lblConfirmFee;
    private String pendingTransferStk = "";
    private long pendingTransferAmount = 0;

    private JPasswordField txtOldPin;
    private JPasswordField txtNewPin;
    private JPasswordField txtConfirmPin;
    private JLabel lblChangePinError;

    private JTable tblStatement;
    private DefaultTableModel modelStatement;
    private JLabel lblSumInfo;
    private JTextArea txtReceipt;

    private JLabel lblBalanceAccName;
    private JLabel lblBalanceAccNo;
    private JLabel lblBalanceCardNo;
    private JLabel lblBalanceAvailable;

    private JLabel lblCountdown;
    private JLabel lblDbStatus;
    private boolean isDbConnected = false;

    private JTextField focusedField = null;

    private String lastTxnType = "";
    private long lastTxnAmount = 0;
    private long lastTxnBalance = 0;
    private String lastTxnPartner = "-";
    private String lastTxnTime = "";
    private String lastTxnId = "";
    private DB.CashBreakdown lastBreakdown = null;

    private final Color COLOR_PRIMARY = new Color(224, 86, 45);
    private final Color COLOR_SECONDARY = new Color(194, 65, 30);
    private final Color COLOR_ACCENT = new Color(255, 237, 213);
    private final Color COLOR_BG_DARK = new Color(24, 24, 27);
    private final Color COLOR_BG_LIGHT = new Color(250, 248, 245);
    private final Color COLOR_CARD_WHITE = Color.WHITE;
    private final Color COLOR_TEXT_MAIN = new Color(24, 24, 27);
    private final Color COLOR_TEXT_MUTED = new Color(113, 113, 122);
    private final Color COLOR_BADGE_BG = new Color(255, 247, 237);
    private final Color COLOR_BADGE_BORDER = new Color(254, 215, 170);
    private final Color COLOR_DANGER = new Color(220, 38, 38);
    private final Color COLOR_WARNING = new Color(217, 119, 6);
    private final Color COLOR_SUCCESS = new Color(16, 185, 129);
    private final Color COLOR_BORDER_DEFAULT = new Color(212, 212, 216);
    private final Color COLOR_BORDER_FOCUS = new Color(224, 86, 45);
    private final Color COLOR_KEY_BG = new Color(255, 247, 237);

    private final Border borderInputDefault = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(212, 212, 216), 1, true),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
    );
    private final Border borderInputFocus = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(224, 86, 45), 2, true),
            BorderFactory.createEmptyBorder(5, 11, 5, 11)
    );
    private final Border borderInputError = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 38, 38), 2, true),
            BorderFactory.createEmptyBorder(5, 11, 5, 11)
    );

    private void datActiveField(JTextField f) {
        if (focusedField != null) {
            focusedField.setBorder(borderInputDefault);
            focusedField.setBackground(Color.WHITE);
        }
        focusedField = f;
        if (focusedField != null) {
            focusedField.setBorder(borderInputFocus);
            focusedField.setBackground(COLOR_BADGE_BG);
            focusedField.requestFocusInWindow();
            SwingUtilities.invokeLater(() -> {
                if (focusedField != null) {
                    int len = focusedField.getText() != null ? focusedField.getText().length() : 0;
                    try {
                        focusedField.setCaretPosition(len);
                    } catch (Exception ignored) {}
                }
            });
        }
    }

    private void ganFocusHighlight(JTextField field) {
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                datActiveField(field);
            }
        });
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                datActiveField(field);
            }
        });
    }

    public static String t(String vi, String en) {
        return isEnglish ? en : vi;
    }

    public static String boDau(String str) {
        if (str == null) return "";
        String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replace('Đ', 'D').replace('đ', 'd');
    }

    public ATM() {
        setTitle("VIETBANK ATM");
        setUndecorated(true);
        setResizable(false);
        setSize(980, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        timer1s = new Timer(1000, e -> {
            giayConLai--;
            capNhatDemNguoc();
            if (giayConLai <= 0) {
                xuLyTimeout();
            }
        });

        timerClock = new Timer(1000, e -> {
            SimpleDateFormat sdfClock = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat sdfDate = new SimpleDateFormat("EEEE, dd/MM/yyyy", isEnglish ? Locale.US : Locale.forLanguageTag("vi-VN"));
            Date now = new Date();
            if (lblHeaderClock != null) lblHeaderClock.setText(sdfClock.format(now));
            if (lblWaitClock != null) lblWaitClock.setText(sdfClock.format(now));
            if (lblWaitDate != null) lblWaitDate.setText(sdfDate.format(now));
        });
        timerClock.start();

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (timer1s.isRunning()) {
                giayConLai = DB.TIMEOUT_GIAY;
                capNhatDemNguoc();
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

        getRootPane().registerKeyboardAction(e -> System.exit(0),
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        initViews();
        setLayout(new BorderLayout());
        add(taoHeader(), BorderLayout.NORTH);
        add(root, BorderLayout.CENTER);
        add(taoFooter(), BorderLayout.SOUTH);

        hienThi("WAIT", t("SẴN SÀNG PHỤC VỤ", "READY TO SERVE"));
        capNhatTrangThaiCSDL();
    }

    private void capNhatDemNguoc() {
        if (lblCountdown == null) return;
        if (giayConLai <= 10) {
            lblCountdown.setForeground(COLOR_DANGER);
            lblCountdown.setText(String.format(t("Phiên còn lại: %02ds (Nhấp để gia hạn)", "Session expires: %02ds (Click to extend)"), giayConLai));
        } else {
            lblCountdown.setForeground(COLOR_TEXT_MAIN);
            lblCountdown.setText(String.format(t("Phiên còn lại: %02ds", "Session remaining: %02ds"), giayConLai));
        }
    }

    private void toggleNgonNgu() {
        isEnglish = !isEnglish;
        btnLangToggle.setText(isEnglish ? "EN | VI" : "VI | EN");
        lblHeaderBank.setText("VIETBANK | DIGITAL BANKING");
        lblWaitSlogan.setText(t("NGÂN HÀNG THƯƠNG MẠI CỔ PHẦN HÀNG ĐẦU VIỆT NAM", "VIETNAM'S LEADING COMMERCIAL BANK"));
        lblWaitInstruction.setText(t("Vui lòng đưa thẻ ngân hàng vào khe đọc để bắt đầu", "Please insert your ATM card to start transaction"));
        btnInsertCard.setText(t("ĐƯA THẺ VÀO ĐÂY / INSERT CARD", "INSERT CARD / ĐƯA THẺ VÀO ĐÂY"));

        btnMenuWithdraw.setText(t("<html><div style='text-align:left;'>‹ <b>RÚT TIỀN MẶT</b><br><small style='color:#71717A;'>Rút tiền mặt tức thì</small></div></html>", "<html><div style='text-align:left;'>‹ <b>CASH WITHDRAWAL</b><br><small style='color:#71717A;'>Instant cash dispenser</small></div></html>"));
        btnMenuDeposit.setText(t("<html><div style='text-align:left;'>‹ <b>NẠP TIỀN MẶT</b><br><small style='color:#71717A;'>Nạp tiền vào tài khoản</small></div></html>", "<html><div style='text-align:left;'>‹ <b>CASH DEPOSIT</b><br><small style='color:#71717A;'>Deposit cash to account</small></div></html>"));
        btnMenuTransfer.setText(t("<html><div style='text-align:left;'>‹ <b>CHUYỂN KHOẢN</b><br><small style='color:#71717A;'>Chuyển tiền nhanh 24/7</small></div></html>", "<html><div style='text-align:left;'>‹ <b>FUND TRANSFER</b><br><small style='color:#71717A;'>Instant 24/7 transfer</small></div></html>"));
        btnMenuBalance.setText(t("<html><div style='text-align:left;'>‹ <b>XEM SỐ DƯ</b><br><small style='color:#71717A;'>Tra cứu số dư khả dụng</small></div></html>", "<html><div style='text-align:left;'>‹ <b>BALANCE INQUIRY</b><br><small style='color:#71717A;'>View available funds</small></div></html>"));
        btnMenuPin.setText(t("<html><div style='text-align:right;'><b>ĐỔI MÃ PIN</b> ›<br><small style='color:#71717A;'>Cập nhật mã bảo vệ</small></div></html>", "<html><div style='text-align:right;'><b>CHANGE PIN</b> ›<br><small style='color:#71717A;'>Update security PIN</small></div></html>"));
        btnMenuStatement.setText(t("<html><div style='text-align:right;'><b>SAO KÊ MINI</b> ›<br><small style='color:#71717A;'>10 giao dịch gần nhất</small></div></html>", "<html><div style='text-align:right;'><b>MINI STATEMENT</b> ›<br><small style='color:#71717A;'>Last 10 transactions</small></div></html>"));
        btnMenuQuick.setText(t("<html><div style='text-align:right;'><b>RÚT NHANH 500K</b> ›<br><small style='color:#E0562D;'>Rút nhanh 1 chạm</small></div></html>", "<html><div style='text-align:right;'><b>QUICK CASH 500K</b> ›<br><small style='color:#E0562D;'>1-touch quick cash</small></div></html>"));
        btnMenuExit.setText(t("<html><div style='text-align:right;'><b>ĐĂNG XUẤT / TRẢ THẺ</b> ›<br><small style='color:#DC2626;'>Hoàn tất phiên giao dịch</small></div></html>", "<html><div style='text-align:right;'><b>EXIT & EJECT CARD</b> ›<br><small style='color:#DC2626;'>Complete & take card</small></div></html>"));
        lblMenuWelcome.setText(t("KÍNH CHÀO QUÝ KHÁCH", "WELCOME VALUED CUSTOMER"));
        lblMenuHint.setText(t("Quý khách vui lòng chọn dịch vụ cần thực hiện", "Please select a service to proceed"));
        if (!soTaiKhoan.isEmpty()) {
            lblMenuAccNo.setText(t("Số tài khoản: ", "Account No: ") + soTaiKhoan);
        }

        capNhatNhanDb();
        capNhatDemNguoc();
        repaint();
    }

    private void hienThi(String cardName, String tieuDe) {
        lblHeaderTitle.setText(tieuDe);
        card.show(root, cardName);
        if ("WAIT".equals(cardName)) {
            if (lblHeaderClock != null) lblHeaderClock.setVisible(false);
            timer1s.stop();
            lblCountdown.setText("");
        } else {
            if (lblHeaderClock != null) lblHeaderClock.setVisible(true);
            giayConLai = DB.TIMEOUT_GIAY;
            capNhatDemNguoc();
            if (!timer1s.isRunning()) timer1s.start();
        }

        if ("CARD".equals(cardName) && txtCardNumber != null) datActiveField(txtCardNumber);
        else if ("PIN".equals(cardName) && txtPin != null) datActiveField(txtPin);
        else if ("WITHDRAW".equals(cardName) && txtWithdrawCustom != null) datActiveField(txtWithdrawCustom);
        else if ("DEPOSIT".equals(cardName) && txtDepositCustom != null) datActiveField(txtDepositCustom);
        else if ("TRANSFER".equals(cardName) && txtTransferAcc != null) datActiveField(txtTransferAcc);
        else if ("CHANGE_PIN".equals(cardName) && txtOldPin != null) datActiveField(txtOldPin);
    }

    private JButton btn(String text, Color bg, Color fg, int matteSide) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    if (Color.WHITE.equals(bg)) {
                        g2.setColor(new Color(255, 247, 237));
                    } else {
                        g2.setColor(bg.brighter());
                    }
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                if (Color.WHITE.equals(bg)) {
                    g2.setColor(new Color(228, 228, 231));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                } else if (COLOR_BADGE_BG.equals(bg) || COLOR_KEY_BG.equals(bg)) {
                    g2.setColor(new Color(253, 186, 116));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 15));
        b.setForeground(fg);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (matteSide == 1) {
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 6, 0, 0, COLOR_PRIMARY),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
            ));
        } else if (matteSide == 2) {
            Color borderC = (COLOR_DANGER.equals(fg) || COLOR_DANGER.equals(bg)) ? COLOR_DANGER : COLOR_PRIMARY;
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 6, borderC),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
            ));
        } else {
            b.setBorderPainted(false);
        }
        return b;
    }

    private JPanel taoLogo() {
        return new JPanel() {
            {
                setPreferredSize(new Dimension(50, 36));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_BADGE_BG);
                g2.fillRoundRect(2, 2, 46, 32, 6, 6);
                g2.setColor(COLOR_PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int x = (50 - fm.stringWidth("ATM")) / 2;
                int y = ((36 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString("ATM", x, y);
                g2.dispose();
            }
        };
    }

    private JPanel taoHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_PRIMARY);
        header.setPreferredSize(new Dimension(980, 62));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, COLOR_BADGE_BORDER),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));

        JPanel pLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        pLeft.setOpaque(false);
        pLeft.add(taoLogo());

        lblHeaderBank = new JLabel("VIETBANK | DIGITAL BANKING");
        lblHeaderBank.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblHeaderBank.setForeground(Color.WHITE);
        pLeft.add(lblHeaderBank);

        JPanel pRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        pRight.setOpaque(false);

        lblHeaderTitle = new JLabel(t("SẴN SÀNG PHỤC VỤ", "READY TO SERVE"));
        lblHeaderTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblHeaderTitle.setForeground(Color.WHITE);
        pRight.add(lblHeaderTitle);

        JLabel lblSep1 = new JLabel("  |  ");
        lblSep1.setForeground(COLOR_BADGE_BORDER);
        pRight.add(lblSep1);

        lblHeaderClock = new JLabel("00:00:00");
        lblHeaderClock.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblHeaderClock.setForeground(COLOR_BADGE_BORDER);
        pRight.add(lblHeaderClock);

        JLabel lblSep2 = new JLabel("  |  ");
        lblSep2.setForeground(COLOR_BADGE_BORDER);
        pRight.add(lblSep2);

        btnLangToggle = btn("VI | EN", COLOR_SECONDARY, Color.WHITE, 0);
        btnLangToggle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLangToggle.setPreferredSize(new Dimension(68, 30));
        btnLangToggle.addActionListener(e -> toggleNgonNgu());
        pRight.add(btnLangToggle);

        header.add(pLeft, BorderLayout.WEST);
        header.add(pRight, BorderLayout.EAST);
        return header;
    }

    private JPanel taoFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BADGE_BORDER));
        footer.setPreferredSize(new Dimension(980, 38));

        JPanel fLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        fLeft.setOpaque(false);
        lblDbStatus = new JLabel("● CSDL: Đang kiểm tra...");
        lblDbStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDbStatus.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblDbStatus.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                hienThiDialogCauHinhDB();
            }
        });
        fLeft.add(lblDbStatus);

        JPanel fRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 8));
        fRight.setOpaque(false);
        lblCountdown = new JLabel("");
        lblCountdown.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCountdown.setForeground(COLOR_TEXT_MAIN);
        lblCountdown.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblCountdown.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                giayConLai = DB.TIMEOUT_GIAY;
                capNhatDemNguoc();
            }
        });

        JLabel lblHotline = new JLabel("Hotline 24/7: 1900 6868  |  PCI-DSS v4.0");
        lblHotline.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHotline.setForeground(COLOR_TEXT_MUTED);

        fRight.add(lblCountdown);
        JLabel lblSepF = new JLabel("  |  ");
        lblSepF.setForeground(COLOR_BADGE_BORDER);
        fRight.add(lblSepF);
        fRight.add(lblHotline);

        footer.add(fLeft, BorderLayout.WEST);
        footer.add(fRight, BorderLayout.EAST);
        return footer;
    }

    private void capNhatTrangThaiCSDL() {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return DB.kiemTraKetNoi();
            }

            @Override
            protected void done() {
                try {
                    isDbConnected = get();
                } catch (Exception ex) {
                    isDbConnected = false;
                }
                capNhatNhanDb();
            }
        }.execute();
    }

    private void capNhatNhanDb() {
        if (lblDbStatus == null) return;
        if (isDbConnected) {
            lblDbStatus.setText(t("● CSDL: Sẵn sàng", "● DB: Ready"));
            lblDbStatus.setForeground(COLOR_SUCCESS);
        } else {
            lblDbStatus.setText(t("● CSDL: Chưa kết nối (Nhấp để cấu hình)", "● DB: Disconnected (Click to configure)"));
            lblDbStatus.setForeground(COLOR_DANGER);
        }
    }

    private void initViews() {
        root.add(taoManHinhCho(), "WAIT");
        root.add(taoManHinhNhapThe(), "CARD");
        root.add(taoManHinhNhapPin(), "PIN");
        root.add(taoManHinhMenu(), "MENU");
        root.add(taoManHinhXemSoDu(), "BALANCE");
        root.add(taoManHinhRutTien(), "WITHDRAW");
        root.add(taoManHinhNapTien(), "DEPOSIT");
        root.add(taoManHinhXuLy(), "PROCESSING");
        root.add(taoManHinhHoiInBienLai(), "ASK_RECEIPT");
        root.add(taoManHinhChuyenKhoan(), "TRANSFER");
        root.add(taoManHinhXacNhanChuyenKhoan(), "CONFIRM_TRANSFER");
        root.add(taoManHinhDoiPin(), "CHANGE_PIN");
        root.add(taoManHinhSaoKe(), "STATEMENT");
        root.add(taoManHinhBienLai(), "RECEIPT");
    }

    private JPanel taoManHinhCho() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(COLOR_BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.gridx = 0;

        lblWaitClock = new JLabel("00:00:00", SwingConstants.CENTER);
        lblWaitClock.setFont(new Font("Segoe UI", Font.BOLD, 50));
        lblWaitClock.setForeground(COLOR_PRIMARY);

        lblWaitDate = new JLabel("Thứ Năm, 19/11/2026", SwingConstants.CENTER);
        lblWaitDate.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblWaitDate.setForeground(COLOR_ACCENT);

        JPanel pLogoBig = new JPanel() {
            {
                setPreferredSize(new Dimension(130, 80));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PRIMARY);
                g2.fillRoundRect(4, 4, 122, 72, 14, 14);
                g2.setColor(COLOR_ACCENT);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(4, 4, 122, 72, 14, 14);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 32));
                FontMetrics fm = g2.getFontMetrics();
                int x = (130 - fm.stringWidth("ATM")) / 2;
                int y = ((80 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString("ATM", x, y);
                g2.dispose();
            }
        };

        lblWaitSlogan = new JLabel("NGÂN HÀNG THƯƠNG MẠI CỔ PHẦN HÀNG ĐẦU VIỆT NAM", SwingConstants.CENTER);
        lblWaitSlogan.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblWaitSlogan.setForeground(Color.WHITE);

        lblWaitInstruction = new JLabel("Vui lòng đưa thẻ ngân hàng vào khe đọc để bắt đầu", SwingConstants.CENTER);
        lblWaitInstruction.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblWaitInstruction.setForeground(COLOR_ACCENT);

        btnInsertCard = btn("ĐƯA THẺ VÀO ĐÂY / INSERT CARD", COLOR_PRIMARY, Color.WHITE, 0);
        btnInsertCard.setFont(new Font("Segoe UI", Font.BOLD, 20));
        btnInsertCard.setPreferredSize(new Dimension(420, 64));
        btnInsertCard.addActionListener(e -> {
            txtCardNumber.setText("");
            lblCardError.setText(" ");
            hienThi("CARD", t("XÁC THỰC THẺ", "CARD AUTHENTICATION"));
            txtCardNumber.requestFocus();
        });

        gbc.gridy = 0; p.add(lblWaitClock, gbc);
        gbc.gridy = 1; p.add(lblWaitDate, gbc);
        gbc.gridy = 2; p.add(pLogoBig, gbc);
        gbc.gridy = 3; p.add(lblWaitSlogan, gbc);
        gbc.gridy = 4; p.add(lblWaitInstruction, gbc);
        gbc.gridy = 5; p.add(btnInsertCard, gbc);

        return p;
    }

    private JPanel taoManHinhNhapThe() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(COLOR_BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel pCard = new JPanel(new GridBagLayout());
        pCard.setBackground(COLOR_CARD_WHITE);
        pCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DEFAULT, 1, true),
                BorderFactory.createEmptyBorder(25, 35, 25, 35)
        ));
        GridBagConstraints cgbc = new GridBagConstraints();
        cgbc.insets = new Insets(8, 10, 8, 10);
        cgbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblInstruction = new JLabel(t("NHẬP SỐ THẺ NGÂN HÀNG (16 CHỮ SỐ):", "ENTER 16-DIGIT CARD NUMBER:"), SwingConstants.CENTER);
        lblInstruction.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblInstruction.setForeground(COLOR_PRIMARY);

        txtCardNumber = new JTextField(19);
        txtCardNumber.setFont(new Font("Segoe UI", Font.BOLD, 24));
        txtCardNumber.setHorizontalAlignment(JTextField.CENTER);
        txtCardNumber.setPreferredSize(new Dimension(420, 50));
        txtCardNumber.setBorder(borderInputDefault);
        txtCardNumber.addActionListener(e -> xuLyNhapThe());
        ganFocusHighlight(txtCardNumber);

        ((AbstractDocument) txtCardNumber.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                formatAndSet(fb, fb.getDocument().getText(0, fb.getDocument().getLength()) + string);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String before = cur.substring(0, offset);
                String after = cur.substring(offset + length);
                formatAndSet(fb, before + (text == null ? "" : text) + after);
            }

            private void formatAndSet(FilterBypass fb, String raw) throws BadLocationException {
                String d = raw.replaceAll("\\D", "");
                if (d.length() > 16) d = d.substring(0, 16);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < d.length(); i++) {
                    if (i > 0 && i % 4 == 0) sb.append(' ');
                    sb.append(d.charAt(i));
                }
                fb.replace(0, fb.getDocument().getLength(), sb.toString(), null);
                SwingUtilities.invokeLater(() -> {
                    if (txtCardNumber != null) txtCardNumber.setCaretPosition(txtCardNumber.getText().length());
                });
            }
        });

        JPanel pSample = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        pSample.setOpaque(false);
        JLabel lblQuick = new JLabel(t("Thẻ mẫu kiểm thử:", "Sample cards:"));
        lblQuick.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        pSample.add(lblQuick);

        String[][] cards = {
                {t("Thẻ 1 (50tr)", "Card 1 (50M)"), "9704 0012 3456 7890"},
                {t("Thẻ 2 (1tr)", "Card 2 (1M)"), "9704 0098 7654 3210"},
                {t("Thẻ 3 (Đã khóa)", "Card 3 (Locked)"), "9704 0011 1122 2233"}
        };
        for (String[] c : cards) {
            JButton b = btn(c[0], COLOR_KEY_BG, COLOR_PRIMARY, 0);
            b.setFont(new Font("Segoe UI", Font.BOLD, 13));
            b.setPreferredSize(new Dimension(170, 36));
            b.addActionListener(e -> {
                txtCardNumber.setText(c[1]);
                txtCardNumber.setCaretPosition(c[1].length());
                txtCardNumber.setBorder(borderInputFocus);
            });
            pSample.add(b);
        }

        lblCardError = new JLabel(" ", SwingConstants.CENTER);
        lblCardError.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCardError.setForeground(COLOR_DANGER);

        JPanel pAction = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        pAction.setOpaque(false);

        JButton btnNext = btn(t("TIẾP TỤC", "CONTINUE"), COLOR_PRIMARY, Color.WHITE, 0);
        btnNext.setPreferredSize(new Dimension(170, 46));
        JButton btnCancel = btn(t("HỦY BỎ", "CANCEL"), COLOR_DANGER, Color.WHITE, 0);
        btnCancel.setPreferredSize(new Dimension(140, 46));

        btnNext.addActionListener(e -> xuLyNhapThe());
        btnCancel.addActionListener(e -> dangXuat());

        pAction.add(btnNext);
        pAction.add(btnCancel);

        cgbc.gridx = 0; cgbc.gridy = 0; pCard.add(lblInstruction, cgbc);
        cgbc.gridy = 1; pCard.add(txtCardNumber, cgbc);
        cgbc.gridy = 2; pCard.add(pSample, cgbc);
        cgbc.gridy = 3; pCard.add(lblCardError, cgbc);
        cgbc.gridy = 4; pCard.add(pAction, cgbc);

        gbc.gridx = 0; gbc.gridy = 0; center.add(pCard, gbc);

        return center;
    }

    private void xuLyNhapThe() {
        lblCardError.setText(" ");
        txtCardNumber.setBorder(borderInputDefault);
        String inputThe = txtCardNumber.getText().replaceAll("\\s+", "").trim();
        if (inputThe.isEmpty()) {
            lblCardError.setText(t("Vui lòng nhập hoặc bấm chọn một thẻ mẫu!", "Please enter or pick a sample card!"));
            txtCardNumber.setBorder(borderInputError);
            return;
        }

        String trangThai = DB.kiemTraThe(inputThe);
        if (trangThai.startsWith("DB_ERROR:")) {
            lblCardError.setText(t("Lỗi kết nối CSDL! Vui lòng nhấp 'CSDL' góc dưới để kiểm tra.", "Database error! Please click 'CSDL' to configure."));
            txtCardNumber.setBorder(borderInputError);
            capNhatTrangThaiCSDL();
            return;
        }
        if ("NOT_FOUND".equals(trangThai)) {
            lblCardError.setText(t("Số thẻ không tồn tại trong hệ thống VietBank!", "Card number not found in VietBank system!"));
            txtCardNumber.setBorder(borderInputError);
            return;
        }
        if ("LOCKED".equalsIgnoreCase(trangThai)) {
            lblCardError.setText(t("Thẻ đã bị tạm khóa do nhập sai PIN 3 lần!", "Card has been locked due to 3 failed PIN attempts!"));
            txtCardNumber.setBorder(borderInputError);
            return;
        }

        this.soThe = inputThe;
        txtPin.setText("");
        lblPinError.setText(t("<html><div style='text-align:center;'>Vui lòng nhập mã PIN bảo mật gồm 6 chữ số</div></html>", "<html><div style='text-align:center;'>Please enter your 6-digit security PIN</div></html>"));
        lblPinError.setForeground(COLOR_TEXT_MUTED);
        txtPin.setBorder(borderInputDefault);
        hienThi("PIN", t("ĐĂNG NHẬP MÃ PIN", "PIN AUTHENTICATION"));
        txtPin.requestFocus();
    }

    private JPanel taoManHinhNhapPin() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(COLOR_BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);

        JPanel pLeft = new JPanel(new GridLayout(4, 1, 8, 8));
        pLeft.setBackground(COLOR_CARD_WHITE);
        pLeft.setPreferredSize(new Dimension(360, 320));
        pLeft.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BADGE_BORDER, 1, true),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        JLabel lblTitle = new JLabel(t("XÁC THỰC MÃ PIN", "PIN AUTHENTICATION"), SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(COLOR_PRIMARY);

        lblPinError = new JLabel(t("<html><div style='text-align:center;'>Vui lòng nhập mã PIN bảo mật gồm 6 chữ số</div></html>", "<html><div style='text-align:center;'>Please enter your 6-digit security PIN</div></html>"), SwingConstants.CENTER);
        lblPinError.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPinError.setForeground(COLOR_TEXT_MUTED);

        txtPin = new JPasswordField(6);
        txtPin.setFont(new Font("Segoe UI", Font.BOLD, 30));
        txtPin.setHorizontalAlignment(JTextField.CENTER);
        txtPin.setPreferredSize(new Dimension(300, 52));
        txtPin.setBorder(borderInputDefault);
        txtPin.addActionListener(e -> xuLyDangNhap());
        ganDigitFilter(txtPin, 6);
        ganFocusHighlight(txtPin);

        JLabel lblPinNotice = new JLabel(t("Lưu ý: Vui lòng che tay khi nhập mã PIN", "Notice: Please cover keypad when entering PIN"), SwingConstants.CENTER);
        lblPinNotice.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblPinNotice.setForeground(COLOR_TEXT_MUTED);

        pLeft.add(lblTitle);
        pLeft.add(lblPinError);
        pLeft.add(txtPin);
        pLeft.add(lblPinNotice);

        JPanel pKeypad = taoNumpad(txtPin, this::xuLyDangNhap, this::dangXuat);

        gbc.gridx = 0; gbc.gridy = 0; center.add(pLeft, gbc);
        gbc.gridx = 1; center.add(pKeypad, gbc);

        return center;
    }

    private void xuLyDangNhap() {
        lblPinError.setText(" ");
        txtPin.setBorder(borderInputFocus);
        String pinStr = new String(txtPin.getPassword()).trim();
        if (pinStr.isEmpty()) {
            lblPinError.setText(t("<html><div style='text-align:center;'>Vui lòng nhập mã PIN!</div></html>", "<html><div style='text-align:center;'>Please enter your PIN!</div></html>"));
            lblPinError.setForeground(COLOR_DANGER);
            txtPin.setBorder(borderInputError);
            return;
        }

        DB.UserSession s = DB.dangNhap(soThe, pinStr);
        if (s == null) {
            lblPinError.setText(t("<html><div style='text-align:center;'>Lỗi kết nối CSDL!</div></html>", "<html><div style='text-align:center;'>Database connection error!</div></html>"));
            lblPinError.setForeground(COLOR_DANGER);
            txtPin.setBorder(borderInputError);
            return;
        }

        if (s.success) {
            this.accId = s.accId;
            this.tenChuTk = s.tenChuTk;
            this.soTaiKhoan = s.soTaiKhoan;

            lblMenuAccName.setText(tenChuTk);
            lblMenuAccNo.setText(t("Số tài khoản: ", "Account No: ") + soTaiKhoan);
            lblMenuCardNo.setText(t("Số thẻ: ", "Card No: ") + dinhDangMaskThe(soThe));
            lblMenuError.setText(" ");

            hienThi("MENU", t("MENU CHÍNH", "MAIN MENU"));
        } else if (s.locked) {
            lblPinError.setText(t("<html><div style='text-align:center;'>Mã PIN sai! Thẻ đã bị khóa do sai quá 3 lần.</div></html>", "<html><div style='text-align:center;'>Incorrect PIN! Card locked due to 3 failed attempts.</div></html>"));
            lblPinError.setForeground(COLOR_DANGER);
            txtPin.setText("");
            txtPin.setBorder(borderInputError);
            Timer tReturn = new Timer(2500, e -> dangXuat());
            tReturn.setRepeats(false);
            tReturn.start();
        } else {
            lblPinError.setText(String.format(t("<html><div style='text-align:center;'>Mã PIN không đúng!<br>Quý khách còn <b>%d</b> lần thử.</div></html>", "<html><div style='text-align:center;'>Incorrect PIN!<br>You have <b>%d</b> attempt(s) remaining.</div></html>"), s.remainingAttempts));
            lblPinError.setForeground(COLOR_DANGER);
            txtPin.setText("");
            txtPin.setBorder(borderInputError);
            txtPin.requestFocus();
        }
    }

    private JPanel taoManHinhMenu() {
        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBackground(COLOR_BG_LIGHT);
        main.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JPanel pCenter = new JPanel(new GridBagLayout());
        pCenter.setBackground(COLOR_CARD_WHITE);
        pCenter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DEFAULT, 1, true),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JPanel pCardSim = new JPanel() {
            {
                setPreferredSize(new Dimension(280, 120));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, COLOR_PRIMARY, 280, 120, COLOR_SECONDARY);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, 280, 120, 12, 12);
                g2.setColor(new Color(230, 180, 60));
                g2.fillRoundRect(18, 18, 34, 24, 4, 4);
                g2.setColor(new Color(190, 140, 20));
                g2.drawRoundRect(18, 18, 34, 24, 4, 4);
                g2.drawLine(18, 30, 52, 30);
                g2.drawLine(35, 18, 35, 42);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString("VIETBANK PLATINUM", 130, 34);
                g2.setFont(new Font("Consolas", Font.BOLD, 15));
                g2.drawString(dinhDangMaskThe(soThe.isEmpty() ? "9704001234567890" : soThe), 18, 75);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.drawString(tenChuTk.isEmpty() ? "NGUYEN VAN AN" : boDau(tenChuTk).toUpperCase(), 18, 102);
                g2.setColor(COLOR_ACCENT);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.drawString("NAPAS EMV", 200, 102);
                g2.dispose();
            }
        };

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(3, 3, 3, 3);

        lblMenuWelcome = new JLabel(t("KÍNH CHÀO QUÝ KHÁCH", "WELCOME VALUED CUSTOMER"), SwingConstants.CENTER);
        lblMenuWelcome.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblMenuWelcome.setForeground(COLOR_PRIMARY);

        lblMenuAccName = new JLabel("NGUYỄN VĂN AN", SwingConstants.CENTER);
        lblMenuAccName.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblMenuAccName.setForeground(COLOR_TEXT_MAIN);

        lblMenuAccNo = new JLabel("Số tài khoản: 1234567890", SwingConstants.CENTER);
        lblMenuAccNo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblMenuAccNo.setForeground(COLOR_TEXT_MUTED);

        lblMenuHint = new JLabel(t("Quý khách vui lòng chọn dịch vụ cần thực hiện", "Please select a service to proceed"), SwingConstants.CENTER);
        lblMenuHint.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblMenuHint.setForeground(COLOR_PRIMARY);

        lblMenuError = new JLabel(" ", SwingConstants.CENTER);
        lblMenuError.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblMenuError.setForeground(COLOR_DANGER);

        gbc.gridy = 0; pCenter.add(pCardSim, gbc);
        gbc.gridy = 1; pCenter.add(Box.createVerticalStrut(6), gbc);
        gbc.gridy = 2; pCenter.add(lblMenuWelcome, gbc);
        gbc.gridy = 3; pCenter.add(lblMenuAccName, gbc);
        gbc.gridy = 4; pCenter.add(lblMenuAccNo, gbc);
        gbc.gridy = 5; pCenter.add(Box.createVerticalStrut(6), gbc);
        gbc.gridy = 6; pCenter.add(lblMenuHint, gbc);
        gbc.gridy = 7; pCenter.add(lblMenuError, gbc);

        JPanel pLeft = new JPanel(new GridLayout(4, 1, 0, 16));
        pLeft.setBackground(COLOR_BG_LIGHT);
        pLeft.setPreferredSize(new Dimension(270, 360));

        btnMenuWithdraw = btn(t("<html><div style='text-align:left;'>‹ <b>RÚT TIỀN MẶT</b><br><small style='color:#71717A;'>Rút tiền mặt tức thì</small></div></html>", "<html><div style='text-align:left;'>‹ <b>CASH WITHDRAWAL</b><br><small style='color:#71717A;'>Instant cash dispenser</small></div></html>"), COLOR_CARD_WHITE, COLOR_TEXT_MAIN, 1);
        btnMenuWithdraw.setHorizontalAlignment(SwingConstants.LEFT);
        btnMenuDeposit = btn(t("<html><div style='text-align:left;'>‹ <b>NẠP TIỀN MẶT</b><br><small style='color:#71717A;'>Nạp tiền vào tài khoản</small></div></html>", "<html><div style='text-align:left;'>‹ <b>CASH DEPOSIT</b><br><small style='color:#71717A;'>Deposit cash to account</small></div></html>"), COLOR_CARD_WHITE, COLOR_TEXT_MAIN, 1);
        btnMenuDeposit.setHorizontalAlignment(SwingConstants.LEFT);
        btnMenuTransfer = btn(t("<html><div style='text-align:left;'>‹ <b>CHUYỂN KHOẢN</b><br><small style='color:#71717A;'>Chuyển tiền nhanh 24/7</small></div></html>", "<html><div style='text-align:left;'>‹ <b>FUND TRANSFER</b><br><small style='color:#71717A;'>Instant 24/7 transfer</small></div></html>"), COLOR_CARD_WHITE, COLOR_TEXT_MAIN, 1);
        btnMenuTransfer.setHorizontalAlignment(SwingConstants.LEFT);
        btnMenuBalance = btn(t("<html><div style='text-align:left;'>‹ <b>XEM SỐ DƯ</b><br><small style='color:#71717A;'>Tra cứu số dư khả dụng</small></div></html>", "<html><div style='text-align:left;'>‹ <b>BALANCE INQUIRY</b><br><small style='color:#71717A;'>View available funds</small></div></html>"), COLOR_CARD_WHITE, COLOR_TEXT_MAIN, 1);
        btnMenuBalance.setHorizontalAlignment(SwingConstants.LEFT);

        btnMenuWithdraw.addActionListener(e -> {
            lblWithdrawError.setText(" ");
            txtWithdrawCustom.setText("");
            hienThi("WITHDRAW", t("RÚT TIỀN MẶT", "CASH WITHDRAWAL"));
        });
        btnMenuDeposit.addActionListener(e -> {
            lblDepositError.setText(" ");
            txtDepositCustom.setText("");
            hienThi("DEPOSIT", t("NẠP TIỀN MẶT", "CASH DEPOSIT"));
        });
        btnMenuTransfer.addActionListener(e -> {
            lblTransferError.setText(" ");
            txtTransferAcc.setText("");
            txtTransferAmount.setText("");
            lblReceiverName.setText(t("Người nhận: (Chưa xác định)", "Beneficiary: (Unchecked)"));
            lblReceiverName.setForeground(COLOR_TEXT_MUTED);
            hienThi("TRANSFER", t("CHUYỂN KHOẢN", "FUND TRANSFER"));
        });
        btnMenuBalance.addActionListener(e -> {
            lblBalanceAccName.setText(tenChuTk);
            lblBalanceAccNo.setText(soTaiKhoan);
            lblBalanceCardNo.setText(dinhDangMaskThe(soThe));
            long du = DB.laySoDu(accId);
            lblBalanceAvailable.setText(DB.dinhDangTien(du));
            hienThi("BALANCE", t("XEM SỐ DƯ TÀI KHOẢN", "BALANCE INQUIRY"));
        });

        pLeft.add(btnMenuWithdraw);
        pLeft.add(btnMenuDeposit);
        pLeft.add(btnMenuTransfer);
        pLeft.add(btnMenuBalance);

        JPanel pRight = new JPanel(new GridLayout(4, 1, 0, 16));
        pRight.setBackground(COLOR_BG_LIGHT);
        pRight.setPreferredSize(new Dimension(270, 360));

        btnMenuPin = btn(t("<html><div style='text-align:right;'><b>ĐỔI MÃ PIN</b> ›<br><small style='color:#71717A;'>Cập nhật mã bảo vệ</small></div></html>", "<html><div style='text-align:right;'><b>CHANGE PIN</b> ›<br><small style='color:#71717A;'>Update security PIN</small></div></html>"), COLOR_CARD_WHITE, COLOR_TEXT_MAIN, 2);
        btnMenuPin.setHorizontalAlignment(SwingConstants.RIGHT);
        btnMenuStatement = btn(t("<html><div style='text-align:right;'><b>SAO KÊ MINI</b> ›<br><small style='color:#71717A;'>10 giao dịch gần nhất</small></div></html>", "<html><div style='text-align:right;'><b>MINI STATEMENT</b> ›<br><small style='color:#71717A;'>Last 10 transactions</small></div></html>"), COLOR_CARD_WHITE, COLOR_TEXT_MAIN, 2);
        btnMenuStatement.setHorizontalAlignment(SwingConstants.RIGHT);
        btnMenuQuick = btn(t("<html><div style='text-align:right;'><b>RÚT NHANH 500K</b> ›<br><small style='color:#E0562D;'>Rút nhanh 1 chạm</small></div></html>", "<html><div style='text-align:right;'><b>QUICK CASH 500K</b> ›<br><small style='color:#E0562D;'>1-touch quick cash</small></div></html>"), COLOR_BADGE_BG, COLOR_PRIMARY, 2);
        btnMenuQuick.setHorizontalAlignment(SwingConstants.RIGHT);
        btnMenuExit = btn(t("<html><div style='text-align:right;'><b>ĐĂNG XUẤT / TRẢ THẺ</b> ›<br><small style='color:#DC2626;'>Hoàn tất phiên giao dịch</small></div></html>", "<html><div style='text-align:right;'><b>EXIT & EJECT CARD</b> ›<br><small style='color:#DC2626;'>Complete & take card</small></div></html>"), new Color(254, 226, 226), COLOR_DANGER, 2);
        btnMenuExit.setHorizontalAlignment(SwingConstants.RIGHT);

        btnMenuPin.addActionListener(e -> {
            lblChangePinError.setText(" ");
            txtOldPin.setText("");
            txtNewPin.setText("");
            txtConfirmPin.setText("");
            hienThi("CHANGE_PIN", t("ĐỔI MÃ PIN", "CHANGE PIN"));
        });
        btnMenuStatement.addActionListener(e -> {
            napDuLieuSaoKe();
            hienThi("STATEMENT", t("SAO KÊ GIAO DỊCH GẦN NHẤT", "MINI STATEMENT"));
        });
        btnMenuQuick.addActionListener(e -> {
            lblMenuError.setText(" ");
            int c = JOptionPane.showConfirmDialog(this,
                    t("Quý khách có chắc chắn muốn rút nhanh 500.000 VNĐ?", "Are you sure you want to withdraw 500,000 VND?"),
                    t("Xác nhận rút nhanh", "Confirm quick cash"), JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                batDauRutTien(500_000L, lblMenuError);
            }
        });
        btnMenuExit.addActionListener(e -> dangXuat());

        pRight.add(btnMenuPin);
        pRight.add(btnMenuStatement);
        pRight.add(btnMenuQuick);
        pRight.add(btnMenuExit);

        main.add(pLeft, BorderLayout.WEST);
        main.add(pCenter, BorderLayout.CENTER);
        main.add(pRight, BorderLayout.EAST);
        return main;
    }

    private JPanel taoManHinhRutTien() {
        JPanel main = new JPanel(new BorderLayout(15, 12));
        main.setBackground(COLOR_BG_LIGHT);
        main.setBorder(BorderFactory.createEmptyBorder(18, 25, 18, 25));

        JPanel pTop = new JPanel(new BorderLayout());
        pTop.setOpaque(false);
        lblWithdrawInstruction = new JLabel(t("Vui lòng chọn mệnh giá hoặc nhập số tiền cần rút (Bội số 50.000 VNĐ):", "Please select or enter amount to withdraw (Multiples of 50,000 VND):"));
        lblWithdrawInstruction.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblWithdrawInstruction.setForeground(COLOR_TEXT_MAIN);

        lblWithdrawError = new JLabel(" ", SwingConstants.RIGHT);
        lblWithdrawError.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblWithdrawError.setForeground(COLOR_DANGER);

        pTop.add(lblWithdrawInstruction, BorderLayout.WEST);
        pTop.add(lblWithdrawError, BorderLayout.EAST);
        main.add(pTop, BorderLayout.NORTH);

        JPanel pCenter = new JPanel(new GridLayout(1, 2, 20, 0));
        pCenter.setOpaque(false);

        JPanel pQuick = new JPanel(new GridLayout(3, 2, 12, 12));
        pQuick.setOpaque(false);

        long[] amounts = {100_000L, 200_000L, 500_000L, 1_000_000L, 2_000_000L, 5_000_000L};
        for (long a : amounts) {
            JButton b = btn(DB.dinhDangTien(a), COLOR_CARD_WHITE, COLOR_TEXT_MAIN, 1);
            b.setFont(new Font("Segoe UI", Font.BOLD, 16));
            b.addActionListener(e -> {
                int c = JOptionPane.showConfirmDialog(this,
                        String.format(t("Quý khách muốn rút số tiền: %s ?", "Do you want to withdraw: %s ?"), DB.dinhDangTien(a)),
                        t("Xác nhận rút tiền", "Confirm withdrawal"), JOptionPane.YES_NO_OPTION);
                if (c == JOptionPane.YES_OPTION) {
                    batDauRutTien(a, lblWithdrawError);
                }
            });
            pQuick.add(b);
        }
        pCenter.add(pQuick);

        JPanel pCustom = new JPanel(new BorderLayout(8, 8));
        pCustom.setBackground(COLOR_CARD_WHITE);
        pCustom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DEFAULT, 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblCustom = new JLabel(t("Nhập số tiền khác (VNĐ):", "Enter custom amount (VND):"));
        lblCustom.setFont(new Font("Segoe UI", Font.BOLD, 14));

        txtWithdrawCustom = new JTextField();
        txtWithdrawCustom.setFont(new Font("Segoe UI", Font.BOLD, 20));
        txtWithdrawCustom.setHorizontalAlignment(JTextField.RIGHT);
        txtWithdrawCustom.setPreferredSize(new Dimension(280, 48));
        txtWithdrawCustom.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        txtWithdrawCustom.setBorder(borderInputDefault);
        txtWithdrawCustom.addActionListener(e -> xuLyRutTienKhac());
        ganTienFilter(txtWithdrawCustom);
        ganFocusHighlight(txtWithdrawCustom);

        JPanel pInputBox = new JPanel(new BorderLayout(4, 6));
        pInputBox.setOpaque(false);
        pInputBox.add(lblCustom, BorderLayout.NORTH);
        pInputBox.add(txtWithdrawCustom, BorderLayout.CENTER);

        JPanel pCustomKeypad = taoNumpad(txtWithdrawCustom, this::xuLyRutTienKhac, () -> hienThi("MENU", t("MENU CHÍNH", "MAIN MENU")));

        pCustom.add(pInputBox, BorderLayout.NORTH);
        pCustom.add(pCustomKeypad, BorderLayout.CENTER);

        pCenter.add(pCustom);
        main.add(pCenter, BorderLayout.CENTER);

        return main;
    }

    private void xuLyRutTienKhac() {
        try {
            long st = Long.parseLong(txtWithdrawCustom.getText().replaceAll("\\D", ""));
            batDauRutTien(st, lblWithdrawError);
        } catch (Exception ex) {
            lblWithdrawError.setText(t("Số tiền nhập vào không hợp lệ!", "Invalid amount entered!"));
            if (txtWithdrawCustom != null) txtWithdrawCustom.setBorder(borderInputError);
        }
    }

    private void batDauRutTien(long soTien, JLabel lblError) {
        lblError.setText(" ");
        if (txtWithdrawCustom != null) txtWithdrawCustom.setBorder(borderInputDefault);
        DB.GiaoDichResult res = DB.rutTien(accId, soTien);
        if (res.thanhCong) {
            lastTxnType = t("RÚT TIỀN MẶT", "CASH WITHDRAWAL");
            lastTxnAmount = soTien;
            lastTxnBalance = res.soDuMoi;
            lastTxnPartner = "-";
            lastTxnTime = res.thoiGian;
            lastTxnId = res.maGiaoDich;
            lastBreakdown = DB.chiaCoSoToTien(soTien);

            chayManHinhXuLy(t("RÚT TIỀN MẶT", "CASH WITHDRAWAL"), () -> {
                hienThiHoiInBienLai(soTien, lastBreakdown);
            });
        } else {
            String msg = t("Giao dịch không thành công!", "Transaction failed!");
            if ("SO_TIEN_KHONG_HOP_LE".equals(res.maLoi)) msg = t("Số tiền rút phải lớn hơn 0!", "Amount must be greater than 0!");
            else if ("BOI_SO".equals(res.maLoi)) msg = t("Số tiền phải là bội số của " + DB.dinhDangTien(DB.BOI_SO) + "!", "Amount must be multiple of " + DB.dinhDangTien(DB.BOI_SO) + "!");
            else if ("HAN_MUC_LAN".equals(res.maLoi)) msg = t("Vượt hạn mức 1 lần rút: " + DB.dinhDangTien(DB.HAN_MUC_LAN) + "!", "Exceeds per-transaction limit: " + DB.dinhDangTien(DB.HAN_MUC_LAN) + "!");
            else if ("HAN_MUC_NGAY".equals(res.maLoi)) msg = t("Vượt hạn mức rút tối đa trong ngày (" + DB.dinhDangTien(DB.HAN_MUC_NGAY) + ")!", "Exceeds daily withdrawal limit (" + DB.dinhDangTien(DB.HAN_MUC_NGAY) + ")!");
            else if ("KHONG_DU_SO_DU".equals(res.maLoi)) msg = t("Số dư tài khoản không đủ để thực hiện giao dịch!", "Insufficient account balance!");
            else if ("LOI_KET_NOI".equals(res.maLoi)) msg = t("Lỗi kết nối CSDL!", "Database error!");
            lblError.setText(msg);
            if (txtWithdrawCustom != null) txtWithdrawCustom.setBorder(borderInputError);
        }
    }

    private JPanel taoManHinhNapTien() {
        JPanel main = new JPanel(new BorderLayout(15, 12));
        main.setBackground(COLOR_BG_LIGHT);
        main.setBorder(BorderFactory.createEmptyBorder(18, 25, 18, 25));

        JPanel pTop = new JPanel(new BorderLayout());
        pTop.setOpaque(false);
        lblDepositInstruction = new JLabel(t("Vui lòng chọn hoặc nhập số tiền mặt cần nạp vào tài khoản:", "Please select or enter amount to deposit into account:"));
        lblDepositInstruction.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblDepositInstruction.setForeground(COLOR_TEXT_MAIN);

        lblDepositError = new JLabel(" ", SwingConstants.RIGHT);
        lblDepositError.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblDepositError.setForeground(COLOR_DANGER);

        pTop.add(lblDepositInstruction, BorderLayout.WEST);
        pTop.add(lblDepositError, BorderLayout.EAST);
        main.add(pTop, BorderLayout.NORTH);

        JPanel pCenter = new JPanel(new GridLayout(1, 2, 20, 0));
        pCenter.setOpaque(false);

        JPanel pQuick = new JPanel(new GridLayout(3, 2, 12, 12));
        pQuick.setOpaque(false);

        long[] amounts = {500_000L, 1_000_000L, 2_000_000L, 5_000_000L, 10_000_000L, 20_000_000L};
        for (long a : amounts) {
            JButton b = btn(DB.dinhDangTien(a), COLOR_CARD_WHITE, COLOR_TEXT_MAIN, 1);
            b.setFont(new Font("Segoe UI", Font.BOLD, 16));
            b.addActionListener(e -> {
                int c = JOptionPane.showConfirmDialog(this,
                        String.format(t("Quý khách muốn nạp số tiền: %s ?", "Do you want to deposit: %s ?"), DB.dinhDangTien(a)),
                        t("Xác nhận nạp tiền", "Confirm deposit"), JOptionPane.YES_NO_OPTION);
                if (c == JOptionPane.YES_OPTION) {
                    batDauNapTien(a);
                }
            });
            pQuick.add(b);
        }
        pCenter.add(pQuick);

        JPanel pCustom = new JPanel(new BorderLayout(8, 8));
        pCustom.setBackground(COLOR_CARD_WHITE);
        pCustom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DEFAULT, 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblCustom = new JLabel(t("Nhập số tiền cần nạp (VNĐ):", "Enter amount to deposit (VND):"));
        lblCustom.setFont(new Font("Segoe UI", Font.BOLD, 14));

        txtDepositCustom = new JTextField();
        txtDepositCustom.setFont(new Font("Segoe UI", Font.BOLD, 20));
        txtDepositCustom.setHorizontalAlignment(JTextField.RIGHT);
        txtDepositCustom.setPreferredSize(new Dimension(280, 48));
        txtDepositCustom.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        txtDepositCustom.setBorder(borderInputDefault);
        txtDepositCustom.addActionListener(e -> xuLyNapTienKhac());
        ganTienFilter(txtDepositCustom);
        ganFocusHighlight(txtDepositCustom);

        JPanel pInputBox = new JPanel(new BorderLayout(4, 6));
        pInputBox.setOpaque(false);
        pInputBox.add(lblCustom, BorderLayout.NORTH);
        pInputBox.add(txtDepositCustom, BorderLayout.CENTER);

        JPanel pCustomKeypad = taoNumpad(txtDepositCustom, this::xuLyNapTienKhac, () -> hienThi("MENU", t("MENU CHÍNH", "MAIN MENU")));

        pCustom.add(pInputBox, BorderLayout.NORTH);
        pCustom.add(pCustomKeypad, BorderLayout.CENTER);

        pCenter.add(pCustom);
        main.add(pCenter, BorderLayout.CENTER);

        return main;
    }

    private void xuLyNapTienKhac() {
        try {
            long st = Long.parseLong(txtDepositCustom.getText().replaceAll("\\D", ""));
            batDauNapTien(st);
        } catch (Exception ex) {
            lblDepositError.setText(t("Số tiền nhập vào không hợp lệ!", "Invalid amount entered!"));
            if (txtDepositCustom != null) txtDepositCustom.setBorder(borderInputError);
        }
    }

    private void batDauNapTien(long soTien) {
        lblDepositError.setText(" ");
        if (txtDepositCustom != null) txtDepositCustom.setBorder(borderInputDefault);
        DB.GiaoDichResult res = DB.napTien(accId, soTien);
        if (res.thanhCong) {
            lastTxnType = t("NẠP TIỀN MẶT", "CASH DEPOSIT");
            lastTxnAmount = soTien;
            lastTxnBalance = res.soDuMoi;
            lastTxnPartner = "-";
            lastTxnTime = res.thoiGian;
            lastTxnId = res.maGiaoDich;
            lastBreakdown = null;

            chayManHinhXuLy(t("NẠP TIỀN MẶT", "CASH DEPOSIT"), () -> {
                hienThiHoiInBienLai(soTien, null);
            });
        } else {
            String msg = t("Nạp tiền thất bại!", "Deposit failed!");
            if ("BOI_SO".equals(res.maLoi)) msg = t("Số tiền nạp phải là bội số của 50.000 VNĐ!", "Deposit must be multiple of 50,000 VND!");
            else if ("HAN_MUC_LAN".equals(res.maLoi)) msg = t("Vượt quá hạn mức 1 lần nạp (50.000.000 VNĐ)!", "Exceeds max deposit per transaction (50,000,000 VND)!");
            lblDepositError.setText(msg);
            if (txtDepositCustom != null) txtDepositCustom.setBorder(borderInputError);
        }
    }

    private JPanel taoManHinhXuLy() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(COLOR_BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 20, 12, 20);
        gbc.gridx = 0;

        lblProcTitle = new JLabel(t("HỆ THỐNG ĐANG XỬ LÝ GIAO DỊCH...", "SYSTEM IS PROCESSING TRANSACTION..."), SwingConstants.CENTER);
        lblProcTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblProcTitle.setForeground(COLOR_TEXT_MAIN);

        barProc = new JProgressBar();
        barProc.setIndeterminate(true);
        barProc.setPreferredSize(new Dimension(450, 14));
        barProc.setForeground(COLOR_PRIMARY);
        barProc.setBackground(COLOR_BADGE_BG);

        lblProcStatus = new JLabel(t("Vui lòng chờ trong giây lát...", "Please wait a moment..."), SwingConstants.CENTER);
        lblProcStatus.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblProcStatus.setForeground(COLOR_TEXT_MUTED);

        JPanel pNotice = new JPanel(new BorderLayout());
        pNotice.setBackground(COLOR_BADGE_BG);
        pNotice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BADGE_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));

        lblProcNotice = new JLabel(t("QUY TRÌNH AN TOÀN: NHẬN THẺ TRƯỚC, NHẬN TIỀN SAU", "SAFETY RULE: TAKE YOUR CARD FIRST, THEN COLLECT CASH"), SwingConstants.CENTER);
        lblProcNotice.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblProcNotice.setForeground(COLOR_PRIMARY);
        pNotice.add(lblProcNotice, BorderLayout.CENTER);

        gbc.gridy = 0; p.add(lblProcTitle, gbc);
        gbc.gridy = 1; p.add(barProc, gbc);
        gbc.gridy = 2; p.add(lblProcStatus, gbc);
        gbc.gridy = 3; p.add(pNotice, gbc);

        return p;
    }

    private void chayManHinhXuLy(String loaiGd, Runnable onDone) {
        String notice;
        if (loaiGd.contains("RÚT") || loaiGd.contains("WITHDRAW") || loaiGd.contains("500K")) {
            notice = t("QUY TRÌNH AN TOÀN: NHẬN THẺ TRƯỚC, NHẬN TIỀN SAU", "SAFETY RULE: TAKE YOUR CARD FIRST, THEN COLLECT CASH");
        } else if (loaiGd.contains("NẠP") || loaiGd.contains("DEPOSIT")) {
            notice = t("QUY TRÌNH AN TOÀN: ĐANG KIỂM ĐẾM TIỀN MẶT TẠI KHAY NẠP", "SAFETY RULE: COUNTING CASH IN DEPOSIT COMPARTMENT");
        } else if (loaiGd.contains("CHUYỂN") || loaiGd.contains("TRANSFER")) {
            notice = t("ĐANG XỬ LÝ CHUYỂN TIỀN NHANH 24/7 ĐẾN NGƯỜI THỤ HƯỞNG", "PROCESSING INSTANT 24/7 TRANSFER TO BENEFICIARY");
        } else if (loaiGd.contains("PIN")) {
            notice = t("ĐANG MÃ HÓA VÀ LƯU TRỮ MẬT MÃ BẢO VỆ MỚI", "ENCRYPTING AND STORING NEW SECURITY PIN");
        } else {
            notice = t("HỆ THỐNG ĐANG ĐỒNG BỘ DỮ LIỆU GIAO DỊCH AN TOÀN...", "SYNCHRONIZING TRANSACTION DATA SECURELY...");
        }
        lblProcNotice.setText(notice);
        hienThi("PROCESSING", t("ĐANG XỬ LÝ GIAO DỊCH", "PROCESSING TRANSACTION"));

        if (timerProcessing != null && timerProcessing.isRunning()) timerProcessing.stop();
        timerProcessing = new Timer(1300, e -> {
            ((Timer) e.getSource()).stop();
            if (onDone != null) onDone.run();
        });
        timerProcessing.setRepeats(false);
        timerProcessing.start();
    }

    private JPanel taoManHinhHoiInBienLai() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(COLOR_BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 15, 8, 15);
        gbc.gridx = 0;

        JPanel pBadgeCheck = new JPanel() {
            {
                setPreferredSize(new Dimension(64, 64));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_SUCCESS);
                g2.fillOval(4, 4, 56, 56);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(18, 33, 27, 42);
                g2.drawLine(27, 42, 45, 22);
                g2.dispose();
            }
        };

        lblAskTitle = new JLabel(t("GIAO DỊCH THÀNH CÔNG!", "TRANSACTION SUCCESSFUL!"), SwingConstants.CENTER);
        lblAskTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblAskTitle.setForeground(COLOR_SUCCESS);

        lblAskSub = new JLabel(t("Quý khách có muốn in biên lai giao dịch không?", "Would you like to print a transaction receipt?"), SwingConstants.CENTER);
        lblAskSub.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblAskSub.setForeground(COLOR_PRIMARY);

        lblAskBreakdown = new JLabel("", SwingConstants.CENTER);
        lblAskBreakdown.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblAskBreakdown.setForeground(COLOR_TEXT_MUTED);

        JPanel pBreakdownCard = new JPanel(new BorderLayout());
        pBreakdownCard.setBackground(COLOR_CARD_WHITE);
        pBreakdownCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DEFAULT, 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        pBreakdownCard.add(lblAskBreakdown, BorderLayout.CENTER);

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15));
        pBtn.setOpaque(false);

        btnAskPrint = btn(t("IN BIÊN LAI", "PRINT RECEIPT"), COLOR_PRIMARY, Color.WHITE, 0);
        btnAskPrint.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnAskPrint.setPreferredSize(new Dimension(210, 54));
        btnAskPrint.addActionListener(e -> {
            hienThiBienLai(lastTxnType, lastTxnAmount, lastTxnBalance, lastTxnPartner, lastTxnTime, lastTxnId, lastBreakdown);
        });

        btnAskSkip = btn(t("KHÔNG IN (VỀ MENU)", "NO RECEIPT (MENU)"), COLOR_BADGE_BG, COLOR_PRIMARY, 0);
        btnAskSkip.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnAskSkip.setPreferredSize(new Dimension(240, 54));
        btnAskSkip.addActionListener(e -> hienThi("MENU", t("MENU CHÍNH", "MAIN MENU")));

        pBtn.add(btnAskPrint);
        pBtn.add(btnAskSkip);

        gbc.gridy = 0; p.add(pBadgeCheck, gbc);
        gbc.gridy = 1; p.add(lblAskTitle, gbc);
        gbc.gridy = 2; p.add(pBreakdownCard, gbc);
        gbc.gridy = 3; p.add(lblAskSub, gbc);
        gbc.gridy = 4; p.add(pBtn, gbc);

        return p;
    }

    private void hienThiHoiInBienLai(long soTien, DB.CashBreakdown bd) {
        if (bd != null && bd.tongTo() > 0) {
            String bdStr = "<html><center>" +
                    t("<b>Chi tiết cơ số tờ tiền nhả ra:</b><br>", "<b>Cash denomination breakdown:</b><br>") +
                    (bd.to500k > 0 ? "500.000đ: <b>" + bd.to500k + t(" tờ</b> &nbsp;|&nbsp; ", " bill(s)</b> &nbsp;|&nbsp; ") : "") +
                    (bd.to200k > 0 ? "200.000đ: <b>" + bd.to200k + t(" tờ</b> &nbsp;|&nbsp; ", " bill(s)</b> &nbsp;|&nbsp; ") : "") +
                    (bd.to100k > 0 ? "100.000đ: <b>" + bd.to100k + t(" tờ</b> &nbsp;|&nbsp; ", " bill(s)</b> &nbsp;|&nbsp; ") : "") +
                    (bd.to50k > 0 ? "50.000đ: <b>" + bd.to50k + t(" tờ</b>", " bill(s)</b>") : "") +
                    "<br><span style='color:#10B981;'>" + t("Vui lòng kiểm đếm tiền tại khe trả tiền bên dưới!", "Please collect your cash from the dispenser below!") + "</span>" +
                    "</center></html>";
            lblAskBreakdown.setText(bdStr);
        } else {
            lblAskBreakdown.setText(String.format(t("Số tiền giao dịch: %s", "Transaction amount: %s"), DB.dinhDangTien(soTien)));
        }
        hienThi("ASK_RECEIPT", t("KẾT QUẢ GIAO DỊCH", "TRANSACTION RESULT"));
    }

    private JPanel taoManHinhChuyenKhoan() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(COLOR_BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel pForm = new JPanel(new GridLayout(6, 1, 6, 6));
        pForm.setBackground(COLOR_CARD_WHITE);
        pForm.setPreferredSize(new Dimension(360, 320));
        pForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BADGE_BORDER, 1, true),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        JLabel lblStk = new JLabel(t("Nhập số tài khoản thụ hưởng:", "Enter beneficiary account number:"));
        lblStk.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel pAccInput = new JPanel(new BorderLayout(8, 0));
        pAccInput.setOpaque(false);
        txtTransferAcc = new JTextField(15);
        txtTransferAcc.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtTransferAcc.setPreferredSize(new Dimension(180, 42));
        txtTransferAcc.setBorder(borderInputDefault);
        ganDigitFilter(txtTransferAcc, 12);
        ganFocusHighlight(txtTransferAcc);

        JButton btnCheck = btn(t("KIỂM TRA", "VERIFY"), COLOR_PRIMARY, Color.WHITE, 0);
        btnCheck.setPreferredSize(new Dimension(110, 40));
        btnCheck.addActionListener(e -> kiemTraNguoiNhan());
        pAccInput.add(txtTransferAcc, BorderLayout.CENTER);
        pAccInput.add(btnCheck, BorderLayout.EAST);

        lblReceiverName = new JLabel(t("Người nhận: (Chưa xác định)", "Beneficiary: (Unchecked)"));
        lblReceiverName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblReceiverName.setForeground(COLOR_TEXT_MUTED);

        JLabel lblAmount = new JLabel(t("Nhập số tiền chuyển (VNĐ):", "Enter transfer amount (VND):"));
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 14));

        txtTransferAmount = new JTextField(15);
        txtTransferAmount.setFont(new Font("Segoe UI", Font.BOLD, 20));
        txtTransferAmount.setHorizontalAlignment(JTextField.RIGHT);
        txtTransferAmount.setPreferredSize(new Dimension(180, 42));
        txtTransferAmount.setBorder(borderInputDefault);
        ganTienFilter(txtTransferAmount);
        ganFocusHighlight(txtTransferAmount);

        lblTransferError = new JLabel(" ", SwingConstants.CENTER);
        lblTransferError.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTransferError.setForeground(COLOR_DANGER);

        pForm.add(lblStk);
        pForm.add(pAccInput);
        pForm.add(lblReceiverName);
        pForm.add(lblAmount);
        pForm.add(txtTransferAmount);
        pForm.add(lblTransferError);

        JPanel pKeypad = taoNumpad(null, this::xacNhanChuyenKhoanBuoc1, () -> hienThi("MENU", t("MENU CHÍNH", "MAIN MENU")));

        gbc.gridx = 0; gbc.gridy = 0; center.add(pForm, gbc);
        gbc.gridx = 1; center.add(pKeypad, gbc);

        return center;
    }

    private void kiemTraNguoiNhan() {
        String stk = txtTransferAcc.getText().trim();
        if (stk.isEmpty()) {
            lblReceiverName.setText(t("Vui lòng nhập STK!", "Please enter account no!"));
            lblReceiverName.setForeground(COLOR_DANGER);
            return;
        }
        if (stk.equals(this.soTaiKhoan)) {
            lblReceiverName.setText(t("Không thể tự chuyển cho chính mình!", "Cannot transfer to own account!"));
            lblReceiverName.setForeground(COLOR_DANGER);
            return;
        }
        String ten = DB.tenNguoiNhan(stk);
        if (ten != null) {
            lblReceiverName.setText(t("Người nhận: ", "Beneficiary: ") + ten);
            lblReceiverName.setForeground(COLOR_SUCCESS);
            txtTransferAmount.requestFocus();
        } else {
            lblReceiverName.setText(t("Không tìm thấy tài khoản này!", "Account number not found!"));
            lblReceiverName.setForeground(COLOR_DANGER);
        }
    }

    private void xacNhanChuyenKhoanBuoc1() {
        lblTransferError.setText(" ");
        String stk = txtTransferAcc.getText().trim();
        if (stk.isEmpty()) {
            lblTransferError.setText(t("Vui lòng nhập số tài khoản người nhận!", "Please enter beneficiary account number!"));
            return;
        }
        if (stk.equals(this.soTaiKhoan)) {
            lblTransferError.setText(t("Không thể chuyển khoản cho chính mình!", "Cannot transfer to own account!"));
            return;
        }
        String tenNhan = DB.tenNguoiNhan(stk);
        if (tenNhan == null) {
            lblTransferError.setText(t("Tài khoản thụ hưởng không tồn tại!", "Beneficiary account does not exist!"));
            return;
        }

        long soTien;
        try {
            soTien = Long.parseLong(txtTransferAmount.getText().replaceAll("\\D", ""));
        } catch (Exception ex) {
            lblTransferError.setText(t("Số tiền chuyển không hợp lệ!", "Invalid amount entered!"));
            return;
        }

        if (soTien <= 0) {
            lblTransferError.setText(t("Số tiền chuyển phải lớn hơn 0!", "Amount must be greater than 0!"));
            return;
        }
        if (soTien > DB.HAN_MUC_CK_LAN) {
            lblTransferError.setText(t("Vượt quá hạn mức 1 lần: " + DB.dinhDangTien(DB.HAN_MUC_CK_LAN), "Exceeds per-transfer limit: " + DB.dinhDangTien(DB.HAN_MUC_CK_LAN)));
            return;
        }
        long soDuHienTai = DB.laySoDu(accId);
        if (soTien > soDuHienTai) {
            lblTransferError.setText(t("Số dư tài khoản không đủ để chuyển!", "Insufficient balance!"));
            return;
        }

        this.pendingTransferStk = stk;
        this.pendingTransferAmount = soTien;
        lblConfirmSender.setText(tenChuTk + " (" + soTaiKhoan + ")");
        lblConfirmReceiver.setText(tenNhan + " (" + stk + ")");
        lblConfirmAmount.setText(DB.dinhDangTien(soTien));
        lblConfirmFee.setText(t("0 VNĐ (Miễn phí chuyển tiền)", "0 VND (Free of charge)"));

        hienThi("CONFIRM_TRANSFER", t("XÁC NHẬN CHUYỂN KHOẢN", "CONFIRM TRANSFER"));
    }

    private JPanel taoRowXacNhan(String label, JComponent val, boolean hasBottomLine) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setBackground(COLOR_CARD_WHITE);
        if (hasBottomLine) {
            r.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BADGE_BORDER),
                    BorderFactory.createEmptyBorder(10, 5, 10, 5)
            ));
        } else {
            r.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        }
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lbl.setForeground(COLOR_TEXT_MUTED);
        r.add(lbl, BorderLayout.WEST);
        r.add(val, BorderLayout.EAST);
        return r;
    }

    private JPanel taoManHinhXacNhanChuyenKhoan() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(COLOR_BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 15, 8, 15);
        gbc.gridx = 0;

        JPanel pCard = new JPanel(new GridLayout(5, 1, 0, 0));
        pCard.setBackground(COLOR_CARD_WHITE);
        pCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BADGE_BORDER, 1, true),
                BorderFactory.createEmptyBorder(15, 25, 15, 25)
        ));
        pCard.setPreferredSize(new Dimension(520, 260));

        lblConfirmSender = new JLabel();
        lblConfirmSender.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblConfirmReceiver = new JLabel();
        lblConfirmReceiver.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblConfirmReceiver.setForeground(COLOR_SECONDARY);
        lblConfirmAmount = new JLabel();
        lblConfirmAmount.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblConfirmAmount.setForeground(COLOR_PRIMARY);
        lblConfirmFee = new JLabel("0 VNĐ");
        lblConfirmFee.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblConfirmFee.setForeground(COLOR_SUCCESS);

        pCard.add(taoRowXacNhan(t("Tài khoản trích tiền:", "Debit account:"), lblConfirmSender, true));
        pCard.add(taoRowXacNhan(t("Người thụ hưởng:", "Beneficiary:"), lblConfirmReceiver, true));
        pCard.add(taoRowXacNhan(t("Số tiền chuyển:", "Transfer amount:"), lblConfirmAmount, true));
        pCard.add(taoRowXacNhan(t("Phí giao dịch:", "Transaction fee:"), lblConfirmFee, true));
        pCard.add(taoRowXacNhan(t("Hình thức:", "Method:"), new JLabel(t("Chuyển tiền nhanh 24/7", "Instant transfer 24/7")), false));

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        pBtn.setBackground(COLOR_BG_LIGHT);
        JButton btnOk = btn(t("XÁC NHẬN CHUYỂN", "CONFIRM TRANSFER"), COLOR_PRIMARY, Color.WHITE, 0);
        btnOk.setPreferredSize(new Dimension(200, 48));
        JButton btnCancel = btn(t("QUAY LẠI", "BACK"), COLOR_DANGER, Color.WHITE, 0);
        btnCancel.setPreferredSize(new Dimension(140, 48));

        btnOk.addActionListener(e -> xuLyThucHienChuyenKhoan());
        btnCancel.addActionListener(e -> hienThi("TRANSFER", t("CHUYỂN KHOẢN", "FUND TRANSFER")));

        pBtn.add(btnOk);
        pBtn.add(btnCancel);

        JLabel lblTitle = new JLabel(t("VUI LÒNG KIỂM TRA KỸ THÔNG TIN GIAO DỊCH", "PLEASE VERIFY TRANSACTION DETAILS CAREFULLY"), SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(COLOR_PRIMARY);

        gbc.gridy = 0; center.add(lblTitle, gbc);
        gbc.gridy = 1; center.add(pCard, gbc);
        gbc.gridy = 2; center.add(pBtn, gbc);

        return center;
    }

    private void xuLyThucHienChuyenKhoan() {
        DB.GiaoDichResult res = DB.chuyenKhoan(accId, pendingTransferStk, pendingTransferAmount);
        if (res.thanhCong) {
            lastTxnType = t("CHUYỂN KHOẢN", "FUND TRANSFER");
            lastTxnAmount = pendingTransferAmount;
            lastTxnBalance = res.soDuMoi;
            lastTxnPartner = pendingTransferStk;
            lastTxnTime = res.thoiGian;
            lastTxnId = res.maGiaoDich;
            lastBreakdown = null;

            chayManHinhXuLy(t("CHUYỂN KHOẢN", "FUND TRANSFER"), () -> {
                hienThiHoiInBienLai(pendingTransferAmount, null);
            });
        } else {
            String msg = t("Chuyển khoản thất bại!", "Transfer failed!");
            if ("KHONG_DU_SO_DU".equals(res.maLoi)) msg = t("Số dư tài khoản không đủ!", "Insufficient balance!");
            else if ("HAN_MUC_NGAY".equals(res.maLoi)) msg = t("Vượt quá hạn mức chuyển khoản trong ngày (20.000.000 VNĐ)!", "Exceeds daily transfer limit (20,000,000 VND)!");
            JOptionPane.showMessageDialog(this, msg, t("Thông báo", "Notification"), JOptionPane.ERROR_MESSAGE);
            hienThi("TRANSFER", t("CHUYỂN KHOẢN", "FUND TRANSFER"));
        }
    }

    private JPanel taoManHinhDoiPin() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(COLOR_BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel pForm = new JPanel(new GridLayout(6, 1, 6, 6));
        pForm.setBackground(COLOR_CARD_WHITE);
        pForm.setPreferredSize(new Dimension(360, 320));
        pForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BADGE_BORDER, 1, true),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        JLabel lbl1 = new JLabel(t("Nhập mã PIN hiện tại (6 số):", "Enter current PIN (6 digits):"));
        lbl1.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtOldPin = new JPasswordField(6);
        txtOldPin.setFont(new Font("Segoe UI", Font.BOLD, 22));
        txtOldPin.setBorder(borderInputDefault);
        ganDigitFilter(txtOldPin, 6);
        ganFocusHighlight(txtOldPin);

        JLabel lbl2 = new JLabel(t("Nhập mã PIN mới (6 số):", "Enter new PIN (6 digits):"));
        lbl2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtNewPin = new JPasswordField(6);
        txtNewPin.setFont(new Font("Segoe UI", Font.BOLD, 22));
        txtNewPin.setBorder(borderInputDefault);
        ganDigitFilter(txtNewPin, 6);
        ganFocusHighlight(txtNewPin);

        JLabel lbl3 = new JLabel(t("Xác nhận lại mã PIN mới:", "Confirm new PIN:"));
        lbl3.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtConfirmPin = new JPasswordField(6);
        txtConfirmPin.setFont(new Font("Segoe UI", Font.BOLD, 22));
        txtConfirmPin.setBorder(borderInputDefault);
        ganDigitFilter(txtConfirmPin, 6);
        ganFocusHighlight(txtConfirmPin);

        lblChangePinError = new JLabel(" ", SwingConstants.CENTER);
        lblChangePinError.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblChangePinError.setForeground(COLOR_DANGER);

        pForm.add(lbl1);
        pForm.add(txtOldPin);
        pForm.add(lbl2);
        pForm.add(txtNewPin);
        pForm.add(lbl3);
        pForm.add(txtConfirmPin);

        JPanel pKeypad = taoNumpad(null, this::xuLyDoiPin, () -> hienThi("MENU", t("MENU CHÍNH", "MAIN MENU")));

        gbc.gridx = 0; gbc.gridy = 0; center.add(pForm, gbc);
        gbc.gridx = 1; center.add(pKeypad, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; center.add(lblChangePinError, gbc);

        return center;
    }

    private void xuLyDoiPin() {
        lblChangePinError.setText(" ");
        String oldP = new String(txtOldPin.getPassword()).trim();
        String newP = new String(txtNewPin.getPassword()).trim();
        String confP = new String(txtConfirmPin.getPassword()).trim();

        if (oldP.length() != 6 || newP.length() != 6 || confP.length() != 6) {
            lblChangePinError.setText(t("Mã PIN phải đủ đúng 6 chữ số!", "PIN must be exactly 6 digits!"));
            return;
        }
        if (oldP.equals(newP)) {
            lblChangePinError.setText(t("Mã PIN mới không được trùng mã PIN cũ!", "New PIN cannot be same as old PIN!"));
            return;
        }
        if (!newP.equals(confP)) {
            lblChangePinError.setText(t("Mã PIN nhập lại không khớp!", "PIN confirmation does not match!"));
            return;
        }

        DB.GiaoDichResult res = DB.doiPin(soThe, oldP, newP);
        if (res.thanhCong) {
            chayManHinhXuLy(t("ĐỔI MÃ PIN", "CHANGE PIN"), () -> {
                JOptionPane.showMessageDialog(this, t("Đổi mã PIN thành công! Vui lòng bảo mật mã PIN mới.", "PIN successfully changed!"), t("Thông báo", "Notification"), JOptionPane.INFORMATION_MESSAGE);
                hienThi("MENU", t("MENU CHÍNH", "MAIN MENU"));
            });
        } else {
            lblChangePinError.setText(t("Mã PIN hiện tại không chính xác!", "Current PIN is incorrect!"));
        }
    }

    private JPanel taoManHinhXemSoDu() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(COLOR_BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.gridx = 0;

        JPanel pCard = new JPanel(new GridLayout(4, 1, 0, 0));
        pCard.setBackground(COLOR_CARD_WHITE);
        pCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BADGE_BORDER, 1, true),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        pCard.setPreferredSize(new Dimension(520, 280));

        lblBalanceAccName = new JLabel();
        lblBalanceAccName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblBalanceAccNo = new JLabel();
        lblBalanceAccNo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblBalanceCardNo = new JLabel();
        lblBalanceCardNo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblBalanceAvailable = new JLabel();

        pCard.add(taoRowXacNhan(t("Chủ tài khoản:", "Account holder:"), lblBalanceAccName, true));
        pCard.add(taoRowXacNhan(t("Số tài khoản:", "Account number:"), lblBalanceAccNo, true));
        pCard.add(taoRowXacNhan(t("Số thẻ liên kết:", "Linked card:"), lblBalanceCardNo, true));

        JPanel pBalanceHighlight = new JPanel(new BorderLayout(10, 0));
        pBalanceHighlight.setBackground(COLOR_BADGE_BG);
        pBalanceHighlight.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BADGE_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        JLabel lblBalTitle = new JLabel(t("Số dư khả dụng:", "Available balance:"));
        lblBalTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblBalTitle.setForeground(COLOR_PRIMARY);
        lblBalanceAvailable.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblBalanceAvailable.setForeground(COLOR_PRIMARY);
        pBalanceHighlight.add(lblBalTitle, BorderLayout.WEST);
        pBalanceHighlight.add(lblBalanceAvailable, BorderLayout.EAST);

        pCard.add(pBalanceHighlight);

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        pBtn.setBackground(COLOR_BG_LIGHT);
        JButton btnBack = btn(t("QUAY LẠI MENU", "BACK TO MENU"), COLOR_PRIMARY, Color.WHITE, 0);
        btnBack.setPreferredSize(new Dimension(170, 46));
        btnBack.addActionListener(e -> hienThi("MENU", t("MENU CHÍNH", "MAIN MENU")));
        pBtn.add(btnBack);

        gbc.gridy = 0; center.add(pCard, gbc);
        gbc.gridy = 1; center.add(pBtn, gbc);
        return center;
    }

    private JPanel taoManHinhSaoKe() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(COLOR_BG_LIGHT);
        main.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JPanel pTop = new JPanel(new BorderLayout(10, 0));
        pTop.setBackground(COLOR_CARD_WHITE);
        pTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BADGE_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        lblSumInfo = new JLabel(t("Chủ tài khoản: NGUYỄN VĂN AN  |  Số TK: 1234567890  |  10 giao dịch gần nhất", "Account: NGUYỄN VĂN AN  |  A/C: 1234567890  |  Last 10 transactions"));
        lblSumInfo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSumInfo.setForeground(COLOR_TEXT_MAIN);
        pTop.add(lblSumInfo, BorderLayout.CENTER);
        main.add(pTop, BorderLayout.NORTH);

        String[] cols = {t("Thời gian", "Time"), t("Loại GD", "Type"), t("Số tiền", "Amount"), t("Số dư sau", "Balance"), t("Đối tác", "Partner")};
        modelStatement = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblStatement = new JTable(modelStatement);
        tblStatement.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblStatement.setRowHeight(32);
        tblStatement.setGridColor(COLOR_BADGE_BORDER);
        tblStatement.setShowVerticalLines(false);
        tblStatement.getTableHeader().setReorderingAllowed(false);

        tblStatement.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            {
                setOpaque(true);
            }

            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                setBackground(COLOR_PRIMARY);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 14));
                setPreferredSize(new Dimension(100, 36));
                if (c == 2 || c == 3) {
                    setHorizontalAlignment(RIGHT);
                    setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
                } else {
                    setHorizontalAlignment(CENTER);
                    setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                }
                return this;
            }
        });

        DefaultTableCellRenderer cellRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) {
                    setBackground(r % 2 == 0 ? Color.WHITE : COLOR_BADGE_BG);
                }
                String text = v != null ? v.toString() : "";
                if (c == 2) {
                    setHorizontalAlignment(RIGHT);
                    setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
                    if ("-".equals(text.trim())) {
                        setForeground(COLOR_TEXT_MUTED);
                    } else if (text.startsWith("-") && text.length() > 1) {
                        setForeground(COLOR_DANGER);
                    } else if (text.startsWith("+")) {
                        setForeground(COLOR_SUCCESS);
                    } else {
                        setForeground(COLOR_PRIMARY);
                    }
                    setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else if (c == 3) {
                    setHorizontalAlignment(RIGHT);
                    setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
                    setForeground(COLOR_TEXT_MAIN);
                    setFont(new Font("Segoe UI", Font.PLAIN, 14));
                } else {
                    setHorizontalAlignment(CENTER);
                    setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                    setForeground(COLOR_TEXT_MAIN);
                    setFont(new Font("Segoe UI", Font.PLAIN, 14));
                }
                return this;
            }
        };

        tblStatement.setDefaultRenderer(Object.class, cellRender);
        for (int i = 0; i < tblStatement.getColumnCount(); i++) {
            tblStatement.getColumnModel().getColumn(i).setCellRenderer(cellRender);
        }

        tblStatement.getColumnModel().getColumn(0).setPreferredWidth(160);
        tblStatement.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblStatement.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblStatement.getColumnModel().getColumn(3).setPreferredWidth(140);
        tblStatement.getColumnModel().getColumn(4).setPreferredWidth(110);

        JScrollPane sp = new JScrollPane(tblStatement);
        sp.getViewport().setBackground(Color.WHITE);
        sp.setBorder(BorderFactory.createLineBorder(COLOR_BADGE_BORDER, 1, true));

        JPanel pBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        pBottom.setBackground(COLOR_BG_LIGHT);
        JButton btnExport = btn(t("XUẤT SAO KÊ (.TXT)", "EXPORT STATEMENT (.TXT)"), COLOR_BADGE_BG, COLOR_PRIMARY, 0);
        btnExport.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnExport.setPreferredSize(new Dimension(200, 44));
        btnExport.addActionListener(e -> xuLyXuatSaoKeTxt());

        JButton btnBack = btn(t("QUAY LẠI MENU", "BACK TO MENU"), COLOR_PRIMARY, Color.WHITE, 0);
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnBack.setPreferredSize(new Dimension(170, 44));
        btnBack.addActionListener(e -> hienThi("MENU", t("MENU CHÍNH", "MAIN MENU")));
        pBottom.add(btnExport);
        pBottom.add(btnBack);

        main.add(sp, BorderLayout.CENTER);
        main.add(pBottom, BorderLayout.SOUTH);
        return main;
    }

    private void napDuLieuSaoKe() {
        modelStatement.setRowCount(0);
        List<String[]> list = DB.saoKeMini(accId);
        for (String[] r : list) {
            modelStatement.addRow(r);
        }
        if (lblSumInfo != null) {
            lblSumInfo.setText(t("Chủ TK: " + tenChuTk + "  |  Số TK: " + soTaiKhoan + "  |  10 giao dịch gần nhất",
                                 "Account: " + tenChuTk + "  |  A/C No: " + soTaiKhoan + "  |  Last 10 transactions"));
            lblSumInfo.setForeground(COLOR_TEXT_MAIN);
        }
    }

    private void xuLyXuatSaoKeTxt() {
        try {
            String fname = "saoke_" + soTaiKhoan + ".txt";
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            StringBuilder sb = new StringBuilder();
            sb.append("========================================================================\n");
            sb.append("                 NGAN HANG TMCP VIETBANK - SAO KE TAI KHOAN             \n");
            sb.append("========================================================================\n");
            sb.append("Chu tai khoan  : ").append(tenChuTk).append("\n");
            sb.append("So tai khoan   : ").append(soTaiKhoan).append("\n");
            sb.append("Thoi gian xuat : ").append(sdf.format(new Date())).append("\n");
            sb.append("Ma ATM / Quay  : VB-DN-086\n");
            sb.append("------------------------------------------------------------------------\n");
            sb.append(String.format("%-20s %-14s %-18s %-18s\n", "Thoi gian", "Loai GD", "So tien (VND)", "So du sau (VND)"));
            sb.append("------------------------------------------------------------------------\n");
            for (int r = 0; r < modelStatement.getRowCount(); r++) {
                String time = String.valueOf(modelStatement.getValueAt(r, 0));
                String type = String.valueOf(modelStatement.getValueAt(r, 1));
                String amt = String.valueOf(modelStatement.getValueAt(r, 2));
                String bal = String.valueOf(modelStatement.getValueAt(r, 3));
                sb.append(String.format("%-20s %-14s %-18s %-18s\n", time, type, amt, bal));
            }
            sb.append("========================================================================\n");
            sb.append("      Cam on Quy khach da su dung dich vu VietBank Digital Banking!     \n");
            sb.append("========================================================================\n");
            java.nio.file.Files.writeString(java.nio.file.Path.of(fname), sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
            if (lblSumInfo != null) {
                lblSumInfo.setText(t("Đã lưu file " + fname + " thành công!", "Saved " + fname + " successfully!"));
                lblSumInfo.setForeground(COLOR_SUCCESS);
            }
        } catch (Exception ex) {
            if (lblSumInfo != null) {
                lblSumInfo.setText(t("Lỗi khi xuất file sao kê!", "Error exporting statement file!"));
                lblSumInfo.setForeground(COLOR_DANGER);
            }
        }
    }

    private JPanel taoManHinhBienLai() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(COLOR_BG_LIGHT);
        main.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        JPanel pSlip = new JPanel(new BorderLayout());
        pSlip.setBackground(Color.WHITE);
        pSlip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 4, 4, COLOR_BADGE_BORDER),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createDashedBorder(COLOR_BADGE_BORDER, 4, 4),
                        BorderFactory.createEmptyBorder(15, 25, 10, 25)
                )
        ));
        pSlip.setPreferredSize(new Dimension(420, 460));

        txtReceipt = new JTextArea();
        txtReceipt.setEditable(false);
        txtReceipt.setFont(new Font("Consolas", Font.PLAIN, 14));
        txtReceipt.setBackground(Color.WHITE);
        txtReceipt.setForeground(COLOR_TEXT_MAIN);
        txtReceipt.setLineWrap(true);
        txtReceipt.setWrapStyleWord(true);

        pSlip.add(txtReceipt, BorderLayout.CENTER);

        JPanel pCenterWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        pCenterWrap.setBackground(COLOR_BG_LIGHT);
        pCenterWrap.add(pSlip);

        JPanel pBottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        pBottom.setBackground(COLOR_BG_LIGHT);

        JButton btnSaveTxt = btn(t("LƯU FILE BIÊN LAI (.TXT)", "SAVE RECEIPT (.TXT)"), COLOR_PRIMARY, Color.WHITE, 0);
        btnSaveTxt.setPreferredSize(new Dimension(220, 46));
        btnSaveTxt.addActionListener(e -> {
            String fname = "bien_lai_" + (lastTxnId != null && !lastTxnId.isEmpty() ? lastTxnId : "ATM") + ".txt";
            try (FileWriter fw = new FileWriter(fname, StandardCharsets.UTF_8)) {
                fw.write(txtReceipt.getText());
                JOptionPane.showMessageDialog(this,
                        t("Đã lưu biên lai vào tệp: " + fname, "Receipt saved successfully to: " + fname),
                        t("Thông báo", "Notice"), JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        t("Lỗi lưu tệp biên lai!", "Error saving receipt file!"),
                        t("Lỗi", "Error"), JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnMenu = btn(t("VỀ MENU CHÍNH", "BACK TO MENU"), COLOR_BADGE_BG, COLOR_PRIMARY, 0);
        btnMenu.setPreferredSize(new Dimension(170, 46));
        btnMenu.addActionListener(e -> hienThi("MENU", t("MENU CHÍNH", "MAIN MENU")));

        JButton btnEject = btn(t("KẾT THÚC / TRẢ THẺ", "FINISH / EJECT CARD"), COLOR_DANGER, Color.WHITE, 0);
        btnEject.setPreferredSize(new Dimension(180, 46));
        btnEject.addActionListener(e -> dangXuat());

        pBottom.add(btnSaveTxt);
        pBottom.add(btnMenu);
        pBottom.add(btnEject);

        main.add(pCenterWrap, BorderLayout.CENTER);
        main.add(pBottom, BorderLayout.SOUTH);
        return main;
    }

    private void hienThiBienLai(String loaiGd, long soTien, long soDuSau, String tkDoiTac, String thoiGian, String txnId, DB.CashBreakdown bd) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("       VIETBANK ATM RECEIPT SLIP        \n");
        sb.append("   NGAN HANG TMCP VIET NAM (VIETBANK)   \n");
        sb.append("========================================\n");
        sb.append("Ma May / ATM ID : ").append(DB.ATM_ID).append("\n");
        sb.append("Ma Giao Dich    : ").append(txnId != null ? txnId : "TXN-000000").append("\n");
        sb.append("Thoi Gian       : ").append(thoiGian).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("Loai Giao Dich  : ").append(boDau(loaiGd).toUpperCase()).append("\n");
        sb.append("Chu Tai Khoan   : ").append(boDau(tenChuTk).toUpperCase()).append("\n");
        sb.append("So Tai Khoan    : ").append(soTaiKhoan).append("\n");
        sb.append("So The          : ").append(dinhDangMaskThe(soThe)).append("\n");
        if (tkDoiTac != null && !"-".equals(tkDoiTac)) {
            sb.append("TK Thu Huong    : ").append(tkDoiTac).append("\n");
        }
        sb.append("So Tien GD      : ").append(NumberFormat.getInstance(Locale.US).format(soTien).replace(',', '.')).append(" VND\n");
        sb.append("Phi Giao Dich   : 0 VND (Mien phi)\n");
        sb.append("So Du Kha Dung  : ").append(NumberFormat.getInstance(Locale.US).format(soDuSau).replace(',', '.')).append(" VND\n");
        if (bd != null && bd.tongTo() > 0) {
            sb.append("----------------------------------------\n");
            sb.append("CO SO TO TIEN NHA RA:\n");
            if (bd.to500k > 0) sb.append("  500.000 VND  x  ").append(bd.to500k).append(" to\n");
            if (bd.to200k > 0) sb.append("  200.000 VND  x  ").append(bd.to200k).append(" to\n");
            if (bd.to100k > 0) sb.append("  100.000 VND  x  ").append(bd.to100k).append(" to\n");
            if (bd.to50k > 0)  sb.append("   50.000 VND  x  ").append(bd.to50k).append(" to\n");
        }
        sb.append("========================================\n");
        sb.append("  CAM ON QUY KHACH DA SU DUNG DICH VU!  \n");
        sb.append("    Hotline Ho Tro 24/7: 1900 6868      \n");
        sb.append("========================================");

        txtReceipt.setText(sb.toString());
        hienThi("RECEIPT", t("BIÊN LAI IN NHIỆT", "TRANSACTION RECEIPT"));
    }

    private JPanel taoNumpad(JTextField target, Runnable onEnter, Runnable onCancel) {
        JPanel keypad = new JPanel(new GridLayout(4, 3, 8, 8));
        keypad.setBackground(COLOR_BADGE_BG);
        keypad.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BADGE_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        keypad.setPreferredSize(new Dimension(300, 250));

        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "HỦY", "0", "XÓA"};
        for (String k : keys) {
            JButton b;
            if ("HỦY".equals(k)) {
                b = btn(t("HỦY", "CANCEL"), COLOR_DANGER, Color.WHITE, 0);
                b.setFont(new Font("Segoe UI", Font.BOLD, 14));
                b.addActionListener(e -> {
                    if (onCancel != null) onCancel.run();
                });
            } else if ("XÓA".equals(k)) {
                b = btn(t("XÓA", "CLEAR"), COLOR_WARNING, Color.WHITE, 0);
                b.setFont(new Font("Segoe UI", Font.BOLD, 14));
                b.addActionListener(e -> {
                    JTextField f = (target != null) ? target : focusedField;
                    if (f != null) {
                        String s = f.getText();
                        if (!s.isEmpty()) f.setText(s.substring(0, s.length() - 1));
                        f.setBorder(borderInputFocus);
                        f.setCaretPosition(f.getText().length());
                    }
                });
            } else {
                b = btn(k, Color.WHITE, COLOR_TEXT_MAIN, 0);
                b.setFont(new Font("Segoe UI", Font.BOLD, 20));
                b.addActionListener(e -> {
                    JTextField f = (target != null) ? target : focusedField;
                    if (f != null) {
                        f.setText(f.getText() + k);
                        f.setBorder(borderInputFocus);
                        f.setCaretPosition(f.getText().length());
                    }
                });
            }
            keypad.add(b);
        }

        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setBackground(COLOR_BG_LIGHT);
        wrap.setPreferredSize(new Dimension(300, 320));
        wrap.add(keypad, BorderLayout.CENTER);

        JButton btnSubmit = btn(t("ĐỒNG Ý (ENTER)", "ENTER / AGREE"), COLOR_PRIMARY, Color.WHITE, 0);
        btnSubmit.setFocusable(false);
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSubmit.setPreferredSize(new Dimension(300, 48));
        btnSubmit.addActionListener(e -> {
            if (onEnter != null) onEnter.run();
        });
        wrap.add(btnSubmit, BorderLayout.SOUTH);

        return wrap;
    }

    private void ganDigitFilter(JTextField field, int maxLen) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = cur.substring(0, offset) + string + cur.substring(offset);
                if (next.matches("\\d*") && next.length() <= maxLen) {
                    super.insertString(fb, offset, string, attr);
                    SwingUtilities.invokeLater(() -> {
                        if (field != null) field.setCaretPosition(field.getText().length());
                    });
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) text = "";
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = cur.substring(0, offset) + text + cur.substring(offset + length);
                if (next.matches("\\d*") && next.length() <= maxLen) {
                    super.replace(fb, offset, length, text, attrs);
                    SwingUtilities.invokeLater(() -> {
                        if (field != null) field.setCaretPosition(field.getText().length());
                    });
                }
            }
        });
    }

    private void ganTienFilter(JTextField field) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                formatAndSet(fb, fb.getDocument().getText(0, fb.getDocument().getLength()) + string);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String before = cur.substring(0, offset);
                String after = cur.substring(offset + length);
                formatAndSet(fb, before + (text == null ? "" : text) + after);
            }

            private void formatAndSet(FilterBypass fb, String raw) throws BadLocationException {
                String d = raw.replaceAll("\\D", "");
                if (d.length() > 9) d = d.substring(0, 9);
                if (d.isEmpty()) {
                    fb.replace(0, fb.getDocument().getLength(), "", null);
                    return;
                }
                try {
                    long val = Long.parseLong(d);
                    String formatted = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(val);
                    fb.replace(0, fb.getDocument().getLength(), formatted, null);
                } catch (Exception ex) {
                    fb.replace(0, fb.getDocument().getLength(), d, null);
                }
                SwingUtilities.invokeLater(() -> {
                    if (field != null) field.setCaretPosition(field.getText().length());
                });
            }
        });
    }

    private String dinhDangMaskThe(String raw) {
        String clean = raw.replaceAll("\\s+", "");
        if (clean.length() == 16) {
            return clean.substring(0, 4) + " **** **** " + clean.substring(12);
        }
        return clean;
    }

    private void dangXuat() {
        timer1s.stop();
        soThe = "";
        accId = 0;
        tenChuTk = "";
        soTaiKhoan = "";
        giayConLai = DB.TIMEOUT_GIAY;
        focusedField = null;
        hienThi("WAIT", t("SẴN SÀNG PHỤC VỤ", "READY TO SERVE"));
        capNhatTrangThaiCSDL();
    }

    private void xuLyTimeout() {
        dangXuat();
        JOptionPane.showMessageDialog(this,
                t("Phiên giao dịch đã tự động kết thúc (" + DB.TIMEOUT_GIAY + " giây) do không có thao tác.\nThẻ ngân hàng đã được hoàn trả an toàn.",
                  "Session timed out (" + DB.TIMEOUT_GIAY + "s) due to inactivity.\nYour card has been safely ejected."),
                t("Hết thời gian phiên", "Session Timed Out"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void hienThiDialogCauHinhDB() {
        JPanel p = new JPanel(new GridLayout(2, 2, 10, 10));
        JTextField txtUser = new JTextField(DB.getUser());
        JPasswordField txtPass = new JPasswordField(DB.getPass());
        p.add(new JLabel("MySQL User:"));
        p.add(txtUser);
        p.add(new JLabel("MySQL Password:"));
        p.add(txtPass);

        int opt = JOptionPane.showConfirmDialog(this, p, t("Cấu Hình & Tự Động Tạo CSDL atm_db", "Configure & Auto-create atm_db"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            String u = txtUser.getText().trim();
            String pwd = new String(txtPass.getPassword());
            String res = DB.khoiTaoCSDL(u, pwd);
            if ("OK".equals(res)) {
                capNhatTrangThaiCSDL();
                JOptionPane.showMessageDialog(this, t("Kết nối và khởi tạo CSDL atm_db thành công!\nDữ liệu 3 thẻ mẫu đã sẵn sàng hoạt động.", "Connected and initialized atm_db successfully!"), t("Thành Công", "Success"), JOptionPane.INFORMATION_MESSAGE);
            } else {
                capNhatTrangThaiCSDL();
                JOptionPane.showMessageDialog(this, t("Kết nối thất bại: ", "Connection failed: ") + res + t("\nVui lòng kiểm tra lại tài khoản hoặc mật khẩu MySQL!", "\nPlease verify MySQL credentials!"), t("Lỗi Kết Nối", "Connection Error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            ATM app = new ATM();
            app.setVisible(true);
        });
    }
}
