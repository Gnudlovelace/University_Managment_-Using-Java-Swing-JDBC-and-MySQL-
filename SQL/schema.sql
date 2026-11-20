USE QUAN_LY_TIN_CHI;

-- 1. Khoa
CREATE TABLE khoa (
    ma_khoa VARCHAR(10) PRIMARY KEY,
    ten_khoa VARCHAR(100) NOT NULL
);

-- 2. Nganh
CREATE TABLE nganh (
    ma_nganh VARCHAR(10) PRIMARY KEY,
    ten_nganh VARCHAR(100) NOT NULL,
    ma_khoa VARCHAR(10) NOT NULL,

    CONSTRAINT fk_nganh_khoa
        FOREIGN KEY (ma_khoa) REFERENCES khoa(ma_khoa)
);

-- 3. Nhom hoc phan
CREATE TABLE nhom_hp (
    ma_nhom VARCHAR(10) PRIMARY KEY,
    ten_nhom VARCHAR(100) NOT NULL
);

-- 4. Lop hanh chinh
CREATE TABLE lop_hanh_chinh (
    ma_lop VARCHAR(10) PRIMARY KEY,
    khoa_hoc VARCHAR(20),
    ma_nganh VARCHAR(10) NOT NULL,

    CONSTRAINT fk_lop_hanh_chinh_nganh
        FOREIGN KEY (ma_nganh) REFERENCES nganh(ma_nganh)
);

-- 5. Hoc ky
CREATE TABLE hoc_ky (
    ma_hk VARCHAR(10) PRIMARY KEY,
    ngay_bd DATE NOT NULL,
    ngay_kt DATE NOT NULL,
    trang_thai VARCHAR(20) NOT NULL DEFAULT 'chua_mo',
    
    dang_mo_duy_nhat TINYINT
        GENERATED ALWAYS AS (CASE WHEN trang_thai = 'dang_mo' THEN 1 END),

    CONSTRAINT ck_hoc_ky_ngay
        CHECK (ngay_kt > ngay_bd),
	CONSTRAINT ck_hoc_ky_trang_thai
        CHECK (trang_thai IN ('chua_mo', 'dang_mo', 'da_dong')),
	CONSTRAINT uq_hoc_ky_dang_mo
        UNIQUE (dang_mo_duy_nhat)
);

-- 6. Phong hoc
CREATE TABLE phong_hoc (
    ma_ph VARCHAR(10) PRIMARY KEY,
    ten_phong VARCHAR(50) NOT NULL,
    suc_chua INT NOT NULL,
    toa VARCHAR(50),

    CONSTRAINT ck_phong_hoc_suc_chua
        CHECK (suc_chua > 0)
);

-- 7. Hoc phan
CREATE TABLE hoc_phan (
    ma_hp VARCHAR(10) PRIMARY KEY,
    ten_hp VARCHAR(100) NOT NULL,
    so_tc INT NOT NULL,
    tscc DECIMAL(5,2) NOT NULL,
    tsgk DECIMAL(5,2) NOT NULL,
    tsthi DECIMAL(5,2) NOT NULL,
    ma_nhom VARCHAR(10),

    CONSTRAINT fk_hoc_phan_nhom
        FOREIGN KEY (ma_nhom) REFERENCES nhom_hp(ma_nhom),

    CONSTRAINT ck_tc
        CHECK (so_tc > 0),

    CONSTRAINT ck_hoc_phan_trong_so
        CHECK (
            tscc >= 0 AND
            tsgk >= 0 AND
            tsthi >= 0 AND
            tscc + tsgk + tsthi = 100
        )
);

-- 8. Giang vien
CREATE TABLE giang_vien (
    ma_gv VARCHAR(10) PRIMARY KEY,
    ten VARCHAR(100) NOT NULL,
    ngay_sinh DATE,
    gioi_tinh VARCHAR(10),
    sdt VARCHAR(15),
    email VARCHAR(100),
    hoc_vi VARCHAR(50),
    ma_khoa VARCHAR(10) NOT NULL,

    CONSTRAINT uq_giang_vien_email UNIQUE (email),

    CONSTRAINT fk_giang_vien_khoa
        FOREIGN KEY (ma_khoa) REFERENCES khoa(ma_khoa)
);

