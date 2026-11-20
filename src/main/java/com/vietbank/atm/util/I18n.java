package com.vietbank.atm.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {
    private static Locale locale = Locale.forLanguageTag("vi");
    private static ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);

    public static void setEnglish(boolean en) {
        locale = Locale.forLanguageTag(en ? "en" : "vi");
        bundle = ResourceBundle.getBundle("i18n.messages", locale);
    }

    public static boolean isEnglish() {
        return "en".equals(locale.getLanguage());
    }

    public static String get(String key, Object... args) {
        String pattern = bundle.containsKey(key) ? bundle.getString(key) : key;
        return (args == null || args.length == 0) ? pattern : MessageFormat.format(pattern, args);
    }
}
