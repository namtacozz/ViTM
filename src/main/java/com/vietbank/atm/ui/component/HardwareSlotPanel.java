package com.vietbank.atm.ui.component;

import com.vietbank.atm.ui.theme.Colors;
import com.vietbank.atm.util.I18n;

import javax.swing.*;
import java.awt.*;

public class HardwareSlotPanel extends JPanel {
    public enum ActiveSlot { NONE, CARD, CASH, RECEIPT }

    private static final String[] KEYS = {"bezel.card_slot", "bezel.cash_slot", "bezel.receipt_slot"};
    private static final int[] HEIGHTS = {10, 14, 8};

    private final JPanel[] boxes = new JPanel[3];
    private final JLabel[] labels = new JLabel[3];
    private final JPanel[] leds = new JPanel[3];
    private ActiveSlot activeSlot = ActiveSlot.NONE;
    private boolean blinkOn = false;

    public HardwareSlotPanel() {
        setLayout(new GridLayout(1, 3, 24, 0));
        setBackground(Colors.BEZEL_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 80, 12, 80));

        for (int i = 0; i < 3; i++) {
            leds[i] = new JPanel();
            leds[i].setPreferredSize(new Dimension(12, 12));
            leds[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

            labels[i] = new JLabel(I18n.get(KEYS[i]), SwingConstants.CENTER);
            labels[i].setFont(new Font("Segoe UI", Font.BOLD, 11));
            labels[i].setForeground(new Color(209, 213, 219));

            JPanel top = new JPanel(new BorderLayout(6, 0));
            top.setOpaque(false);
            top.add(labels[i], BorderLayout.CENTER);
            top.add(leds[i], BorderLayout.EAST);

            JPanel slit = new JPanel();
            slit.setPreferredSize(new Dimension(100, HEIGHTS[i]));
            slit.setBackground(new Color(15, 17, 20));
            slit.setBorder(BorderFactory.createLineBorder(new Color(90, 95, 105), 1));

            boxes[i] = new JPanel(new BorderLayout(6, 4));
            boxes[i].setBackground(Colors.BEZEL_INNER);
            boxes[i].add(top, BorderLayout.NORTH);
            boxes[i].add(slit, BorderLayout.CENTER);
            add(boxes[i]);
        }

        updateVisuals();
        new Timer(450, e -> { blinkOn = !blinkOn; updateVisuals(); }).start();
    }

    public void setActiveSlot(ActiveSlot slot) {
        this.activeSlot = slot;
        updateVisuals();
    }

    public void refreshLabels() {
        for (int i = 0; i < 3; i++) labels[i].setText(I18n.get(KEYS[i]));
    }

    private void updateVisuals() {
        int activeIdx = switch (activeSlot) { case CARD -> 0; case CASH -> 1; case RECEIPT -> 2; case NONE -> -1; };
        for (int i = 0; i < 3; i++) {
            boolean active = i == activeIdx;
            Color border = active ? (blinkOn ? Colors.PRIMARY : Colors.ACCENT_BORDER) : new Color(75, 85, 99);
            int pad = active ? 5 : 6;
            boxes[i].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, active ? 2 : 1, true),
                BorderFactory.createEmptyBorder(pad, pad + 5, pad + 2, pad + 5)));
            leds[i].setBackground(active ? (blinkOn ? Colors.PRIMARY : Colors.SUCCESS) : new Color(75, 85, 99));
        }
    }
}
