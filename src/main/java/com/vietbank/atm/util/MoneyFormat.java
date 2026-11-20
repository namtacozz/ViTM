package com.vietbank.atm.util;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyFormat {
    public static String formatMoney(long amount) {
        return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " VNĐ";
    }

    public static String maskCardNumber(String cardNo) {
        String clean = cardNo == null ? "" : cardNo.replaceAll("\\s+", "");
        return clean.length() < 8 ? clean : clean.substring(0, 4) + " •••• •••• " + clean.substring(clean.length() - 4);
    }

    public static String maskAccountNumber(String accNo) {
        String clean = accNo == null ? "" : accNo.trim();
        return clean.length() <= 4 ? clean : "••••••" + clean.substring(clean.length() - 4);
    }

    public static String toAsciiUpperName(String input) {
        if (input == null) return "";
        String noAccent = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccent.replace('Đ', 'D').replace('đ', 'd').toUpperCase(Locale.ROOT).trim();
    }

    public static String formatCardInput(String digits) {
        String clean = digits == null ? "" : digits.replaceAll("\\D", "");
        if (clean.length() > 16) clean = clean.substring(0, 16);
        return clean.replaceAll("(.{4})(?!$)", "$1 ");
    }
}