-- 9. Sinh vien
CREATE TABLE sinh_vien (
    ma_sv VARCHAR(10) PRIMARY KEY,
    ten VARCHAR(100) NOT NULL,
    ngay_sinh DATE,
    gioi_tinh VARCHAR(10),
    sdt VARCHAR(15),
    email VARCHAR(100),
    ma_lop VARCHAR(10) NOT NULL,
    ma_lop_tc VARCHAR(10),

    CONSTRAINT uq_sinh_vien_email UNIQUE (email),

    CONSTRAINT fk_sinh_vien_lop
        FOREIGN KEY (ma_lop) REFERENCES lop_hanh_chinh(ma_lop),

    CONSTRAINT ck_sinh_vien_gioi_tinh
        CHECK (gioi_tinh IN ('nam', 'nu') OR gioi_tinh IS NULL)
);

-- 10. Lop tin chi
CREATE TABLE lop_tin_chi (
    ma_lop_tc VARCHAR(10) PRIMARY KEY,
    si_so_toi_da INT NOT NULL,
    ma_hk VARCHAR(10) NOT NULL,
    ma_gv VARCHAR(10) NOT NULL,
    ma_hp VARCHAR(10) NOT NULL,

    CONSTRAINT ck_lop_tin_chi_si_so
        CHECK (si_so_toi_da > 0),

    CONSTRAINT fk_lop_tin_chi_hoc_ky
        FOREIGN KEY (ma_hk) REFERENCES hoc_ky(ma_hk),

    CONSTRAINT fk_lop_tin_chi_giang_vien
        FOREIGN KEY (ma_gv) REFERENCES giang_vien(ma_gv),

    CONSTRAINT fk_lop_tin_chi_hoc_phan
        FOREIGN KEY (ma_hp) REFERENCES hoc_phan(ma_hp)
);

-- 11. Dang ky
CREATE TABLE dang_ky (
    ma_sv VARCHAR(10),
    ma_lop_tc VARCHAR(10),
    ngay_dk DATE NOT NULL,
    diem_cc DECIMAL(4,2),
    diem_gk DECIMAL(4,2),
    diem_thi DECIMAL(4,2),

    PRIMARY KEY (ma_sv, ma_lop_tc),

    CONSTRAINT fk_dang_ky_sinh_vien
        FOREIGN KEY (ma_sv) REFERENCES sinh_vien(ma_sv),

    CONSTRAINT fk_dang_ky_lop_tin_chi
        FOREIGN KEY (ma_lop_tc) REFERENCES lop_tin_chi(ma_lop_tc),

    CONSTRAINT ck_dang_ky_diem
        CHECK (
            (diem_cc IS NULL OR diem_cc BETWEEN 0 AND 10) AND
            (diem_gk IS NULL OR diem_gk BETWEEN 0 AND 10) AND
            (diem_thi IS NULL OR diem_thi BETWEEN 0 AND 10)
        )
);

-- 12. Hoc phi
CREATE TABLE hoc_phi (
    ma_sv VARCHAR(10),
    ma_hk VARCHAR(10),
    so_tien DECIMAL(15,2) NOT NULL,
    trang_thai VARCHAR(30) NOT NULL,
    ngay_nop DATE,

    PRIMARY KEY (ma_sv, ma_hk),

    CONSTRAINT fk_hoc_phi_sinh_vien
        FOREIGN KEY (ma_sv) REFERENCES sinh_vien(ma_sv),

    CONSTRAINT fk_hoc_phi_hoc_ky
        FOREIGN KEY (ma_hk) REFERENCES hoc_ky(ma_hk),

    CONSTRAINT ck_hoc_phi_so_tien
        CHECK (so_tien >= 0),

    CONSTRAINT ck_hoc_phi_trang_thai
        CHECK (trang_thai IN ('da_nop', 'chua_nop'))
);

