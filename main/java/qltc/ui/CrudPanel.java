package qltc.ui;

import qltc.db.Sql;
import qltc.ui.Entity.F;
import qltc.ui.Entity.Kind;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

/** Màn hình quản lý một bảng: tìm kiếm, phân trang, thêm / sửa / xóa. */
public class CrudPanel extends JPanel {
    private static final int SIZE = 15;

    private final Entity e;
    private final JTable table = Ui.table();
    private final JTextField search = new JTextField(20);
    private final JLabel pageLbl = new JLabel();
    private List<Object[]> rows = new ArrayList<>();
    private int page = 0, pages = 1;

    public CrudPanel(Entity e) {
        this.e = e;
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(Ui.title("Quản lý: " + e.title), BorderLayout.NORTH);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        JButton find = Ui.button("Tìm", false), add = Ui.button("+ Thêm", true),
                edit = Ui.button("Sửa", false), del = Ui.button("Xóa", false);
        bar.add(search); bar.add(find); bar.add(add); bar.add(edit); bar.add(del);
        north.add(bar, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        south.setOpaque(false);
        JButton prev = Ui.button("◀", false), next = Ui.button("▶", false);
        south.add(prev); south.add(pageLbl); south.add(next);
        add(south, BorderLayout.SOUTH);

        find.addActionListener(ev -> { page = 0; load(); });
        search.addActionListener(ev -> { page = 0; load(); });
        prev.addActionListener(ev -> { if (page > 0) { page--; load(); } });
        next.addActionListener(ev -> { if (page < pages - 1) { page++; load(); } });
        add.addActionListener(ev -> openForm(null));
        edit.addActionListener(ev -> editSelected());
        del.addActionListener(ev -> deleteSelected());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent ev) { if (ev.getClickCount() == 2) editSelected(); }
        });
        load();
    }

    private void load() {
        try {
            String where = "";
            List<Object> args = new ArrayList<>();
            String kw = search.getText().trim();
            if (!kw.isEmpty()) {
                where = " WHERE CONCAT_WS(' ', " + e.cols() + ") LIKE ?";
                args.add("%" + kw + "%");
            }
            int total = ((Number) Sql.scalar("SELECT COUNT(*) FROM " + e.table + where, args.toArray())).intValue();
            pages = Math.max(1, (total + SIZE - 1) / SIZE);
            page = Math.min(page, pages - 1);
            args.add(SIZE);
            args.add(page * SIZE);
            Sql.Table t = Sql.query("SELECT " + e.cols() + " FROM " + e.table + where
                    + " ORDER BY " + e.order() + " LIMIT ? OFFSET ?", args.toArray());
            rows = t.rows();
            List<String> labels = new ArrayList<>();
            e.fields.forEach(f -> labels.add(f.label));
            Ui.fill(table, new Sql.Table(labels, rows));
            pageLbl.setText("Trang " + (page + 1) + " / " + pages + "   (" + total + " bản ghi)");
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }

    private Object[] selected() {
        int i = table.getSelectedRow();
        if (i < 0) { Ui.info(this, "Hãy chọn một dòng trong bảng."); return null; }
        return rows.get(i);
    }

    private void editSelected() {
        Object[] r = selected();
        if (r != null) openForm(r);
    }

    private void deleteSelected() {
        Object[] r = selected();
        if (r == null || !Ui.confirm(this, "Xóa bản ghi đã chọn?")) return;
        try {
            StringJoiner w = new StringJoiner(" AND ");
            List<Object> args = new ArrayList<>();
            for (int i = 0; i < e.fields.size(); i++) {
                if (e.fields.get(i).pk) { w.add(e.fields.get(i).col + " = ?"); args.add(r[i]); }
            }
            Sql.update("DELETE FROM " + e.table + " WHERE " + w, args.toArray());
            e.after.run();
            load();
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }

    private void openForm(Object[] row) {
        Form f = new Form(SwingUtilities.getWindowAncestor(this), e, row);
        f.setVisible(true);
        if (f.saved) load();
    }

    /** Hộp thoại thêm/sửa. */
    private static final class Form extends JDialog {
        boolean saved;
        private final Entity e;
        private final Object[] row;
        private final JComponent[] inputs;

        Form(Window owner, Entity e, Object[] row) {
            super(owner, (row == null ? "Thêm " : "Sửa ") + e.title.toLowerCase(), ModalityType.APPLICATION_MODAL);
            this.e = e;
            this.row = row;
            inputs = new JComponent[e.fields.size()];

            JPanel p = new JPanel(new GridBagLayout());
            p.setBorder(new EmptyBorder(16, 18, 8, 18));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(5, 4, 5, 4);
            g.anchor = GridBagConstraints.WEST;
            for (int i = 0; i < inputs.length; i++) {
                F f = e.fields.get(i);
                g.gridx = 0; g.gridy = i; g.fill = GridBagConstraints.NONE;
                p.add(new JLabel(f.label + (f.req ? " *" : "")), g);
                inputs[i] = makeInput(f, row == null ? null : row[i]);
                inputs[i].setPreferredSize(new Dimension(280, 28));
                if (row != null && f.pk) inputs[i].setEnabled(false);
                g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL;
                p.add(inputs[i], g);
            }
            JPanel btn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            JButton ok = Ui.button("Lưu", true), cancel = Ui.button("Hủy", false);
            btn.add(cancel); btn.add(ok);
            ok.addActionListener(ev -> save());
            cancel.addActionListener(ev -> dispose());
            getRootPane().setDefaultButton(ok);
            add(p, BorderLayout.CENTER);
            add(btn, BorderLayout.SOUTH);
            pack();
            setLocationRelativeTo(owner);
        }

        private JComponent makeInput(F f, Object raw) {
            try {
                switch (f.kind) {
                    case FK, ENUM, BOOL -> {
                        JComboBox<Item> cb = new JComboBox<>();
                        if (!f.req) cb.addItem(new Item("", ""));
                        if (f.kind == Kind.FK) {
                            for (Object[] r : Sql.query(f.sql).rows()) cb.addItem(new Item(String.valueOf(r[0]), String.valueOf(r[1])));
                        } else if (f.kind == Kind.ENUM) {
                            for (String o : f.opts) cb.addItem(new Item(o, o));
                        } else {
                            cb.addItem(new Item("1", "Có"));
                            cb.addItem(new Item("0", "Không"));
                        }
                        if (raw != null) {
                            String v = raw instanceof Boolean b ? (b ? "1" : "0") : raw.toString();
                            for (int i = 0; i < cb.getItemCount(); i++) if (cb.getItemAt(i).value().equals(v)) cb.setSelectedIndex(i);
                        }
                        return cb;
                    }
                    default -> {
                        JTextField t = new JTextField(raw == null ? "" : raw.toString());
                        if (f.kind == Kind.DATE) t.setToolTipText("Định dạng yyyy-MM-dd, ví dụ 2006-01-10");
                        return t;
                    }
                }
            } catch (Exception ex) {
                Ui.error(this, ex);
                return new JTextField();
            }
        }

        private Object value(F f, JComponent c) {
            String s;
            if (c instanceof JComboBox<?> cb) {
                Object it = cb.getSelectedItem();
                s = it == null ? "" : ((Item) it).value();
            } else {
                s = ((JTextField) c).getText().trim();
            }
            if (s.isEmpty()) {
                if (f.req) throw new IllegalArgumentException(f.label + " không được để trống.");
                return null;
            }
            try {
                return switch (f.kind) {
                    case INT -> Integer.parseInt(s);
                    case DEC -> new BigDecimal(s.replace(',', '.'));
                    case DATE -> java.sql.Date.valueOf(s);
                    case BOOL -> s.equals("1");
                    default -> s;
                };
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Sai định dạng ở trường \"" + f.label + "\""
                        + (f.kind == Kind.DATE ? " (cần yyyy-MM-dd)." : "."));
            }
        }

        private void save() {
            try {
                List<Object> vals = new ArrayList<>();
                for (int i = 0; i < inputs.length; i++) vals.add(value(e.fields.get(i), inputs[i]));
                if (row == null) {
                    String ph = String.join(",", Collections.nCopies(vals.size(), "?"));
                    Sql.update("INSERT INTO " + e.table + " (" + e.cols() + ") VALUES (" + ph + ")", vals.toArray());
                } else {
                    StringJoiner set = new StringJoiner(", "), where = new StringJoiner(" AND ");
                    List<Object> setArgs = new ArrayList<>(), whereArgs = new ArrayList<>();
                    for (int i = 0; i < inputs.length; i++) {
                        F f = e.fields.get(i);
                        if (f.pk) { where.add(f.col + " = ?"); whereArgs.add(row[i]); }
                        else { set.add(f.col + " = ?"); setArgs.add(vals.get(i)); }
                    }
                    if (set.length() == 0) {
                        Ui.info(this, "Bảng này chỉ gồm khóa nên không có gì để sửa. Hãy xóa rồi thêm lại.");
                        return;
                    }
                    setArgs.addAll(whereArgs);
                    Sql.update("UPDATE " + e.table + " SET " + set + " WHERE " + where, setArgs.toArray());
                }
                e.after.run();
                saved = true;
                dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        }
    }
}
