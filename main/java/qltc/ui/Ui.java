package qltc.ui;

import qltc.db.Sql;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.*;
import java.awt.*;
import java.util.Enumeration;

/** Giao diện trắng - đỏ dùng chung và các hàm tiện ích Swing. */
public final class Ui {
    public static final Color RED = new Color(0xC62828);
    public static final Color DARK = new Color(0x8E0000);
    public static final Color SOFT = new Color(0xFDECEA);
    public static final Color LINE = new Color(0xE0E0E0);
    public static final Color GRAY = new Color(0x666666);
    public static final Font BASE = new Font("Segoe UI", Font.PLAIN, 14);

    private Ui() {}

    public static void install() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        FontUIResource f = new FontUIResource(BASE);
        for (Enumeration<Object> k = UIManager.getDefaults().keys(); k.hasMoreElements();) {
            Object key = k.nextElement();
            if (key.toString().endsWith(".font")) UIManager.put(key, f);
        }
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("OptionPane.background", Color.WHITE);
    }

    public static JButton button(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primary) { b.setBackground(RED); b.setForeground(Color.WHITE); }
        else { b.setBackground(SOFT); b.setForeground(DARK); }
        return b;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BASE.deriveFont(Font.BOLD, 20f));
        l.setForeground(RED);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(GRAY);
        return l;
    }

    public static JTable table() {
        DefaultTableModel m = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(m);
        t.setRowHeight(26);
        t.setGridColor(LINE);
        t.setSelectionBackground(SOFT);
        t.setSelectionForeground(Color.BLACK);
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        DefaultTableCellRenderer hr = new DefaultTableCellRenderer();
        hr.setBackground(RED);
        hr.setForeground(Color.WHITE);
        hr.setFont(BASE.deriveFont(Font.BOLD));
        hr.setHorizontalAlignment(SwingConstants.CENTER);
        hr.setOpaque(true);
        t.getTableHeader().setDefaultRenderer(hr);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    /** Đổ dữ liệu vào bảng; Boolean hiển thị Có/Không, NULL hiển thị rỗng. */
    public static void fill(JTable t, Sql.Table d) {
        Object[][] data = new Object[d.rows().size()][];
        for (int i = 0; i < data.length; i++) {
            Object[] src = d.rows().get(i);
            Object[] row = new Object[src.length];
            for (int j = 0; j < src.length; j++) row[j] = src[j] instanceof Boolean b ? (b ? "Có" : "Không") : src[j];
            data[i] = row;
        }
        ((DefaultTableModel) t.getModel()).setDataVector(data, d.cols().toArray());
        autoFit(t);
    }

    /** Chỉnh độ rộng từng cột theo nội dung: cột ngắn (số, mã) hẹp lại, cột dài (tên) rộng ra. */
    static void autoFit(JTable t) {
        FontMetrics fm = t.getFontMetrics(t.getFont());
        FontMetrics hm = t.getTableHeader().getFontMetrics(BASE.deriveFont(Font.BOLD));
        for (int c = 0; c < t.getColumnCount(); c++) {
            int w = hm.stringWidth(String.valueOf(t.getColumnName(c)));
            for (int r = 0; r < t.getRowCount(); r++) {
                Object v = t.getValueAt(r, c);
                if (v != null) w = Math.max(w, fm.stringWidth(v.toString()));
            }
            t.getColumnModel().getColumn(c).setPreferredWidth(Math.max(50, Math.min(w + 24, 420)));
        }
    }

    public static void error(Component c, Throwable e) {
        JOptionPane.showMessageDialog(c, Sql.explain(e), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component c, String msg) {
        JOptionPane.showMessageDialog(c, msg, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component c, String msg) {
        return JOptionPane.showConfirmDialog(c, msg, "Xác nhận", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
}