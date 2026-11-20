package qltc.ui;

import qltc.auth.Auth;
import qltc.ui.QueryPanel.Param;

import javax.swing.*;
import java.awt.*;

/**
 * Các chức năng báo cáo, mỗi chức năng tương ứng một truy vấn trong đề:
 * Q1 ĐRL cao | Q2 TKB | Q3 GPA | Q4 tỷ lệ trượt | Q5 học phần & lớp mở | Q6 chưa nộp học phí
 * Q7 hoàn thành nhóm học phần | Q8 học bổng top 1% | Q9 môn tiên quyết (đệ quy).
 */
public final class Features {
    private Features() {}

    static final String SV_COMBO = "SELECT ma_sv, CONCAT(ma_sv,' - ',ten) FROM sinh_vien ORDER BY ma_sv";

    private static Param me() { return Param.fixed("sv", () -> Auth.current().maSv()); }

    /** Q1 */
    public static QueryPanel drlCao() {
        return new QueryPanel("Sinh viên có điểm rèn luyện cao", """
            select sv.ma_sv, sv.ten, sv.gioi_tinh, sv.ngay_sinh, sv.email, round(avg(k.drl), 1) as drl_tb
            from sinh_vien sv join ket_qua k on k.ma_sv = sv.ma_sv
            where sv.ma_lop = :lop
            group by sv.ma_sv, sv.ten, sv.gioi_tinh, sv.ngay_sinh, sv.email
            having avg(k.drl) > :nguong
            order by drl_tb desc""",
                Param.combo("lop", "Lớp", "SELECT ma_lop, ma_lop FROM lop_hanh_chinh ORDER BY 1").def("D24CQKH02"),
                Param.number("nguong", "ĐRL TB lớn hơn", "80"));
    }

    /** Q2 */
    public static QueryPanel tkb(boolean admin) {
        return new QueryPanel("Thời khóa biểu học kỳ đang mở", """
            select t.thu, t.tiet_bd, t.so_tiet, t.tuan_bd, t.tuan_kt, l.ma_lop_tc, hp.ten_hp,
                   p.ten_phong, p.toa, gv.ten as ten_giang_vien
            from dang_ky d
            join lop_tin_chi l on l.ma_lop_tc = d.ma_lop_tc
            join hoc_ky h on h.ma_hk = l.ma_hk
            join hoc_phan hp on hp.ma_hp = l.ma_hp
            join giang_vien gv on gv.ma_gv = l.ma_gv
            join tkb t on t.ma_lop_tc = l.ma_lop_tc
            join phong_hoc p on p.ma_ph = t.ma_ph
            where d.ma_sv = :sv and h.trang_thai = 'dang_mo'
            order by t.thu, t.tiet_bd""",
                admin ? Param.combo("sv", "Sinh viên", SV_COMBO) : me());
    }

    /** Q3 - quản lý: GPA từng học kỳ của sinh viên một khoa */
    public static QueryPanel gpaTheoKhoa() {
        return new QueryPanel("GPA học kỳ (thang 4) theo khoa", """
            select v.ma_sv, v.ten_sinh_vien, v.ma_hk, sum(v.so_tc) as tc, round(sum(v.diem_he4 * v.so_tc) / sum(v.so_tc), 2) as gpa
            from vw_diem_he4 v
            join sinh_vien sv on sv.ma_sv = v.ma_sv
            join lop_hanh_chinh l on l.ma_lop = sv.ma_lop
            join nganh n on n.ma_nganh = l.ma_nganh
            where n.ma_khoa = :khoa and v.diem_he4 is not null
            group by v.ma_sv, v.ten_sinh_vien, v.ma_hk
            order by v.ma_sv, v.ma_hk""",
                Param.combo("khoa", "Khoa", "SELECT ma_khoa, ten_khoa FROM khoa ORDER BY 1").def("K01"));
    }