-- 13. Ket qua
CREATE TABLE ket_qua (
    ma_sv VARCHAR(10),
    ma_hk VARCHAR(10),
    drl INT,
    muc_canh_bao VARCHAR(50),

    PRIMARY KEY (ma_sv, ma_hk),

    CONSTRAINT fk_ket_qua_sinh_vien
        FOREIGN KEY (ma_sv) REFERENCES sinh_vien(ma_sv),

    CONSTRAINT fk_ket_qua_hoc_ky
        FOREIGN KEY (ma_hk) REFERENCES hoc_ky(ma_hk),

    CONSTRAINT ck_ket_qua_drl
        CHECK (drl IS NULL OR drl BETWEEN 0 AND 100)
);

-- 14. Thoi khoa bieu
CREATE TABLE tkb (
    ma_tkb VARCHAR(10) PRIMARY KEY,
    tiet_bd INT NOT NULL,
    thu INT NOT NULL,
    so_tiet INT NOT NULL,
    tuan_bd INT NOT NULL,
    tuan_kt INT NOT NULL,
    ma_lop_tc VARCHAR(10) NOT NULL,
    ma_ph VARCHAR(10) NOT NULL,

    CONSTRAINT fk_tkb_lop_tin_chi
        FOREIGN KEY (ma_lop_tc) REFERENCES lop_tin_chi(ma_lop_tc),

    CONSTRAINT fk_tkb_phong_hoc
        FOREIGN KEY (ma_ph) REFERENCES phong_hoc(ma_ph),

    CONSTRAINT ck_tkb_thu
        CHECK (thu BETWEEN 2 AND 8),

    CONSTRAINT ck_tkb_tiet
        CHECK (tiet_bd > 0 AND so_tiet > 0),

    CONSTRAINT ck_tkb_tuan
        CHECK (tuan_bd > 0 AND tuan_kt >= tuan_bd)
);

-- 15. Chuong trinh
CREATE TABLE chuong_trinh (
    ma_nganh VARCHAR(10),
    ma_hp VARCHAR(10),
    ky_thu INT NOT NULL,
    tinh_bat_buoc BOOLEAN NOT NULL,

    PRIMARY KEY (ma_nganh, ma_hp),

    CONSTRAINT fk_chuong_trinh_nganh
        FOREIGN KEY (ma_nganh) REFERENCES nganh(ma_nganh),

    CONSTRAINT fk_chuong_trinh_hoc_phan
        FOREIGN KEY (ma_hp) REFERENCES hoc_phan(ma_hp),

    CONSTRAINT ck_chuong_trinh_ky_thu
        CHECK (ky_thu > 0)
);

-- 16. Tien quyet
CREATE TABLE tien_quyet (
    ma_hp1 VARCHAR(10),
    ma_hp2 VARCHAR(10),

    PRIMARY KEY (ma_hp1, ma_hp2),

    CONSTRAINT fk_tien_quyet_hoc_phan_1
        FOREIGN KEY (ma_hp1) REFERENCES hoc_phan(ma_hp),

    CONSTRAINT fk_tien_quyet_hoc_phan_2
        FOREIGN KEY (ma_hp2) REFERENCES hoc_phan(ma_hp),

    CONSTRAINT ck_tien_quyet_khac_nhau
        CHECK (ma_hp1 <> ma_hp2)
);

CREATE TABLE tai_khoan (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    ten_admin  VARCHAR(50) UNIQUE,
    ma_sv      VARCHAR(10) UNIQUE,
    mat_khau   VARCHAR(50) NOT NULL,
    vai_tro    VARCHAR(10) NOT NULL,
    CONSTRAINT ck_tai_khoan_vai_tro CHECK (vai_tro IN ('admin', 'sinh_vien')),
    CONSTRAINT fk_tai_khoan_sinh_vien FOREIGN KEY (ma_sv)
        REFERENCES sinh_vien(ma_sv) ON DELETE CASCADE ON UPDATE CASCADE
);