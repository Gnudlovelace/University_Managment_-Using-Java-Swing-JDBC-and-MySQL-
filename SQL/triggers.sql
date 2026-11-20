USE QUAN_LY_TIN_CHI;

DELIMITER //

-- Trigger 1: Kiem tra si so va trung lich khi dang ky
CREATE TRIGGER trg_dang_ky_insert
BEFORE INSERT ON dang_ky
FOR EACH ROW
BEGIN
    DECLARE v_si_so INT;
    DECLARE v_si_so_toi_da INT; 
    DECLARE v_trung_lich INT;
    DECLARE v_tien_quyet_thieu VARCHAR(100);
    DECLARE v_canh_bao VARCHAR(50);
    DECLARE v_tong_tc INT;
    DECLARE v_tc_lop INT;
    DECLARE v_ma_hk VARCHAR(20);
    DECLARE v_msg VARCHAR(128);
    DECLARE v_dang_hoc INT;
    DECLARE v_da_dat INT;

    -- Hoc lai: chi duoc dang ky neu moi lan hoc truoc cua hoc phan nay deu da co diem va < 4.0
    SELECT COALESCE(SUM(t.diem_tong_ket IS NULL), 0), COALESCE(SUM(t.diem_tong_ket >= 4.0), 0)
    INTO v_dang_hoc, v_da_dat
    FROM vw_diem_tong_ket t
    JOIN lop_tin_chi l ON l.ma_lop_tc = NEW.ma_lop_tc
    WHERE t.ma_sv = NEW.ma_sv AND t.ma_hp = l.ma_hp;

    IF v_da_dat > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Sinh vien da dat hoc phan nay';
    END IF;

    IF v_dang_hoc > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Sinh vien dang hoc hoac chua co diem hoc phan nay';
    END IF;

    -- Luat 1: mon tien quyet phai co diem tong ket >= 4.0 (o bat ky lan hoc nao)
    SELECT GROUP_CONCAT(tq.ma_hp1 SEPARATOR ', ')
    INTO v_tien_quyet_thieu
    FROM tien_quyet tq
    JOIN lop_tin_chi l ON l.ma_lop_tc = NEW.ma_lop_tc AND l.ma_hp = tq.ma_hp2
    WHERE NOT EXISTS (
        SELECT 1 FROM vw_diem_tong_ket t
        WHERE t.ma_sv = NEW.ma_sv AND t.ma_hp = tq.ma_hp1 AND t.diem_tong_ket >= 4.0
    );

    IF v_tien_quyet_thieu IS NOT NULL THEN
        SET v_msg = LEFT(CONCAT('Chua dat mon tien quyet: ', v_tien_quyet_thieu), 128);
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = v_msg;
    END IF;

    -- Luat 2: sinh vien bi canh bao hoc vu chi duoc dang ky toi da 14 tin chi/hoc ky
    SELECT l.ma_hk, hp.so_tc INTO v_ma_hk, v_tc_lop
    FROM lop_tin_chi l JOIN hoc_phan hp ON hp.ma_hp = l.ma_hp
    WHERE l.ma_lop_tc = NEW.ma_lop_tc;

    -- Muc canh bao cua hoc ky gan nhat (tinh den hoc ky cua lop dang dang ky)
    SELECT k.muc_canh_bao INTO v_canh_bao
    FROM ket_qua k
    JOIN hoc_ky h ON h.ma_hk = k.ma_hk
    WHERE k.ma_sv = NEW.ma_sv
      AND h.ngay_bd <= (SELECT ngay_bd FROM hoc_ky WHERE ma_hk = v_ma_hk)
    ORDER BY h.ngay_bd DESC
    LIMIT 1;

    IF v_canh_bao IS NOT NULL AND TRIM(v_canh_bao) <> '' THEN
        SELECT COALESCE(SUM(hp.so_tc), 0) INTO v_tong_tc
        FROM dang_ky dk
        JOIN lop_tin_chi l ON l.ma_lop_tc = dk.ma_lop_tc AND l.ma_hk = v_ma_hk
        JOIN hoc_phan hp ON hp.ma_hp = l.ma_hp
        WHERE dk.ma_sv = NEW.ma_sv;

        IF v_tong_tc + v_tc_lop > 14 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Sinh vien bi canh bao hoc vu khong duoc dang ky qua 14 tin chi/hoc ky';
        END IF;
    END IF;

    -- Lay si so hien tai va si so toi da
    SELECT COUNT(*), ltc.si_so_toi_da
    INTO v_si_so, v_si_so_toi_da
    FROM dang_ky dk
    JOIN lop_tin_chi ltc
        ON dk.ma_lop_tc = ltc.ma_lop_tc
    WHERE dk.ma_lop_tc = NEW.ma_lop_tc
    GROUP BY ltc.si_so_toi_da;

    IF v_si_so IS NULL THEN
        SET v_si_so = 0;
        SELECT si_so_toi_da
        INTO v_si_so_toi_da
        FROM lop_tin_chi
        WHERE ma_lop_tc = NEW.ma_lop_tc;
    END IF;

    IF v_si_so >= v_si_so_toi_da THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lop tin chi da day si so';
    END IF;

    -- Kiem tra trung lich voi cac lop da dang ky
    SELECT COUNT(*)
	INTO v_trung_lich
	FROM lop_tin_chi l1
	JOIN tkb t1 ON t1.ma_lop_tc = l1.ma_lop_tc
	JOIN dang_ky dk ON dk.ma_sv = NEW.ma_sv
	JOIN lop_tin_chi l2 ON l2.ma_lop_tc = dk.ma_lop_tc AND l2.ma_hk = l1.ma_hk
	JOIN tkb t2 ON t2.ma_lop_tc = l2.ma_lop_tc
	WHERE l1.ma_lop_tc = NEW.ma_lop_tc
	  AND t1.thu = t2.thu
	  AND t1.tuan_bd <= t2.tuan_kt AND t1.tuan_kt >= t2.tuan_bd
	  AND t1.tiet_bd < t2.tiet_bd + t2.so_tiet
	  AND t1.tiet_bd + t1.so_tiet > t2.tiet_bd;

    IF v_trung_lich > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Sinh vien bi trung lich hoc';
    END IF;