    /** Q3 - sinh viên: bảng điểm của chính mình (GPA từng kỳ + tích lũy, và chi tiết môn) */
    public static JComponent bangDiemCuaToi() {
        QueryPanel gpa = new QueryPanel("GPA của tôi", """
            select x.hoc_ky, x.tin_chi, x.gpa,
                   (select k.drl from ket_qua k where k.ma_sv = :sv and k.ma_hk = x.hoc_ky) as drl
            from (
                select v.ma_hk as hoc_ky, sum(v.so_tc) as tin_chi,
                       round(sum(v.diem_he4 * v.so_tc) / sum(v.so_tc), 2) as gpa
                from vw_diem_he4 v
                where v.ma_sv = :sv and v.diem_he4 is not null
                group by v.ma_hk
                union all
                select 'Tích lũy', sum(g.so_tc), round(sum(g.diem_he4 * g.so_tc) / sum(g.so_tc), 2)
                from vw_diem_tinh_gpa g
                where g.ma_sv = :sv
                having count(*) > 0
            ) x order by (x.hoc_ky = 'Tích lũy'), x.hoc_ky""", me());
        QueryPanel ct = new QueryPanel("Điểm chi tiết", """
            select v.ma_hk, v.ten_hp, v.so_tc, v.diem_tong_ket, v.diem_he4, v.diem_chu,
                   case v.da_dat when 1 then 'Đạt' when 0 then 'Không đạt' end as ket_qua
            from vw_diem_he4 v where v.ma_sv = :sv order by v.ma_hk, v.ten_hp""", me());
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.add(gpa);
        p.add(ct);
        return p;
    }

    /** Q4 */
    public static QueryPanel tyLeTruot() {
        return new QueryPanel("Lớp tín chỉ có tỷ lệ trượt cao", """
            select l.ma_lop_tc, hp.ten_hp, l.ma_hk, count(v.diem_tong_ket) as so_sv,
                   sum(v.diem_tong_ket < 4.0) as so_truot,
                   round(100 * sum(v.diem_tong_ket < 4.0) / count(v.diem_tong_ket), 1) as ty_le_truot
            from vw_diem_tong_ket v
            join lop_tin_chi l on l.ma_lop_tc = v.ma_lop_tc
            join hoc_phan hp on hp.ma_hp = l.ma_hp
            group by l.ma_lop_tc, hp.ten_hp, l.ma_hk
            having 100 * sum(v.diem_tong_ket < 4.0) / count(v.diem_tong_ket) > :nguong
            order by ty_le_truot desc""",
                Param.number("nguong", "Tỷ lệ trượt lớn hơn (%)", "30"));
    }

    /** Q5 */
    public static QueryPanel hocPhanVaLopMo() {
        return new QueryPanel("Học phần và lớp mở trong học kỳ đang mở", """
            select * from (
                select h.ma_hp, h.ten_hp, h.so_tc, l.ma_lop_tc, gv.ten as giang_vien, l.si_so_toi_da,
                       (select count(*) from dang_ky d where d.ma_lop_tc = l.ma_lop_tc) as da_dang_ky
                from hoc_phan h
                left join lop_tin_chi l on l.ma_hp = h.ma_hp
                     and l.ma_hk in (select ma_hk from hoc_ky where trang_thai = 'dang_mo')
                left join giang_vien gv on gv.ma_gv = l.ma_gv
            ) x
            where (:loc = 'all' or x.ma_lop_tc is null)
            order by x.ma_hp, x.ma_lop_tc""",
                Param.options("loc", "Hiển thị", new Item("all", "Tất cả học phần"), new Item("chua", "Chỉ học phần chưa mở lớp")));
    }

    /** Q6 */
    public static QueryPanel chuaNopHocPhi() {
        return new QueryPanel("Sinh viên đã đăng ký nhưng chưa nộp học phí", """
            select distinct sv.ma_sv, sv.ten, sv.ma_lop, sv.sdt, sv.email, h.ma_hk
            from sinh_vien sv
            join dang_ky d on d.ma_sv = sv.ma_sv
            join lop_tin_chi l on l.ma_lop_tc = d.ma_lop_tc
            join hoc_ky h on h.ma_hk = l.ma_hk
            where h.trang_thai = 'dang_mo'
              and not exists (select 1 from hoc_phi hp
                              where hp.ma_sv = sv.ma_sv and hp.ma_hk = h.ma_hk and hp.trang_thai = 'da_nop')
            order by sv.ma_sv""");
    }

