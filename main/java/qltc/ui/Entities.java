package qltc.ui;

import qltc.auth.Auth;
import qltc.ui.Entity.F;

import java.sql.SQLException;

/** Định nghĩa 15 bảng cho các màn hình quản lý. */
public final class Entities {
    private Entities() {}

    static final String KHOA = "SELECT ma_khoa, CONCAT(ma_khoa,' - ',ten_khoa) FROM khoa ORDER BY 1";
    static final String NGANH = "SELECT ma_nganh, CONCAT(ma_nganh,' - ',ten_nganh) FROM nganh ORDER BY 1";
    static final String NHOM = "SELECT ma_nhom, CONCAT(ma_nhom,' - ',ten_nhom) FROM nhom_hp ORDER BY 1";
    static final String LOP = "SELECT ma_lop, ma_lop FROM lop_hanh_chinh ORDER BY 1";
    static final String HK = "SELECT ma_hk, ma_hk FROM hoc_ky ORDER BY ngay_bd";
    static final String PHONG = "SELECT ma_ph, CONCAT(ma_ph,' - ',ten_phong) FROM phong_hoc ORDER BY 1";
    static final String HP = "SELECT ma_hp, CONCAT(ma_hp,' - ',ten_hp) FROM hoc_phan ORDER BY 1";
    static final String GV = "SELECT ma_gv, CONCAT(ma_gv,' - ',ten) FROM giang_vien ORDER BY 1";
    static final String SV = "SELECT ma_sv, CONCAT(ma_sv,' - ',ten) FROM sinh_vien ORDER BY 1";
    static final String LTC = "SELECT ma_lop_tc, CONCAT(ma_lop_tc,' - ',ma_hp,' - ',ma_hk) FROM lop_tin_chi ORDER BY 1";

    public static Entity khoa() {
        return new Entity("khoa", "Khoa", F.text("ma_khoa", "Mã khoa").pk(), F.text("ten_khoa", "Tên khoa").req());
    }

    public static Entity nganh() {
        return new Entity("nganh", "Ngành", F.text("ma_nganh", "Mã ngành").pk(), F.text("ten_nganh", "Tên ngành").req(),
                F.fk("ma_khoa", "Khoa", KHOA).req());
    }

    public static Entity nhomHp() {
        return new Entity("nhom_hp", "Nhóm học phần", F.text("ma_nhom", "Mã nhóm").pk(), F.text("ten_nhom", "Tên nhóm").req());
    }

    public static Entity lopHanhChinh() {
        return new Entity("lop_hanh_chinh", "Lớp hành chính", F.text("ma_lop", "Mã lớp").pk(),
                F.text("khoa_hoc", "Khóa học"), F.fk("ma_nganh", "Ngành", NGANH).req());
    }

    public static Entity phongHoc() {
        return new Entity("phong_hoc", "Phòng học", F.text("ma_ph", "Mã phòng").pk(), F.text("ten_phong", "Tên phòng").req(),
                F.integer("suc_chua", "Sức chứa").req(), F.text("toa", "Tòa"));
    }

    public static Entity hocPhan() {
        return new Entity("hoc_phan", "Học phần", F.text("ma_hp", "Mã học phần").pk(), F.text("ten_hp", "Tên học phần").req(),
                F.integer("so_tc", "Số tín chỉ").req(), F.dec("tscc", "TS chuyên cần (%)").req(),
                F.dec("tsgk", "TS giữa kỳ (%)").req(), F.dec("tsthi", "TS thi (%)").req(),
                F.fk("ma_nhom", "Nhóm học phần", NHOM));
    }

    public static Entity giangVien() {
        return new Entity("giang_vien", "Giảng viên", F.text("ma_gv", "Mã GV").pk(), F.text("ten", "Họ tên").req(),
                F.date("ngay_sinh", "Ngày sinh"), F.en("gioi_tinh", "Giới tính", "nam", "nu"), F.text("sdt", "SĐT"),
                F.text("email", "Email"), F.text("hoc_vi", "Học vị"), F.fk("ma_khoa", "Khoa", KHOA).req());
    }

