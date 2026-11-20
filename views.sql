USE QUAN_LY_TIN_CHI;

CREATE VIEW vw_diem_tong_ket AS
SELECT
    dk.ma_sv,
    sv.ten AS ten_sinh_vien,
    dk.ma_lop_tc,
	ltc.ma_hk,
    hp.ma_hp,
    hp.ten_hp,
    hp.so_tc,
    dk.diem_cc,
    dk.diem_gk,
    dk.diem_thi,
    hp.tscc,
    hp.tsgk,
    hp.tsthi,

    CASE
        WHEN dk.diem_cc IS NOT NULL
         AND dk.diem_gk IS NOT NULL
         AND dk.diem_thi IS NOT NULL
        THEN ROUND(
            (
                dk.diem_cc * hp.tscc +
                dk.diem_gk * hp.tsgk +
                dk.diem_thi * hp.tsthi
            ) / 100,
            2
        )
        ELSE NULL
    END AS diem_tong_ket

FROM dang_ky dk
JOIN sinh_vien sv
    ON dk.ma_sv = sv.ma_sv
JOIN lop_tin_chi ltc
    ON dk.ma_lop_tc = ltc.ma_lop_tc
JOIN hoc_phan hp
    ON ltc.ma_hp = hp.ma_hp;
    
    
CREATE VIEW vw_diem_he4 AS
SELECT
    t.ma_sv,
    t.ten_sinh_vien,
    t.ma_lop_tc,
    t.ma_hk,
    t.ma_hp,
    t.ten_hp,
    t.so_tc,
    t.diem_tong_ket,

    CASE
        WHEN t.diem_tong_ket IS NULL THEN NULL
        WHEN t.diem_tong_ket >= 8.5 THEN 4.0
        WHEN t.diem_tong_ket >= 8.0 THEN 3.5
        WHEN t.diem_tong_ket >= 7.0 THEN 3.0
        WHEN t.diem_tong_ket >= 6.5 THEN 2.5
        WHEN t.diem_tong_ket >= 5.5 THEN 2.0
        WHEN t.diem_tong_ket >= 5.0 THEN 1.5
        WHEN t.diem_tong_ket >= 4.0 THEN 1.0
        ELSE 0.0
    END AS diem_he4,

    CASE
        WHEN t.diem_tong_ket IS NULL THEN NULL
        WHEN t.diem_tong_ket >= 8.5 THEN 'A'
        WHEN t.diem_tong_ket >= 8.0 THEN 'B+'
        WHEN t.diem_tong_ket >= 7.0 THEN 'B'
        WHEN t.diem_tong_ket >= 6.5 THEN 'C+'
        WHEN t.diem_tong_ket >= 5.5 THEN 'C'
        WHEN t.diem_tong_ket >= 5.0 THEN 'D+'
        WHEN t.diem_tong_ket >= 4.0 THEN 'D'
        ELSE 'F'
    END AS diem_chu,

    CASE
        WHEN t.diem_tong_ket IS NULL THEN NULL
        WHEN t.diem_tong_ket >= 4.0 THEN 1
        ELSE 0
    END AS da_dat
FROM vw_diem_tong_ket t;

CREATE VIEW vw_diem_tinh_gpa AS
SELECT ma_sv, ten_sinh_vien, ma_lop_tc, ma_hk, ma_hp, ten_hp, so_tc,
       diem_tong_ket, diem_he4, diem_chu, da_dat
FROM (
    SELECT v.*,
           ROW_NUMBER() OVER (PARTITION BY v.ma_sv, v.ma_hp
                              ORDER BY v.diem_tong_ket DESC, v.ma_hk DESC) AS rn
    FROM vw_diem_he4 v
    WHERE v.diem_tong_ket IS NOT NULL
) x
WHERE rn = 1;