    /** Q7 */
    public static QueryPanel hoanThanhNhom() {
        return new QueryPanel("Sinh viên đã đỗ tất cả học phần của một nhóm", """
            select sv.ma_sv, sv.ten, sv.ma_lop
            from sinh_vien sv
            join vw_diem_he4 v on v.ma_sv = sv.ma_sv
            join hoc_phan h on h.ma_hp = v.ma_hp
            where h.ma_nhom = :nhom and v.da_dat = 1
            group by sv.ma_sv, sv.ten, sv.ma_lop
            having count(distinct h.ma_hp) = (select count(*) from hoc_phan where ma_nhom = :nhom)
            order by sv.ma_sv""",
                Param.combo("nhom", "Nhóm học phần", "SELECT ma_nhom, ten_nhom FROM nhom_hp ORDER BY 1"));
    }

    /** Q8 */
    public static QueryPanel hocBong() {
        return new QueryPanel("Xét học bổng: top sinh viên theo ngành", """
            with gpa as (
                select d.ma_sv, round(sum(d.diem_he4 * d.so_tc) / sum(d.so_tc), 2) as gpa_tich_luy
                from vw_diem_tinh_gpa d where d.diem_he4 is not null group by d.ma_sv
            ),
            xh as (
                select sv.ma_sv, sv.ten, l.ma_nganh, g.gpa_tich_luy,
                       rank() over (partition by l.ma_nganh order by g.gpa_tich_luy desc) as hang,
                       count(*) over (partition by l.ma_nganh) as si_so
                from gpa g
                join sinh_vien sv on sv.ma_sv = g.ma_sv
                join lop_hanh_chinh l on l.ma_lop = sv.ma_lop
            )
            select xh.ma_nganh, n.ten_nganh, xh.ma_sv, xh.ten, xh.gpa_tich_luy, xh.hang
            from xh join nganh n on n.ma_nganh = xh.ma_nganh
            where xh.hang <= ceiling(xh.si_so * :pct / 100)
            order by xh.ma_nganh, xh.hang""",
                Param.number("pct", "Top (% sinh viên)", "1"));
    }

    /** Q9 */
    public static QueryPanel tienQuyet() {
        return new QueryPanel("Môn tiên quyết", """
            with recursive ds as (
                select tq.ma_hp1 as ma_hp, 1 as cap from tien_quyet tq where tq.ma_hp2 = :hp
                union all
                select tq.ma_hp1, t.cap + 1 from tien_quyet tq join ds t on tq.ma_hp2 = t.ma_hp where t.cap < 20
            )
            select hp.ma_hp, hp.ten_hp, hp.so_tc, min(t.cap) as cap,
                   case when min(t.cap) = 1 then 'Trực tiếp' else 'Gián tiếp' end as loai
            from ds t join hoc_phan hp on hp.ma_hp = t.ma_hp
            group by hp.ma_hp, hp.ten_hp, hp.so_tc
            order by cap, hp.ma_hp""",
                Param.combo("hp", "Học phần", "SELECT ma_hp, CONCAT(ma_hp,' - ',ten_hp) FROM hoc_phan ORDER BY 1").def("HP033"));
    }

    // ---- màn hình dành cho sinh viên ----
    public static QueryPanel thongTinCuaToi() {
        return new QueryPanel("Thông tin của tôi", """
            select sv.ma_sv, sv.ten, sv.ngay_sinh, sv.gioi_tinh, sv.sdt, sv.email, sv.ma_lop, n.ten_nganh, k.ten_khoa
            from sinh_vien sv
            join lop_hanh_chinh l on l.ma_lop = sv.ma_lop
            join nganh n on n.ma_nganh = l.ma_nganh
            join khoa k on k.ma_khoa = n.ma_khoa
            where sv.ma_sv = :sv""", me())
                .vertical("Mã sinh viên", "Họ tên", "Ngày sinh", "Giới tính", "Số điện thoại", "Email",
                        "Lớp", "Ngành", "Khoa")
                .withAction("Cập nhật thông tin", q -> {
                    if (ProfileDialog.show(SwingUtilities.getWindowAncestor(q))) q.run();
                });
    }

    public static QueryPanel hocPhiCuaToi() {
        return new QueryPanel("Học phí của tôi", """
            select h.ma_hk, format(h.so_tien, 0, 'de_DE') as so_tien, h.trang_thai, h.ngay_nop
            from hoc_phi h where h.ma_sv = :sv order by h.ma_hk""", me());
    }
}