    public static Entity sinhVien() {
        Entity e = new Entity("sinh_vien", "Sinh viên", F.text("ma_sv", "Mã SV").pk(), F.text("ten", "Họ tên").req(),
                F.date("ngay_sinh", "Ngày sinh"), F.en("gioi_tinh", "Giới tính", "nam", "nu"), F.text("sdt", "SĐT"),
                F.text("email", "Email (tên đăng nhập)"), F.fk("ma_lop", "Lớp", LOP).req());
        e.after = () -> {
            try { Auth.provision(); } catch (SQLException ex) { throw new RuntimeException(ex); }
        };
        return e;
    }

    public static Entity hocKy() {
        return new Entity("hoc_ky", "Học kỳ", F.text("ma_hk", "Mã học kỳ").pk(), F.date("ngay_bd", "Ngày bắt đầu").req(),
                F.date("ngay_kt", "Ngày kết thúc").req(), F.en("trang_thai", "Trạng thái", "chua_mo", "dang_mo", "da_dong").req());
    }

    public static Entity lopTinChi() {
        return new Entity("lop_tin_chi", "Lớp tín chỉ", F.text("ma_lop_tc", "Mã lớp TC").pk(),
                F.integer("si_so_toi_da", "Sĩ số tối đa").req(), F.fk("ma_hk", "Học kỳ", HK).req(),
                F.fk("ma_gv", "Giảng viên", GV).req(), F.fk("ma_hp", "Học phần", HP).req());
    }

    public static Entity tkb() {
        return new Entity("tkb", "Thời khóa biểu", F.text("ma_tkb", "Mã TKB").pk(), F.integer("tiet_bd", "Tiết bắt đầu").req(),
                F.integer("thu", "Thứ (2-8)").req(), F.integer("so_tiet", "Số tiết").req(),
                F.integer("tuan_bd", "Tuần bắt đầu").req(), F.integer("tuan_kt", "Tuần kết thúc").req(),
                F.fk("ma_lop_tc", "Lớp tín chỉ", LTC).req(), F.fk("ma_ph", "Phòng học", PHONG).req());
    }

    public static Entity dangKy() {
        return new Entity("dang_ky", "Đăng ký & điểm", F.fk("ma_sv", "Sinh viên", SV).pk(),
                F.fk("ma_lop_tc", "Lớp tín chỉ", LTC).pk(), F.date("ngay_dk", "Ngày đăng ký").req(),
                F.dec("diem_cc", "Điểm chuyên cần"), F.dec("diem_gk", "Điểm giữa kỳ"), F.dec("diem_thi", "Điểm thi"));
    }

    public static Entity hocPhi() {
        return new Entity("hoc_phi", "Học phí", F.fk("ma_sv", "Sinh viên", SV).pk(), F.fk("ma_hk", "Học kỳ", HK).pk(),
                F.dec("so_tien", "Số tiền").req(), F.en("trang_thai", "Trạng thái", "da_nop", "chua_nop").req(),
                F.date("ngay_nop", "Ngày nộp"));
    }

    public static Entity ketQua() {
        return new Entity("ket_qua", "Rèn luyện & cảnh báo", F.fk("ma_sv", "Sinh viên", SV).pk(), F.fk("ma_hk", "Học kỳ", HK).pk(),
                F.integer("drl", "Điểm rèn luyện"), F.text("muc_canh_bao", "Mức cảnh báo"));
    }

    public static Entity chuongTrinh() {
        return new Entity("chuong_trinh", "Chương trình đào tạo", F.fk("ma_nganh", "Ngành", NGANH).pk(),
                F.fk("ma_hp", "Học phần", HP).pk(), F.integer("ky_thu", "Kỳ thứ").req(), F.bool("tinh_bat_buoc", "Bắt buộc"));
    }
}