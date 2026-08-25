# 🗄️ GIÁO TRÌNH LIQUIBASE TOÀN DIỆN: QUẢN LÝ PHIÊN BẢN CSDL BẰNG CODE (DATABASE MIGRATION)

> 🎯 **Mục tiêu**: Hiểu trọn vẹn bản chất của Database Migration (Git cho CSDL), giải phẫu cơ chế tính mã băm MD5Sum, xử lý dứt điểm các lỗi Checksum/Khóa DB (`DATABASECHANGELOGLOCK`), và cung cấp **Bộ Nhiệm Vụ Thực Hành (Project Quests)** để tự tay quản lý CSDL trong dự án mới!
> ⏱️ **Thời gian đọc**: ~15 phút.

---

## 📚 PHẦN 1: GIẢI PHÃ BẢN CHẤT LÝ THUYẾT (DEEP DIVE THEORY)

---

### 1. Vấn Đề Cốt Lõi: Thảm Họa Khi Sửa Database Bằng Tay (Thủ Công)

Trong các dự án thực tế, mã nguồn Java được quản lý rất chặt chẽ bằng **Git** (ai sửa gì, commit nào đều có lịch sử). Nhưng CSDL MySQL thường bị sửa thủ công bằng MySQL Workbench hoặc phpMyAdmin:

```text
[ THẢM HỌA KHI SỬA DATABASE BẰNG TAY ]

1. Lập trình viên A gõ: ALTER TABLE users ADD COLUMN phone VARCHAR(15);
2. Lập trình viên A sửa file Java: private String phone; rồi push code lên Git.
3. Lập trình viên B kéo code về và bấm Run App:
   ├── Code Java thì CÓ trường 'phone'
   └── Nhưng MySQL của máy B thì KHÔNG CÓ cột 'phone'!
   👉 SẬP ỨNG DỤNG NGAY LẬP TỨC: "Unknown column 'phone' in 'field list'"
```

### 🛡️ Cứu Cánh Của Liquibase: Đồng Bộ Schema Tự Động 100%
* Mọi câu lệnh tạo bảng, thêm cột, sửa khóa ngoại đều được viết thành **File Code (Changeset)**.
* File này được lưu trong Git cùng với code Java.
* Khi bất kỳ ai (Đồng nghiệp, Docker, Server Production) bật ứng dụng lên $\rightarrow$ Liquibase sẽ tự động đọc và thực thi tất cả các câu lệnh SQL mới nhất vào Database!

---

### 2. Dòng Chảy Hoạt Động & Cơ Chế Kiểm Soát Bằng 2 Bảng Hệ Thống

Khi Spring Boot khởi động, Liquibase tự động tạo và quản lý **2 bảng đặc biệt** trong MySQL:

```text
[ Spring Boot khởi động ]
            │
            ▼ (1. Khóa CSDL chống nhiều máy cùng chạy đè nhau)
[ Bảng DATABASECHANGELOGLOCK: SET LOCKED = 1 ]
            │
            ▼ (2. Đọc file tổng db.changelog-master.xml)
[ Quét qua từng ChangeSet con trong thư mục changes/ ]
            │
            ▼ (3. Tra cứu bảng DATABASECHANGELOG xem ChangeSet này đã chạy chưa?)
            │
    ┌───────┴──────────────────────────────────────────────────────┐
    ▼                                                              ▼
[ ĐÃ CHẠY RỒI ]                                            [ CHƯA CHẠY (MỚI) ]
  1. So sánh mã băm MD5Sum cũ vs mới                         1. Thực thi câu lệnh SQL (Tạo bảng/Thêm cột)
  2. Nếu trùng khớp ──► BỎ QUA (Không chạy lại)              2. Tính mã băm MD5Sum của nội dung ChangeSet
  3. Nếu bị sửa đổi ──► BÁO LỖI Checksum Changed!            3. Ghi id, author, md5sum vào DATABASECHANGELOG
                                                             4. Đánh dấu hoàn thành!
            │
            ▼ (4. Mở khóa CSDL)
[ Bảng DATABASECHANGELOGLOCK: SET LOCKED = 0 ] ──► Spring Boot tiếp tục khởi động!
```

---

### 3. Giải Phẫu 2 Bảng Hệ Thống Của Liquibase

1. **Bảng `DATABASECHANGELOG` (Nhật ký thay đổi)**:
   * `ID`: Tên định danh của changeset (Ví dụ: `001-create-users-table`).
   * `AUTHOR`: Tên người viết (Ví dụ: `admin`, `nhan`).
   * `FILENAME`: Đường dẫn file changeset.
   * `DATEEXECUTED`: Ngày giờ chính xác câu lệnh được chạy trên máy này.
   * `MD5SUM`: **Mã vân tay bảo mật** của nội dung file changeset. Dùng để đảm bảo không ai được phép sửa lén file cũ sau khi đã chạy!

2. **Bảng `DATABASECHANGELOGLOCK` (Chiếc khóa an toàn)**:
   * Khi ứng dụng khởi động, cột `LOCKED` chuyển thành `1` (Khóa lại).
   * Mục đích: Nếu bạn triển khai ứng dụng trên cụm 5 server cùng lúc, chỉ có 1 server đầu tiên được phép chạy migration DB, 4 server còn lại phải đứng chờ (Tránh xung đột tạo bảng trùng lặp!).

