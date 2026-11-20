package qltc.ui;

import qltc.auth.Auth;
import qltc.db.Sql;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

/** Hộp thoại sinh viên tự cập nhật email và số điện thoại của mình. */
public final class ProfileDialog extends JDialog {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^\\d{9,11}$");
    private boolean ok;

    private ProfileDialog(Window owner, String email, String sdt) {
        super(owner, "Cập nhật thông tin", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JTextField tfEmail = new JTextField(email, 24);
        JTextField tfSdt = new JTextField(sdt, 24);

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 10));
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 8, 18));
        p.add(new JLabel("Email"));
        p.add(tfEmail);
        p.add(new JLabel("Số điện thoại"));
        p.add(tfSdt);

        JLabel note = Ui.muted("Email cũng là tên đăng nhập của bạn.");
        note.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));

        JPanel btn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancel = Ui.button("Hủy", false);
        cancel.addActionListener(ev -> dispose());
        JButton save = Ui.button("Lưu", true);
        btn.add(cancel);
        btn.add(save);

        save.addActionListener(ev -> {
            String e = tfEmail.getText().trim();
            String s = tfSdt.getText().trim();
            if (!EMAIL.matcher(e).matches()) { warn("Email không hợp lệ (ví dụ: ten@example.com)."); return; }
            if (!s.isEmpty() && !PHONE.matcher(s).matches()) { warn("Số điện thoại chỉ gồm 9 đến 11 chữ số."); return; }
            try {
                String ma = Auth.current().maSv();
                Object dup = Sql.scalar("SELECT COUNT(*) FROM sinh_vien WHERE email = ? AND ma_sv <> ?", e, ma);
                if (((Number) dup).intValue() > 0) { warn("Email này đã được sinh viên khác sử dụng."); return; }
                Sql.update("UPDATE sinh_vien SET email = ?, sdt = ? WHERE ma_sv = ?", e, s.isEmpty() ? null : s, ma);
                ok = true;
                Ui.info(this, "Đã cập nhật thông tin.");
                dispose();
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        });
        getRootPane().setDefaultButton(save);

        JPanel center = new JPanel(new BorderLayout());
        center.add(p, BorderLayout.CENTER);
        center.add(note, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        add(btn, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    /** Mở hộp thoại cho sinh viên đang đăng nhập. Trả về true nếu đã lưu thay đổi. */
    public static boolean show(Window owner) {
        try {
            String ma = Auth.current().maSv();
            Sql.Table t = Sql.query("SELECT email, sdt FROM sinh_vien WHERE ma_sv = ?", ma);
            if (t.rows().isEmpty()) return false;
            Object[] r = t.rows().get(0);
            ProfileDialog d = new ProfileDialog(owner, r[0] == null ? "" : r[0].toString(), r[1] == null ? "" : r[1].toString());
            d.setVisible(true);
            return d.ok;
        } catch (Exception ex) {
            Ui.error(owner, ex);
            return false;
        }
    }
}