END//

-- Trigger 2: Kiem tra si so va trung lich khi cap nhat dang ky
CREATE TRIGGER trg_dang_ky_update
BEFORE UPDATE ON dang_ky
FOR EACH ROW
BEGIN
    DECLARE v_si_so INT;
    DECLARE v_si_so_toi_da INT;
    DECLARE v_trung_lich INT;
    DECLARE v_tien_quyet_thieu VARCHAR(100);
    DECLARE v_canh_bao VARCHAR(50);
    DECLARE v_tong_tc INT;
    DECLARE v_tc_lop INT;
    DECLARE v_ma_hk VARCHAR(20);
    DECLARE v_msg VARCHAR(128);
    DECLARE v_dang_hoc INT;
    DECLARE v_da_dat INT;

    -- Chỉ kiểm tra khi chuyển sang lớp tín chỉ khác (sửa điểm thì bỏ qua)
    IF NEW.ma_lop_tc <> OLD.ma_lop_tc THEN

        -- Hoc lai (khong tinh lop cu dang duoc doi)
        SELECT COALESCE(SUM(t.diem_tong_ket IS NULL), 0), COALESCE(SUM(t.diem_tong_ket >= 4.0), 0)
        INTO v_dang_hoc, v_da_dat
        FROM vw_diem_tong_ket t
        JOIN lop_tin_chi l ON l.ma_lop_tc = NEW.ma_lop_tc
        WHERE t.ma_sv = NEW.ma_sv AND t.ma_hp = l.ma_hp AND t.ma_lop_tc <> OLD.ma_lop_tc;

        IF v_da_dat > 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Sinh vien da dat hoc phan nay';
        END IF;

        IF v_dang_hoc > 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Sinh vien dang hoc hoac chua co diem hoc phan nay';
        END IF;

        -- Luat 1: mon tien quyet phai co diem tong ket >= 4.0
        SELECT GROUP_CONCAT(tq.ma_hp1 SEPARATOR ', ')
        INTO v_tien_quyet_thieu
        FROM tien_quyet tq
        JOIN lop_tin_chi l ON l.ma_lop_tc = NEW.ma_lop_tc AND l.ma_hp = tq.ma_hp2
        WHERE NOT EXISTS (
            SELECT 1 FROM vw_diem_tong_ket t
            WHERE t.ma_sv = NEW.ma_sv AND t.ma_hp = tq.ma_hp1 AND t.diem_tong_ket >= 4.0
        );

        IF v_tien_quyet_thieu IS NOT NULL THEN
            SET v_msg = LEFT(CONCAT('Chua dat mon tien quyet: ', v_tien_quyet_thieu), 128);
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = v_msg;
        END IF;

        -- Luat 2: toi da 14 tin chi/hoc ky neu bi canh bao hoc vu (khong tinh lop cu dang doi)
        SELECT l.ma_hk, hp.so_tc INTO v_ma_hk, v_tc_lop
        FROM lop_tin_chi l JOIN hoc_phan hp ON hp.ma_hp = l.ma_hp
        WHERE l.ma_lop_tc = NEW.ma_lop_tc;

        SELECT k.muc_canh_bao INTO v_canh_bao
        FROM ket_qua k
        JOIN hoc_ky h ON h.ma_hk = k.ma_hk
        WHERE k.ma_sv = NEW.ma_sv
          AND h.ngay_bd <= (SELECT ngay_bd FROM hoc_ky WHERE ma_hk = v_ma_hk)
        ORDER BY h.ngay_bd DESC
        LIMIT 1;

        IF v_canh_bao IS NOT NULL AND TRIM(v_canh_bao) <> '' THEN
            SELECT COALESCE(SUM(hp.so_tc), 0) INTO v_tong_tc
            FROM dang_ky dk
            JOIN lop_tin_chi l ON l.ma_lop_tc = dk.ma_lop_tc AND l.ma_hk = v_ma_hk
            JOIN hoc_phan hp ON hp.ma_hp = l.ma_hp
            WHERE dk.ma_sv = NEW.ma_sv AND dk.ma_lop_tc <> OLD.ma_lop_tc;

            IF v_tong_tc + v_tc_lop > 14 THEN
                SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Sinh vien bi canh bao hoc vu khong duoc dang ky qua 14 tin chi/hoc ky';
            END IF;
        END IF;

        SELECT COUNT(*) INTO v_si_so
        FROM dang_ky
        WHERE ma_lop_tc = NEW.ma_lop_tc;

        SELECT si_so_toi_da INTO v_si_so_toi_da
        FROM lop_tin_chi
        WHERE ma_lop_tc = NEW.ma_lop_tc;

        IF v_si_so >= v_si_so_toi_da THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Lop tin chi da day si so';
        END IF;

        SELECT COUNT(*) INTO v_trung_lich
        FROM lop_tin_chi l1
        JOIN tkb t1 ON t1.ma_lop_tc = l1.ma_lop_tc
        JOIN dang_ky dk ON dk.ma_sv = NEW.ma_sv
                       AND dk.ma_lop_tc <> OLD.ma_lop_tc
        JOIN lop_tin_chi l2 ON l2.ma_lop_tc = dk.ma_lop_tc AND l2.ma_hk = l1.ma_hk
        JOIN tkb t2 ON t2.ma_lop_tc = l2.ma_lop_tc
        WHERE l1.ma_lop_tc = NEW.ma_lop_tc
          AND t1.thu = t2.thu
          AND t1.tuan_bd <= t2.tuan_kt AND t1.tuan_kt >= t2.tuan_bd
          AND t1.tiet_bd < t2.tiet_bd + t2.so_tiet
          AND t1.tiet_bd + t1.so_tiet > t2.tiet_bd;

        IF v_trung_lich > 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Sinh vien bi trung lich hoc';
        END IF;
    END IF;
END//

-- Trigger 3: Khong cho giam si so toi da thap hon si so hien tai
CREATE TRIGGER trg_lop_tin_chi_update
BEFORE UPDATE ON lop_tin_chi
FOR EACH ROW
BEGIN
    DECLARE v_si_so_hien_tai INT;

    SELECT COUNT(*)
    INTO v_si_so_hien_tai
    FROM dang_ky
    WHERE ma_lop_tc = OLD.ma_lop_tc;

    IF NEW.si_so_toi_da < v_si_so_hien_tai THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Si so toi da khong duoc nho hon si so hien tai';
    END IF;
END//

DELIMITER ;