---

### 4. Bảng So Sánh 3 Định Dạng Changeset: XML vs YAML vs SQL Thuần

| Định dạng | Ưu điểm | Nhược điểm | Khuyên dùng khi nào? |
| :--- | :--- | :--- | :--- |
| **YAML (`.yaml`)** | Cực kỳ ngắn gọn, dễ đọc, cú pháp hiện đại. | Dễ thụt lề sai khoảng trắng (Indent). | ✅ Khuyên dùng cho dự án mới, sạch đẹp. |
| **XML (`.xml`)** | Chuẩn tắc, IDE tự gợi ý thẻ (Autocomplete) chuẩn 100%. | Hơi dài dòng do đóng mở thẻ `<column>`. | ✅ Dùng làm file Master tổng hoặc khi cần kiểm tra cú pháp chặt chẽ. |
| **SQL (`.sql`)** | Viết trực tiếp câu lệnh SQL quen thuộc. | Phụ thuộc vào từng loại CSDL (khó chuyển từ MySQL sang Postgres). | Dùng khi có các Stored Procedure / Trigger phức tạp. |

---

## 🚨 PHẦN 2: BẢNG BẮT BỆNH LỖI LIQUIBASE THƯỜNG GẶP (TROUBLESHOOTING)

| Triệu chứng lỗi | Nguyên nhân cốt lõi | Cách xử lý chuẩn xác |
| :--- | :--- | :--- |
| **`Validation Failed: ChangeSet checksum changed`** | Bạn đã sửa lại nội dung của một file changeset **đã từng chạy trong quá khứ**. Mã MD5Sum mới bị lệch với mã cũ trong DB. | **Quy tắc**: Tuyệt đối không sửa file cũ. Hãy tạo 1 file changeset MỚI (Ví dụ: `005-alter-column.xml`) để thêm thay đổi. Hoặc nếu đang ở môi trường DEV, xóa bảng `DATABASECHANGELOG` và chạy lại từ đầu. |
| **`Waiting for changelog lock... / Database is locked`** | Khi ứng dụng đang chạy migration thì bạn bấm tắt đột ngột (hoặc mất điện), khiến cột `LOCKED = 1` chưa kịp mở khóa. | Mở MySQL Workbench gõ: `UPDATE DATABASECHANGELOGLOCK SET LOCKED = 0;` rồi khởi động lại app! |

---

## 🎮 PHẦN 3: BỘ NHIỆM VỤ THỰC HÀNH DỰ ÁN MỚI (LIQUIBASE QUESTS)

---

### ⚔️ QUEST 1: Cấu Hình Liquibase Trong `application.properties`
* **Nhiệm vụ cần làm**:
  ```properties
  # Kích hoạt Liquibase tự động chạy khi khởi động app
  spring.liquibase.enabled=true
  # Chỉ định đường dẫn tới file Changelog Master tổng
  spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
  # Tắt tính năng tự sinh bảng của JPA Hibernate để nhường toàn quyền cho Liquibase
  spring.jpa.hibernate.ddl-auto=validate
  ```

---

### ⚔️ QUEST 2: Xây Dựng Cấu Trúc File Master & File Con
* **Nhiệm vụ cần làm**:
  1. Tạo thư mục: `src/main/resources/db/changelog/changes/`.
  2. Tạo file `db.changelog-master.xml`:
     ```xml
     <databaseChangeLog
         xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
             http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

         <include file="db/changelog/changes/001-create-initial-tables.xml"/>
         <include file="db/changelog/changes/002-create-products-table.xml"/>
     </databaseChangeLog>
     ```

---

### ⚔️ QUEST 3: Viết Changeset Tạo Bảng Có Khóa Ngoại & Rollback
* **Nhiệm vụ cần làm**: Viết file `002-create-products-table.xml`:
  ```xml
  <changeSet id="002-create-products" author="admin">
      <createTable tableName="products">
          <column name="product_id" type="bigint" autoIncrement="true">
              <constraints primaryKey="true" nullable="false"/>
          </column>
          <column name="product_name" type="varchar(150)">
              <constraints nullable="false"/>
          </column>
          <column name="price" type="decimal(15,2)">
              <constraints nullable="false"/>
          </column>
          <column name="category_id" type="bigint">
              <constraints nullable="false" foreignKeyName="fk_prod_cat" references="categories(category_id)"/>
          </column>
      </createTable>

      <rollback>
          <dropTable tableName="products"/>
      </rollback>
  </changeSet>
  ```

---

## 🏆 BOSS QUEST: THỬ THÁCH NÂNG CẤP SCHEMA CSDL KHÔNG LỖI

* 🧪 **Thử nghiệm**:
  1. Ứng dụng đang chạy mượt mà. Yêu cầu mới: Thêm cột `avatar_url VARCHAR(255)` vào bảng `users`.
  2. 👉 **Thực hiện đúng**: Tạo file mới `003-add-avatar-url.xml` $\rightarrow$ Include vào `db.changelog-master.xml` $\rightarrow$ Bật app lên.
  3. Mở MySQL kiểm tra: Bảng `users` đã tự động có thêm cột `avatar_url`, và bảng `DATABASECHANGELOG` tự động xuất hiện thêm dòng ghi nhận changeset `003` hoàn hảo!
