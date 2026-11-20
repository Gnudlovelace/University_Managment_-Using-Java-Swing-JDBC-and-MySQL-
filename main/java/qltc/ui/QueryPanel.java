package qltc.ui;

import qltc.db.Sql;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.*;

/**
 * Màn hình báo cáo: ô chọn tham số + bảng kết quả.
 * Trong câu SQL, tham số viết dạng :ten (có thể dùng lại nhiều lần); khi chạy được đổi thành ? của PreparedStatement.
 */
public class QueryPanel extends JPanel {
    public static final class Param {
        enum Kind { COMBO, TEXT, NUMBER, FIXED }
        final String name, label;
        final Kind kind;
        String sql, def = "";
        Item[] items;
        Supplier<Object> fixed;

        private Param(String name, String label, Kind kind) { this.name = name; this.label = label; this.kind = kind; }
        public static Param combo(String name, String label, String sql) { Param p = new Param(name, label, Kind.COMBO); p.sql = sql; return p; }
        public static Param options(String name, String label, Item... items) { Param p = new Param(name, label, Kind.COMBO); p.items = items; return p; }
        public static Param text(String name, String label, String def) { Param p = new Param(name, label, Kind.TEXT); p.def = def; return p; }
        public static Param number(String name, String label, String def) { Param p = new Param(name, label, Kind.NUMBER); p.def = def; return p; }
        public static Param fixed(String name, Supplier<Object> v) { Param p = new Param(name, "", Kind.FIXED); p.fixed = v; return p; }
        /** Giá trị mặc định (với combo: value của mục được chọn sẵn). */
        public Param def(String d) { this.def = d; return this; }
    }

    private static final Pattern NAMED = Pattern.compile("(?<![:\\w]):([a-z_][a-z0-9_]*)");

    private final String sql;
    private final Map<String, Param> params = new LinkedHashMap<>();
    private final Map<String, JComponent> inputs = new HashMap<>();
    private final JTable table = Ui.table();
    private final JLabel status = Ui.muted(" ");
    private final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    private String[] verticalLabels; // != null: hiển thị theo chiều dọc (mỗi cột là một dòng)

    public QueryPanel(String title, String sql, Param... ps) {
        this.sql = sql;
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        north.add(Ui.title(title));

        bar.setOpaque(false);
        for (Param p : ps) {
            params.put(p.name, p);
            if (p.kind == Param.Kind.FIXED) continue;
            bar.add(new JLabel(p.label + ":"));
            JComponent c = makeInput(p);
            inputs.put(p.name, c);
            bar.add(c);
        }
        JButton run = Ui.button("Xem kết quả", true);
        run.addActionListener(ev -> run());
        bar.add(run);
        north.add(bar);
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        run();
    }

    /** Thêm một nút chức năng cạnh nút "Xem kết quả". */
    public QueryPanel withAction(String text, java.util.function.Consumer<QueryPanel> action) {
        JButton b = Ui.button(text, false);
        b.addActionListener(ev -> action.accept(this));
        bar.add(b);
        bar.revalidate();
        return this;
    }

    /** Hiển thị kết quả theo chiều dọc: mỗi cột của dòng đầu tiên thành một dòng "Thông tin | Giá trị". */
    public QueryPanel vertical(String... labels) {
        this.verticalLabels = labels;
        run();
        return this;
    }

    private Sql.Table transpose(Sql.Table t) {
        List<Object[]> rows = new ArrayList<>();
        if (!t.rows().isEmpty()) {
            Object[] r = t.rows().get(0);
            for (int i = 0; i < t.cols().size(); i++) {
                String label = i < verticalLabels.length ? verticalLabels[i] : t.cols().get(i);
                rows.add(new Object[]{label, r[i]});
            }
        }
        return new Sql.Table(List.of("Thông tin", "Giá trị"), rows);
    }

    private JComponent makeInput(Param p) {
        if (p.kind == Param.Kind.COMBO) {
            JComboBox<Item> cb = new JComboBox<>();
            try {
                if (p.items != null) for (Item i : p.items) cb.addItem(i);
                else for (Object[] r : Sql.query(p.sql).rows()) cb.addItem(new Item(String.valueOf(r[0]), String.valueOf(r[1])));
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
            for (int i = 0; i < cb.getItemCount(); i++) if (cb.getItemAt(i).value().equals(p.def)) cb.setSelectedIndex(i);
            cb.setPreferredSize(new Dimension(Math.max(160, Math.min(380, cb.getPreferredSize().width + 20)), 28));
            return cb;
        }
        JTextField t = new JTextField(p.def, 10);
        t.addActionListener(ev -> run());
        return t;
    }

    private Object value(String name) {
        Param p = params.get(name);
        if (p == null) throw new IllegalStateException("Thiếu tham số :" + name);
        JComponent c = inputs.get(name);
        switch (p.kind) {
            case FIXED: return p.fixed.get();
            case COMBO: {
                Object it = ((JComboBox<?>) c).getSelectedItem();
                if (it == null) throw new IllegalArgumentException("Chưa có dữ liệu để chọn cho \"" + p.label + "\".");
                return ((Item) it).value();
            }
            case NUMBER: {
                try { return new BigDecimal(((JTextField) c).getText().trim().replace(',', '.')); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("\"" + p.label + "\" phải là số."); }
            }
            default: return ((JTextField) c).getText().trim();
        }
    }

    public void run() {
        try {
            Matcher m = NAMED.matcher(sql);
            StringBuilder out = new StringBuilder();
            List<Object> args = new ArrayList<>();
            while (m.find()) {
                args.add(value(m.group(1)));
                m.appendReplacement(out, "?");
            }
            m.appendTail(out);
            Sql.Table t = Sql.query(out.toString(), args.toArray());
            if (verticalLabels != null) {
                Ui.fill(table, transpose(t));
                table.getColumnModel().getColumn(0).setMaxWidth(180);
                status.setText(t.rows().isEmpty() ? "Không có dữ liệu" : " ");
            } else {
                Ui.fill(table, t);
                status.setText(t.rows().size() + " dòng");
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Tham số chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }
}