package qltc.auth;

import qltc.db.Sql;

import java.sql.SQLException;

/**
 * Đăng nhập: sinh viên dùng email trong bảng sinh_vien, quản lý dùng tên 'admin'.
 * Mật khẩu lưu dạng văn bản thường (đơn giản hóa cho bài tập lớn). Mật khẩu khởi tạo của sinh viên = DEFAULT_PW (1234).
 */
public final class Auth {
    public record User(int id, String role, String maSv, String ten) {
        public boolean isAdmin() { return "admin".equals(role); }
    }

    /** Mật khẩu khởi tạo cho mọi tài khoản sinh viên mới. */
    public static final String DEFAULT_PW = "1234";
    private static User current;

    private Auth() {}

    public static User current() { return current; }
    public static void logout() { current = null; }

    /** Tạo bảng tai_khoan nếu chưa có, tạo admin mặc định, cấp tài khoản cho sinh viên chưa có. */
    public static void init() throws SQLException {
        Sql.update("""
            CREATE TABLE IF NOT EXISTS tai_khoan (
                id INT AUTO_INCREMENT PRIMARY KEY,
                ten_admin VARCHAR(50) UNIQUE,
                ma_sv VARCHAR(10) UNIQUE,
                mat_khau VARCHAR(50) NOT NULL,
                vai_tro VARCHAR(10) NOT NULL,
                CONSTRAINT ck_tai_khoan_vai_tro CHECK (vai_tro IN ('admin', 'sinh_vien')),
                CONSTRAINT fk_tai_khoan_sinh_vien FOREIGN KEY (ma_sv)
                    REFERENCES sinh_vien(ma_sv) ON DELETE CASCADE ON UPDATE CASCADE
            )""");
        Object n = Sql.scalar("SELECT COUNT(*) FROM tai_khoan WHERE ten_admin = 'admin'");
        if (((Number) n).intValue() == 0) {
            Sql.update("INSERT INTO tai_khoan (ten_admin, mat_khau, vai_tro) VALUES ('admin', ?, 'admin')",
                    "admin123");
        }
        provision();
    }

    /** Cấp tài khoản cho mọi sinh viên có email mà chưa có tài khoản. Trả về số tài khoản mới. */
    public static int provision() throws SQLException {
        Sql.Table t = Sql.query("""
            SELECT sv.ma_sv FROM sinh_vien sv
            LEFT JOIN tai_khoan t ON t.ma_sv = sv.ma_sv
            WHERE t.id IS NULL AND sv.email IS NOT NULL AND TRIM(sv.email) <> ''""");
        for (Object[] r : t.rows()) {
            String ma = (String) r[0];
            Sql.update("INSERT INTO tai_khoan (ma_sv, mat_khau, vai_tro) VALUES (?, ?, 'sinh_vien')", ma, DEFAULT_PW);
        }
        return t.rows().size();
    }

    /** Đặt lại mật khẩu của TẤT CẢ sinh viên về DEFAULT_PW Trả về số tài khoản bị đổi. */
    public static int resetAllStudentPasswords() throws SQLException {
        Sql.Table t = Sql.query("SELECT id FROM tai_khoan WHERE vai_tro = 'sinh_vien'");
        for (Object[] r : t.rows()) {
            Sql.update("UPDATE tai_khoan SET mat_khau = ? WHERE id = ?",
                    DEFAULT_PW, ((Number) r[0]).intValue());
        }
        return t.rows().size();
    }

    /** Trả về User nếu đúng, null nếu sai. */
    public static User login(String name, String password) throws SQLException {
        Sql.Table t = Sql.query("""
            SELECT t.id, t.vai_tro, t.ma_sv, t.mat_khau, COALESCE(sv.ten, t.ten_admin)
            FROM tai_khoan t LEFT JOIN sinh_vien sv ON sv.ma_sv = t.ma_sv
            WHERE t.ten_admin = ? OR sv.email = ?""", name, name);
        for (Object[] r : t.rows()) {
            if (password.equals(r[3])) {
                current = new User(((Number) r[0]).intValue(), (String) r[1], (String) r[2], (String) r[4]);
                return current;
            }
        }
        return null;
    }

    public static void changePassword(int id, String oldPw, String newPw) throws SQLException {
        Object h = Sql.scalar("SELECT mat_khau FROM tai_khoan WHERE id = ?", id);
        if (h == null || !oldPw.equals(h)) throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        if (newPw.length() < 6) throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        if (newPw.equals(oldPw)) throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu cũ.");
        Sql.update("UPDATE tai_khoan SET mat_khau = ? WHERE id = ?", newPw, id);
    }
}