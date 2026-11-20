USE QUAN_LY_TIN_CHI;
-- 1
select sv.ma_sv, sv.ten, sv.gioi_tinh, sv.ngay_sinh, sv.email,
       round(avg(k.drl), 1) as drl_tb
from sinh_vien sv
join ket_qua k on k.ma_sv = sv.ma_sv
where sv.ma_lop = 'D24CQKH02'
group by sv.ma_sv
having avg(k.drl) > 80;

-- 2
select t.thu, t.tiet_bd, t.so_tiet, t.tuan_bd, t.tuan_kt,
       hp.ten_hp, p.ten_phong, p.toa, gv.ten as ten_giang_vien
from dang_ky d
join lop_tin_chi l on l.ma_lop_tc = d.ma_lop_tc
join hoc_ky h      on h.ma_hk     = l.ma_hk
join hoc_phan hp   on hp.ma_hp    = l.ma_hp
join giang_vien gv on gv.ma_gv    = l.ma_gv
join tkb t         on t.ma_lop_tc = l.ma_lop_tc
join phong_hoc p   on p.ma_ph     = t.ma_ph
where d.ma_sv = 'SV001' and h.trang_thai = 'dang_mo'
order by t.thu, t.tiet_bd;

-- 3
select v.ma_sv, v.ten_sinh_vien, v.ma_hk,
	   round(sum(v.diem_he4 * v.so_tc)/sum(v.so_tc),2) as gpa
from vw_diem_he4 v
join sinh_vien sv on sv.ma_sv = v.ma_sv
join lop_hanh_chinh l on sv.ma_lop = l.ma_lop
join nganh n on l.ma_nganh = n.ma_nganh
join khoa k on n.ma_khoa = k.ma_khoa
where k.ten_khoa = 'Cong nghe thong tin'
and v.diem_he4 is not null
group by v.ma_sv, v.ten_sinh_vien, v.ma_hk 
order by v.ma_sv, v.ma_hk;

-- 4
select ma_lop_tc,
       count(diem_tong_ket) as so_sv,
       sum(diem_tong_ket < 4.0) as so_truot,
       round(100 * sum(diem_tong_ket < 4.0) / count(diem_tong_ket), 1) as ty_le_truot
from vw_diem_tong_ket
group by ma_lop_tc
having sum(diem_tong_ket < 4.0) / count(diem_tong_ket) > 0.3;

-- 5
select h.ma_hp, h.ten_hp, l.ma_lop_tc
from hoc_phan h
left join lop_tin_chi l on l.ma_hp = h.ma_hp
and l.ma_hk in (select ma_hk from hoc_ky where trang_thai = 'dang_mo')
order by h.ma_hp;

-- 6
select distinct sv.ma_sv, sv.ten, sv.ma_lop
from sinh_vien sv
join dang_ky d on sv.ma_sv = d.ma_sv
join lop_tin_chi l on l.ma_lop_tc = d.ma_lop_tc
join hoc_ky h on l.ma_hk = h.ma_hk 
where h.trang_thai = 'dang_mo' 
and not exists (
	select 1
    from hoc_phi hp
    where hp.ma_sv = sv.ma_sv
    and hp.ma_hk = h.ma_hk
    and hp.trang_thai = 'da_nop'
);

-- 7
select sv.ma_sv, sv.ten
from sinh_vien sv
join vw_diem_he4 v on sv.ma_sv = v.ma_sv
join hoc_phan h on v.ma_hp = h.ma_hp
join nhom_hp n on h.ma_nhom = n.ma_nhom
where n.ten_nhom = 'Ky nang mem'
  and v.da_dat = 1
group by sv.ma_sv
having count(distinct h.ma_hp) = (
    select count(*)
    from hoc_phan h2
    join nhom_hp n2 on h2.ma_nhom = n2.ma_nhom
    where n2.ten_nhom = 'Ky nang mem'
);

-- 8
with gpa as (
    select d.ma_sv,
           round(sum(d.diem_he4 * d.so_tc) / sum(d.so_tc), 2) as gpa_tich_luy
    from vw_diem_tinh_gpa d
    group by d.ma_sv
),
xh as (
    select sv.ma_sv, sv.ten, l.ma_nganh, g.gpa_tich_luy,
           rank()   over (partition by l.ma_nganh order by g.gpa_tich_luy desc) as hang,
           count(*) over (partition by l.ma_nganh) as si_so
    from gpa g
    join sinh_vien sv     on sv.ma_sv = g.ma_sv
    join lop_hanh_chinh l on l.ma_lop = sv.ma_lop
)
select ma_nganh, ma_sv, ten, gpa_tich_luy, hang
from xh
where hang <= ceiling(si_so * 0.01);

-- 9
with recursive ds_tien_quyet as (
    select tq.ma_hp1 as ma_hp, 1 as cap
    from tien_quyet tq
    join hoc_phan hp on hp.ma_hp = tq.ma_hp2
    where hp.ten_hp = 'Do an tot nghiep'
    union all
    select tq.ma_hp1, t.cap + 1
    from tien_quyet tq
    join ds_tien_quyet t on tq.ma_hp2 = t.ma_hp
)
select hp.ma_hp, hp.ten_hp, hp.so_tc,
       min(t.cap) as cap,
       case when min(t.cap) = 1 then 'Trực tiếp' else 'Gián tiếp' end as loai
from ds_tien_quyet t
join hoc_phan hp on hp.ma_hp = t.ma_hp
group by hp.ma_hp, hp.ten_hp, hp.so_tc
order by cap, hp.ma_hp;
