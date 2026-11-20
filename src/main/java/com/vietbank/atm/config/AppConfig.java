package com.vietbank.atm.config;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream is = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}
    }

    public static String getString(String key, String def) {
        return props.getProperty(key, def);
    }

    public static long getLong(String key, long def) {
        try { return Long.parseLong(props.getProperty(key, String.valueOf(def))); }
        catch (Exception e) { return def; }
    }

    public static int getInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key, String.valueOf(def))); }
        catch (Exception e) { return def; }
    }
}
