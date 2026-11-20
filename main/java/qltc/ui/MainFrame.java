package qltc.ui;

import qltc.auth.Auth;
import qltc.db.Sql;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Cửa sổ chính: menu bên trái theo vai trò, nội dung bên phải. */
public class MainFrame extends JFrame {
    private final JPanel menu = new JPanel();
    private final JPanel content = new JPanel(new BorderLayout());
    private final List<JButton> buttons = new ArrayList<>();
    private JButton first;

    public MainFrame() {
        super("Quản lý đào tạo tín chỉ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 720);
        setLocationRelativeTo(null);
        Auth.User u = Auth.current();

        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Ui.RED);
        menu.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel who = new JLabel("<html><b>" + u.ten() + "</b><br>" + (u.isAdmin() ? "Quản lý" : "Sinh viên " + u.maSv()) + "</html>");
        who.setForeground(Color.WHITE);
        who.setBorder(new EmptyBorder(8, 16, 14, 16));
        who.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(who);

        if (u.isAdmin()) buildAdmin(); else buildStudent();

        menu.add(Box.createVerticalStrut(12));
        section("TÀI KHOẢN");
        item("Đổi mật khẩu", () -> {
            PasswordDialog.show(this, false);
            return blank();
        });
        JButton out = item("Đăng xuất", () -> blank());
        out.addActionListener(ev -> {
            Auth.logout();
            new LoginFrame().setVisible(true);
            dispose();
        });

        JScrollPane sp = new JScrollPane(menu, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setPreferredSize(new Dimension(230, 0));
        add(sp, BorderLayout.WEST);
        content.setBackground(Color.WHITE);
        add(content, BorderLayout.CENTER);
        if (first != null) first.doClick();
    }

    private void buildAdmin() {
        section("TỔNG QUAN");
        item("Trang chủ", Dashboard::new);

        section("DANH MỤC");
        crud("Khoa", Entities::khoa);
        crud("Ngành", Entities::nganh);
        crud("Nhóm học phần", Entities::nhomHp);
        crud("Lớp hành chính", Entities::lopHanhChinh);
        crud("Phòng học", Entities::phongHoc);
        crud("Học phần", Entities::hocPhan);
        crud("Chương trình đào tạo", Entities::chuongTrinh);

        section("CON NGƯỜI");
        crud("Sinh viên", Entities::sinhVien);
        crud("Giảng viên", Entities::giangVien);

        section("ĐÀO TẠO");
        crud("Học kỳ", Entities::hocKy);
        crud("Lớp tín chỉ", Entities::lopTinChi);
        crud("Thời khóa biểu", Entities::tkb);
        crud("Đăng ký & điểm", Entities::dangKy);
        crud("Học phí", Entities::hocPhi);
        crud("Rèn luyện & cảnh báo", Entities::ketQua);

        section("BÁO CÁO / TRA CỨU");
        item("Thời khóa biểu sinh viên", () -> Features.tkb(true));
        item("GPA theo khoa", Features::gpaTheoKhoa);
        item("Học phần & lớp mở", Features::hocPhanVaLopMo);
        item("Môn tiên quyết", Features::tienQuyet);
        item("Sinh viên ĐRL cao", Features::drlCao);
        item("Lớp có tỷ lệ trượt cao", Features::tyLeTruot);
        item("Chưa nộp học phí", Features::chuaNopHocPhi);
        item("Hoàn thành nhóm học phần", Features::hoanThanhNhom);
        item("Xét học bổng", Features::hocBong);
    }

    private void buildStudent() {
        section("CỦA TÔI");
        item("Thông tin cá nhân", Features::thongTinCuaToi);
        item("Thời khóa biểu", () -> Features.tkb(false));
        item("Bảng điểm & GPA", Features::bangDiemCuaToi);
        item("Học phí", Features::hocPhiCuaToi);
        section("HỌC TẬP");
        item("Đăng ký lớp tín chỉ", DangKyPanel::new);
        item("Học phần & lớp mở", Features::hocPhanVaLopMo);
        item("Môn tiên quyết", Features::tienQuyet);
    }

    private JComponent blank() { return new JPanel(); }

    private void crud(String label, Supplier<Entity> e) {
        item(label, () -> new CrudPanel(e.get()));
    }

    private void section(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(0xFFCDD2));
        l.setFont(Ui.BASE.deriveFont(Font.BOLD, 11f));
        l.setBorder(new EmptyBorder(12, 16, 4, 16));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(l);
    }

    private JButton item(String label, Supplier<JComponent> view) {
        JButton b = new JButton(label);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(6, 22, 6, 12));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setForeground(Color.WHITE);
        b.setBackground(Ui.RED);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(ev -> {
            JComponent v = view.get();
            for (JButton x : buttons) { x.setBackground(Ui.RED); x.setForeground(Color.WHITE); }
            b.setBackground(Color.WHITE);
            b.setForeground(Ui.RED);
            content.removeAll();
            content.add(v, BorderLayout.CENTER);
            content.revalidate();
            content.repaint();
        });
        buttons.add(b);
        menu.add(b);
        if (first == null) first = b;
        return b;
    }

    /** Trang chủ quản lý: số liệu tổng quan. */
    private static final class Dashboard extends JPanel {
        Dashboard() {
            setLayout(new BorderLayout(0, 14));
            setBackground(Color.WHITE);
            setBorder(new EmptyBorder(18, 20, 18, 20));
            add(Ui.title("Tổng quan hệ thống"), BorderLayout.NORTH);
            JPanel grid = new JPanel(new GridLayout(0, 3, 14, 14));
            grid.setOpaque(false);
            try {
                card(grid, "Sinh viên", "SELECT COUNT(*) FROM sinh_vien");
                card(grid, "Giảng viên", "SELECT COUNT(*) FROM giang_vien");
                card(grid, "Học phần", "SELECT COUNT(*) FROM hoc_phan");
                card(grid, "Lớp tín chỉ", "SELECT COUNT(*) FROM lop_tin_chi");
                card(grid, "Lượt đăng ký", "SELECT COUNT(*) FROM dang_ky");
                Object hk = Sql.scalar("SELECT ma_hk FROM hoc_ky WHERE trang_thai = 'dang_mo'");
                grid.add(box("Học kỳ đang mở", hk == null ? "Chưa có" : hk.toString()));
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.add(grid, BorderLayout.NORTH);
            add(wrap, BorderLayout.CENTER);
        }

        private void card(JPanel grid, String label, String sql) throws java.sql.SQLException {
            grid.add(box(label, String.valueOf(Sql.scalar(sql))));
        }

        private JPanel box(String label, String value) {
            JPanel p = new JPanel(new GridLayout(2, 1));
            p.setBackground(Ui.SOFT);
            p.setBorder(new EmptyBorder(16, 18, 16, 18));
            JLabel v = new JLabel(value);
            v.setFont(Ui.BASE.deriveFont(Font.BOLD, 30f));
            v.setForeground(Ui.RED);
            p.add(v);
            p.add(Ui.muted(label));
            return p;
        }
    }
}