package qltc.ui;

import qltc.auth.Auth;
import qltc.db.Sql;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Sinh viên đăng ký / hủy lớp tín chỉ của học kỳ đang mở.
 * App KHÔNG tự kiểm tra sĩ số hay trùng lịch: chỉ INSERT, trigger trg_dang_ky_insert quyết định
 * (lỗi SQLSTATE 45000 được Sql.explain hiển thị nguyên văn).
 */
public class DangKyPanel extends JPanel {
    private static final int COL_MA = 0, COL_TRANG_THAI = 8;

    private final String maSv = Auth.current().maSv(); // luôn lấy từ phiên đăng nhập
    private final JTable table = Ui.table();
    private final JLabel summary = Ui.muted(" ");

    public DangKyPanel() {
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        north.add(Ui.title("Đăng ký lớp tín chỉ (học kỳ đang mở)"));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setOpaque(false);
        JButton reg = Ui.button("Đăng ký", true);
        JButton del = Ui.button("Hủy đăng ký", false);
        JButton ref = Ui.button("Làm mới", false);
        reg.addActionListener(ev -> dangKy());
        del.addActionListener(ev -> huy());
        ref.addActionListener(ev -> load());
        bar.add(reg);
        bar.add(del);
        bar.add(ref);
        north.add(bar);

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(summary, BorderLayout.SOUTH);
        load();
    }

    private void load() {
        try {
            Sql.Table t = Sql.query("""
                select l.ma_lop_tc as 'Mã lớp', hp.ten_hp as 'Học phần', hp.so_tc as 'Tín chỉ',
                       gv.ten as 'Giảng viên',
                       (select group_concat(concat('Thứ ', t.thu, ' tiết ', t.tiet_bd, '-', t.tiet_bd + t.so_tiet - 1,
                                                   ' (', p.ten_phong, ')') order by t.thu, t.tiet_bd separator '; ')
                        from tkb t join phong_hoc p on p.ma_ph = t.ma_ph
                        where t.ma_lop_tc = l.ma_lop_tc) as 'Lịch học',
                       (select min(t.tuan_bd) from tkb t where t.ma_lop_tc = l.ma_lop_tc) as 'Từ tuần',
                       (select max(t.tuan_kt) from tkb t where t.ma_lop_tc = l.ma_lop_tc) as 'Đến tuần',
                       concat((select count(*) from dang_ky d where d.ma_lop_tc = l.ma_lop_tc), '/', l.si_so_toi_da) as 'Sĩ số',
                       case
                         when exists (select 1 from dang_ky d where d.ma_lop_tc = l.ma_lop_tc and d.ma_sv = ?)
                              then 'Đã đăng ký'
                         when exists (select 1 from vw_diem_tong_ket t
                                      where t.ma_sv = ? and t.ma_hp = l.ma_hp and t.diem_tong_ket >= 4.0)
                              then 'Đã đạt môn này'
                         when exists (select 1 from vw_diem_tong_ket t
                                      where t.ma_sv = ? and t.ma_hp = l.ma_hp and t.diem_tong_ket is null)
                              then 'Đang học môn này'
                         when exists (select 1 from vw_diem_tong_ket t
                                      where t.ma_sv = ? and t.ma_hp = l.ma_hp and t.diem_tong_ket < 4.0)
                              then 'Học lại'
                         else '' end as 'Trạng thái'
                from lop_tin_chi l
                join hoc_ky h on h.ma_hk = l.ma_hk and h.trang_thai = 'dang_mo'
                join hoc_phan hp on hp.ma_hp = l.ma_hp
                join giang_vien gv on gv.ma_gv = l.ma_gv
                order by hp.ten_hp, l.ma_lop_tc""", maSv, maSv, maSv, maSv);
            Ui.fill(table, t);

            Object tc = Sql.scalar("""
                select coalesce(sum(hp.so_tc), 0)
                from dang_ky d
                join lop_tin_chi l on l.ma_lop_tc = d.ma_lop_tc
                join hoc_ky h on h.ma_hk = l.ma_hk and h.trang_thai = 'dang_mo'
                join hoc_phan hp on hp.ma_hp = l.ma_hp
                where d.ma_sv = ?""", maSv);
            summary.setText("Tổng tín chỉ đã đăng ký trong học kỳ này: " + tc);
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
    }

    private String selectedLop() {
        int r = table.getSelectedRow();
        if (r < 0) {
            Ui.info(this, "Hãy chọn một lớp trong danh sách.");
            return null;
        }
        return String.valueOf(table.getValueAt(r, COL_MA));
    }

    private boolean selectedRegistered() {
        return "Đã đăng ký".equals(table.getValueAt(table.getSelectedRow(), COL_TRANG_THAI));
    }

    private void dangKy() {
        String lop = selectedLop();
        if (lop == null) return;
        if (selectedRegistered()) {
            Ui.info(this, "Bạn đã đăng ký lớp " + lop + " rồi.");
            return;
        }
        try {
            Sql.update("INSERT INTO dang_ky (ma_sv, ma_lop_tc, ngay_dk) VALUES (?, ?, CURDATE())", maSv, lop);
            Ui.info(this, "Đăng ký lớp " + lop + " thành công.");
        } catch (Exception ex) {
            Ui.error(this, ex); // lỗi từ trigger: đầy sĩ số / trùng lịch
        }
        load();
    }

    private void huy() {
        String lop = selectedLop();
        if (lop == null) return;
        if (!selectedRegistered()) {
            Ui.info(this, "Bạn chưa đăng ký lớp " + lop + ".");
            return;
        }
        if (!Ui.confirm(this, "Hủy đăng ký lớp " + lop + "?")) return;
        try {
            int n = Sql.update("""
                DELETE FROM dang_ky WHERE ma_sv = ? AND ma_lop_tc = ?
                  AND diem_cc IS NULL AND diem_gk IS NULL AND diem_thi IS NULL""", maSv, lop);
            if (n == 0) Ui.info(this, "Không thể hủy: lớp này đã có điểm.");
            else Ui.info(this, "Đã hủy đăng ký lớp " + lop + ".");
        } catch (Exception ex) {
            Ui.error(this, ex);
        }
        load();
    }
}