# Quản lý tín chỉ

Ứng dụng desktop quản lý đào tạo theo hệ thống tín chỉ, được xây dựng bằng **Java Swing** và **MySQL**. Ứng dụng hỗ trợ hai vai trò chính: quản trị viên và sinh viên.

## Công nghệ sử dụng

- Java (khuyến nghị JDK 17 trở lên)
- Java Swing – giao diện desktop
- MySQL – hệ quản trị cơ sở dữ liệu
- JDBC – kết nối Java với MySQL
- SQL – tạo cấu trúc, dữ liệu mẫu, view và trigger

> Dự án hiện không kèm Maven/Gradle build file. Khi chạy trong IDE, cần tự thêm MySQL Connector/J vào Libraries/Classpath.

## Chức năng chính

### Quản trị viên

- Đăng nhập bằng tài khoản quản trị.
- Quản lý dữ liệu đào tạo thông qua các màn hình thêm, xem, sửa, xóa (CRUD), gồm khoa, ngành, nhóm học phần, lớp hành chính, phòng học, học phần, giảng viên, sinh viên, học kỳ, lớp tín chỉ, thời khóa biểu, đăng ký/điểm, học phí, kết quả và chương trình đào tạo.
- Xem các báo cáo/truy vấn phục vụ quản lý như điểm rèn luyện cao, thời khóa biểu, GPA theo khoa, tỷ lệ trượt, học phần và lớp đang mở, học phí chưa nộp, hoàn thành nhóm học phần, học bổng và môn tiên quyết.

### Sinh viên

- Đăng nhập bằng email đã lưu trong hồ sơ sinh viên.
- Xem và cập nhật thông tin cá nhân.
- Đăng ký hoặc hủy đăng ký lớp tín chỉ thuộc học kỳ đang mở.
- Xem thời khóa biểu, bảng điểm, GPA và học phí của bản thân.
- Đổi mật khẩu.

Việc đăng ký lớp được kiểm tra ở tầng cơ sở dữ liệu bằng trigger, bao gồm điều kiện tiên quyết, học phần đã đạt/đang học, giới hạn tín chỉ đối với sinh viên bị cảnh báo học vụ, sĩ số và trùng lịch. Sinh viên không thể hủy đăng ký nếu bản ghi đã có điểm.

## Cấu trúc thư mục

```text
src/
├── DB_Design/
│   ├── Lược đồ ER.drawio
│   └── Lược đồ Quan_he.drawio
├── main/
│   └── java/qltc/
│       ├── Main.java
│       ├── auth/          # Xác thực và quản lý phiên đăng nhập
│       ├── db/            # Kết nối JDBC và tiện ích truy vấn SQL
│       └── ui/            # Các màn hình và thành phần giao diện
└── SQL/
    ├── CREATE_DB.sql      # Tạo cơ sở dữ liệu
    ├── schema.sql         # Tạo bảng và ràng buộc
    ├── seed.sql           # Dữ liệu mẫu
    ├── views.sql          # Các view tính điểm
    ├── triggers.sql       # Quy tắc nghiệp vụ đăng ký
    └── queries.sql        # Các truy vấn tham khảo
```

## Cài đặt và chạy

### 1. Chuẩn bị môi trường

Cài đặt:

- JDK 17 trở lên.
- MySQL Server.
- Một IDE Java như IntelliJ IDEA, Eclipse hoặc NetBeans.
- MySQL Connector/J tương thích với phiên bản MySQL đang dùng.

### 2. Tạo cơ sở dữ liệu

Mở MySQL Workbench hoặc MySQL client và thực thi các file SQL theo thứ tự:

1. `SQL/CREATE_DB.sql` – tạo database `QUAN_LY_TIN_CHI`.
2. `SQL/schema.sql` – tạo các bảng và khóa/ràng buộc.
3. `SQL/seed.sql` – nạp dữ liệu mẫu.
4. `SQL/views.sql` – tạo các view phục vụ tính điểm.
5. `SQL/triggers.sql` – tạo trigger kiểm tra quy tắc nghiệp vụ.

**Lưu ý:** `CREATE_DB.sql` trong repository hiện có cả lệnh `CREATE DATABASE` và `DROP DATABASE`. Nếu chạy nguyên file, database sẽ bị xóa ở lệnh `DROP DATABASE`. Hãy bỏ/comment lệnh `DROP DATABASE QUAN_LY_TIN_CHI;` trước khi chạy nếu không muốn xóa dữ liệu hiện có.

### 3. Cấu hình kết nối

Tạo file `db.properties` tại thư mục làm việc gốc khi chạy ứng dụng (working directory), với nội dung:

```properties
db.url=jdbc:mysql://localhost:3306/QUAN_LY_TIN_CHI?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
db.user=root
db.password=YOUR_MYSQL_PASSWORD
```

Thay `YOUR_MYSQL_PASSWORD` bằng mật khẩu MySQL của bạn. Không đưa file chứa mật khẩu thật lên GitHub.

Ứng dụng đọc các khóa `db.url`, `db.user` và `db.password` từ `db.properties`.

### 4. Cấu hình project Java

1. Mở thư mục `src` bằng IDE hoặc tạo Java project và thêm các file trong `main/java`.
2. Đặt source root tương ứng để package `qltc` được nhận diện.
3. Thêm MySQL Connector/J vào classpath/dependencies.
4. Đảm bảo `db.properties` nằm trong working directory khi chạy.

### 5. Khởi chạy

Chạy lớp:

```text
qltc.Main
```

Ứng dụng sẽ kiểm tra kết nối database, khởi tạo tài khoản và mở màn hình đăng nhập.

## Tài khoản mặc định

Theo mã nguồn hiện tại:

| Vai trò | Tên đăng nhập | Mật khẩu mặc định |
|---|---|---|
| Quản trị viên | `admin` | `admin123` |
| Sinh viên | Email trong bảng `sinh_vien` | `1234` |

Khi khởi động, ứng dụng tạo tài khoản quản trị mặc định nếu chưa tồn tại và cấp tài khoản cho sinh viên có email nhưng chưa có tài khoản.

Sinh viên nên đổi mật khẩu sau lần đăng nhập đầu tiên. Đây là cấu hình phục vụ đồ án/minh họa; mã nguồn hiện lưu mật khẩu dạng văn bản thường, không nên dùng nguyên trạng cho môi trường thực tế.

## Ghi chú

- Database phải được khởi tạo đầy đủ bảng, dữ liệu mẫu, view và trigger trước khi sử dụng các chức năng tương ứng.
- Trigger là nơi thực thi một số quy tắc đăng ký lớp; không nên bỏ qua file `triggers.sql`.
- Tên đăng nhập sinh viên là email trong bảng `sinh_vien`.
- File `db.properties` chứa thông tin kết nối riêng của từng máy và không nên commit lên repository.

## Tác giả

Bổ sung tên tác giả/nhóm thực hiện, lớp và thông tin môn học tại đây.
