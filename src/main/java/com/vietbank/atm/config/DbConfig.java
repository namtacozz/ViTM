package com.vietbank.atm.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.Properties;

public class DbConfig {
    private static String url = AppConfig.getString("db.url", "jdbc:mysql://localhost:3306/atm_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&connectTimeout=3000&socketTimeout=5000");
    private static String user = AppConfig.getString("db.user", "root");
    private static String pass = AppConfig.getString("db.password", "");

    static { loadOverrides(); }

    public static void loadOverrides() {
        if (System.getenv("DB_URL") != null) url = System.getenv("DB_URL");
        if (System.getenv("DB_USER") != null) user = System.getenv("DB_USER");
        if (System.getenv("DB_PASS") != null) pass = System.getenv("DB_PASS");
        File f = new File("db.properties");
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                Properties p = new Properties();
                p.load(fis);
                url = p.getProperty("db.url", url);
                user = p.getProperty("db.user", user);
                pass = p.getProperty("db.password", pass);
            } catch (Exception ignored) {}
        }
    }

    public static void saveConfig(String newUser, String newPass) {
        user = newUser;
        pass = newPass;
        try (FileOutputStream fos = new FileOutputStream("db.properties")) {
            Properties p = new Properties();
            p.setProperty("db.url", url);
            p.setProperty("db.user", user);
            p.setProperty("db.password", pass);
            p.store(fos, null);
        } catch (Exception ignored) {}
    }

    public static void setTestConnection(String testUrl, String testUser, String testPass) {
        url = testUrl;
        user = testUser;
        pass = testPass;
    }

    public static String getUser() { return user; }
    public static String getPass() { return pass; }
    public static Connection getConnection() throws SQLException { return DriverManager.getConnection(url, user, pass); }

    public static boolean testConnection() {
        try (Connection c = getConnection()) { return c != null && !c.isClosed(); }
        catch (Exception e) { return false; }
    }

    public static String initDatabase(String rootUser, String rootPass) {
        String rootUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&connectTimeout=3000";
        try (Connection c = DriverManager.getConnection(rootUrl, rootUser, rootPass); Statement st = c.createStatement()) {
            executeSqlFile(st, new File("database/schema.sql"));
            executeSqlFile(st, new File("database/seed.sql"));
            saveConfig(rootUser, rootPass);
            return "OK";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private static void executeSqlFile(Statement st, File file) throws Exception {
        if (!file.exists()) throw new IllegalStateException("Missing SQL file: " + file.getPath());
        for (String stmt : Files.readString(file.toPath(), StandardCharsets.UTF_8).split(";\\s*(\\r?\\n|$)")) {
            if (!stmt.trim().isEmpty()) st.execute(stmt.trim());
        }
    }
}
