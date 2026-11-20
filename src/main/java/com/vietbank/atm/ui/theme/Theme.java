package com.vietbank.atm.ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;

public class Theme {
    public static final Border BORDER_INPUT_DEFAULT = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Colors.BORDER_DEFAULT, 1, true), BorderFactory.createEmptyBorder(8, 12, 8, 12));
    public static final Border BORDER_INPUT_FOCUS = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Colors.PRIMARY, 2, true), BorderFactory.createEmptyBorder(7, 11, 7, 11));
    public static final Border BORDER_CARD = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Colors.ACCENT_BORDER, 1, true), BorderFactory.createEmptyBorder(16, 20, 16, 20));

    private static JButton baseBtn(String text, Color bg, Color fg, Border border) {
        JButton b = new JButton(text);
        b.setFont(Fonts.BTN);
        b.setOpaque(true);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setBorder(border);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton createPrimaryButton(String text) {
        return baseBtn(text, Colors.PRIMARY, Color.WHITE, BorderFactory.createEmptyBorder(10, 18, 10, 18));
    }

    public static JButton createSecondaryButton(String text) {
        return baseBtn(text, Color.WHITE, Colors.TEXT_MAIN, BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Colors.BORDER_DEFAULT, 1, true), BorderFactory.createEmptyBorder(9, 16, 9, 16)));
    }

    public static JButton createDangerButton(String text) {
        return baseBtn(text, Colors.DANGER, Color.WHITE, BorderFactory.createEmptyBorder(10, 18, 10, 18));
    }

    public static JButton createAtmMenuButton(String text, boolean isLeft) {
        Border accent = BorderFactory.createMatteBorder(0, isLeft ? 6 : 0, 0, isLeft ? 0 : 6, Colors.PRIMARY);
        Border border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Colors.ACCENT_BORDER, 1),
            BorderFactory.createCompoundBorder(accent, BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        JButton b = baseBtn(text, Color.WHITE, Colors.TEXT_MAIN, border);
        b.setHorizontalAlignment(isLeft ? SwingConstants.LEFT : SwingConstants.RIGHT);
        return b;
    }

    public static JProgressBar createOrangeProgressBar() {
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new Dimension(420, 14));
        bar.setBackground(Colors.ACCENT_BG);
        bar.setForeground(Colors.PRIMARY);
        bar.setBorder(BorderFactory.createLineBorder(Colors.ACCENT_BORDER, 1, true));
        bar.setUI(new BasicProgressBarUI() {
            @Override protected Color getSelectionBackground() { return Colors.TEXT_MAIN; }
            @Override protected Color getSelectionForeground() { return Color.WHITE; }
        });
        return bar;
    }

    public static void applyFocusStyle(JTextField field, boolean focused) {
        field.setBorder(focused ? BORDER_INPUT_FOCUS : BORDER_INPUT_DEFAULT);
        field.setBackground(focused ? Colors.ACCENT_BG : Color.WHITE);
    }
}
