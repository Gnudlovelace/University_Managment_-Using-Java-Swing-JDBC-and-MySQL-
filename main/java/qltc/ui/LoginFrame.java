package qltc.ui;

import qltc.auth.Auth;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField user = new JTextField(22);
    private final JPasswordField pass = new JPasswordField(22);

    public LoginFrame() {
        super("Đăng nhập - Quản lý đào tạo tín chỉ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel head = new JPanel(new GridLayout(2, 1));
        head.setBackground(Ui.RED);
        head.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JLabel t = new JLabel("QUẢN LÝ ĐÀO TẠO TÍN CHỈ");
        t.setFont(Ui.BASE.deriveFont(Font.BOLD, 22f));
        t.setForeground(Color.WHITE);
        JLabel s = new JLabel("Đăng nhập bằng email sinh viên (hoặc tài khoản quản lý)");
        s.setForeground(new Color(0xFFCDD2));
        head.add(t); head.add(s);
        add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Email / Tên đăng nhập"), g);
        g.gridx = 1; form.add(user, g);
        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Mật khẩu"), g);
        g.gridx = 1; form.add(pass, g);
        add(form, BorderLayout.CENTER);

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 12));
        JButton go = Ui.button("Đăng nhập", true);
        foot.add(go);
        add(foot, BorderLayout.SOUTH);
        go.addActionListener(ev -> login());
        getRootPane().setDefaultButton(go);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void login() {
        try {
            Auth.User u = Auth.login(user.getText().trim(), new String(pass.getPassword()));
            if (u == null) {
                JOptionPane.showMessageDialog(this, "Sai email/tên đăng nhập hoặc mật khẩu.", "Đăng nhập thất bại",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            new MainFrame().setVisible(true);
            dispose();
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }
}