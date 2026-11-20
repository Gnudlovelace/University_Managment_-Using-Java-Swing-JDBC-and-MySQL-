package qltc.ui;

import qltc.auth.Auth;

import javax.swing.*;
import java.awt.*;

/** Hộp thoại đổi mật khẩu. force = true: bắt buộc đổi (lần đăng nhập đầu), không có nút Hủy. */
public final class PasswordDialog extends JDialog {
    private boolean ok;

    private PasswordDialog(Window owner, boolean force) {
        super(owner, force ? "Đổi mật khẩu (bắt buộc ở lần đăng nhập đầu)" : "Đổi mật khẩu", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPasswordField oldPw = new JPasswordField(18), newPw = new JPasswordField(18), again = new JPasswordField(18);
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 10));
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 8, 18));
        p.add(new JLabel("Mật khẩu hiện tại")); p.add(oldPw);
        p.add(new JLabel("Mật khẩu mới")); p.add(newPw);
        p.add(new JLabel("Nhập lại mật khẩu mới")); p.add(again);
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton save = Ui.button("Đổi mật khẩu", true);
        if (true) {
            JButton cancel = Ui.button("Hủy", false);
            cancel.addActionListener(ev -> dispose());
            btn.add(cancel);
        }
        btn.add(save);
        save.addActionListener(ev -> {
            String n = new String(newPw.getPassword());
            if (!n.equals(new String(again.getPassword()))) {
                JOptionPane.showMessageDialog(this, "Hai lần nhập mật khẩu mới không khớp.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                Auth.changePassword(Auth.current().id(), new String(oldPw.getPassword()), n);
                ok = true;
                Ui.info(this, "Đã đổi mật khẩu.");
                dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        });
        getRootPane().setDefaultButton(save);
        add(p, BorderLayout.CENTER);
        add(btn, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Trả về true nếu đã đổi thành công. */
    public static boolean show(Window owner, boolean force) {
        PasswordDialog d = new PasswordDialog(owner, force);
        d.setVisible(true);
        return d.ok;
    }
}
