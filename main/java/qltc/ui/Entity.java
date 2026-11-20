package qltc.ui;

import java.util.*;

/** Mô tả một bảng cho màn hình CRUD: tên bảng, tiêu đề, các trường. */
public final class Entity {
    public enum Kind { TEXT, INT, DEC, DATE, BOOL, FK, ENUM }

    public static final class F {
        public final String col, label;
        public final Kind kind;
        public boolean pk, req;
        public String sql;      // FK: câu SQL trả (giá trị, nhãn)
        public String[] opts;   // ENUM: các giá trị

        private F(String col, String label, Kind kind) { this.col = col; this.label = label; this.kind = kind; }
        public static F text(String c, String l) { return new F(c, l, Kind.TEXT); }
        public static F integer(String c, String l) { return new F(c, l, Kind.INT); }
        public static F dec(String c, String l) { return new F(c, l, Kind.DEC); }
        public static F date(String c, String l) { return new F(c, l, Kind.DATE); }
        public static F bool(String c, String l) { F f = new F(c, l, Kind.BOOL); f.req = true; return f; }
        public static F fk(String c, String l, String sql) { F f = new F(c, l, Kind.FK); f.sql = sql; return f; }
        public static F en(String c, String l, String... o) { F f = new F(c, l, Kind.ENUM); f.opts = o; return f; }
        public F pk() { pk = true; req = true; return this; }
        public F req() { req = true; return this; }
    }

    public final String table, title;
    public final List<F> fields;
    /** Chạy sau mỗi lần thêm/sửa/xóa thành công (ví dụ cấp tài khoản cho sinh viên mới). */
    public Runnable after = () -> {};

    public Entity(String table, String title, F... fields) {
        this.table = table;
        this.title = title;
        this.fields = List.of(fields);
    }

    public String cols() {
        StringJoiner j = new StringJoiner(", ");
        fields.forEach(f -> j.add(f.col));
        return j.toString();
    }

    public String order() {
        StringJoiner j = new StringJoiner(", ");
        fields.stream().filter(f -> f.pk).forEach(f -> j.add(f.col));
        return j.length() == 0 ? fields.get(0).col : j.toString();
    }
}
