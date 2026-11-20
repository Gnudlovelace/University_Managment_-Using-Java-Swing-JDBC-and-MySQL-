package qltc.db;

import java.sql.*;
import java.util.*;

/** Tiện ích chạy câu lệnh bằng PreparedStatement; mọi tham số đều truyền qua dấu ?. */
public final class Sql {
    private Sql() {}

    /** Kết quả truy vấn: tên cột + các dòng (NULL giữ nguyên là null). */
    public record Table(List<String> cols, List<Object[]> rows) {}

    private static void bind(PreparedStatement ps, Object[] p) throws SQLException {
        for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
    }

    public static Table query(String sql, Object... p) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                List<String> cols = new ArrayList<>();
                for (int i = 1; i <= n; i++) cols.add(md.getColumnLabel(i));
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    Object[] r = new Object[n];
                    for (int i = 0; i < n; i++) r[i] = rs.getObject(i + 1);
                    rows.add(r);
                }
                return new Table(cols, rows);
            }
        }
    }

    public static int update(String sql, Object... p) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            return ps.executeUpdate();
        }
    }

    public static Object scalar(String sql, Object... p) throws SQLException {
        Table t = query(sql, p);
        return t.rows().isEmpty() ? null : t.rows().get(0)[0];
    }

    /** Đổi lỗi kỹ thuật thành thông báo dễ hiểu. Lỗi từ trigger (SQLState 45000) giữ nguyên nội dung. */
    public static String explain(Throwable t) {
        for (Throwable x = t; x != null; x = x.getCause()) {
            if (x instanceof SQLException e) {
                if ("45000".equals(e.getSQLState())) return e.getMessage();
                return switch (e.getErrorCode()) {
                    case 1062 -> "Dữ liệu bị trùng (khóa chính hoặc giá trị UNIQUE đã tồn tại).";
                    case 1451 -> "Không thể xóa/sửa vì bản ghi này đang được dữ liệu khác tham chiếu.";
                    case 1452 -> "Giá trị khóa ngoại không tồn tại trong bảng được tham chiếu.";
                    case 3819 -> "Dữ liệu vi phạm ràng buộc CHECK: " + e.getMessage();
                    case 1048 -> "Thiếu dữ liệu bắt buộc: " + e.getMessage();
                    case 1292, 1366, 1265 -> "Giá trị không đúng định dạng/kiểu dữ liệu.";
                    case 1406 -> "Giá trị quá dài so với độ dài cột.";
                    case 1045 -> "Sai tên đăng nhập hoặc mật khẩu MySQL (kiểm tra db.properties).";
                    case 1049 -> "Chưa có database (kiểm tra db.url trong db.properties).";
                    default -> e.getSQLState() != null && e.getSQLState().startsWith("08")
                            ? "Không kết nối được tới MySQL server (kiểm tra db.properties, MySQL đã chạy chưa)."
                            : "Lỗi CSDL: " + e.getMessage();
                };
            }
        }
        return t.getMessage() != null ? t.getMessage() : t.toString();
    }
}
