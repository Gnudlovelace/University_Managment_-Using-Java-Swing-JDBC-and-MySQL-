package qltc.db;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;

/** Đọc db.properties và cấp kết nối JDBC. */
public final class DB {
    private static Properties props;

    private DB() {}

    private static synchronized Properties props() {
        if (props == null) {
            Properties p = new Properties();
            File f = new File("db.properties");
            try (InputStream in = f.exists() ? new FileInputStream(f) : DB.class.getResourceAsStream("/db.properties")) {
                if (in == null) throw new IllegalStateException("Không tìm thấy file db.properties");
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("Không đọc được db.properties: " + e.getMessage(), e);
            }
            props = p;
        }
        return props;
    }

    public static Connection get() throws SQLException {
        Properties p = props();
        String url = p.getProperty("db.url", "").trim();
        if (url.isEmpty()) throw new IllegalStateException("db.url trong db.properties đang để trống");
        return DriverManager.getConnection(url, p.getProperty("db.user", "").trim(), p.getProperty("db.password", ""));
    }
}
