# LIQUIBASE TRONG SPRING BOOT

Tài liệu này giải thích cách sử dụng Liquibase để quản lý lịch sử thay đổi database cho
SuperMarketAPI.

Mục tiêu chính:

- Cấu trúc database được mô tả bằng code và lưu trong Git.
- Không sửa database thủ công mà không có lịch sử.
- Mỗi thay đổi schema có một changeset riêng.
- Có thể tạo cùng một schema trên máy khác, môi trường test hoặc server triển khai.

---

## 1. Vấn đề khi sửa database thủ công

Khi chỉ chạy SQL trực tiếp trong MySQL Workbench, database trên máy hiện tại được thay đổi
nhưng source code không ghi lại lịch sử đó.

Ví dụ vừa sửa bảng Refresh Token:

```sql
ALTER TABLE refresh_tokens
    CHANGE COLUMN token token_hash VARCHAR(64) NOT NULL,
    ADD COLUMN revoked_at TIMESTAMP NULL;
```

Máy hiện tại có thay đổi, nhưng một developer khác hoặc server mới không biết cần chạy câu
SQL này.

Hậu quả có thể xảy ra:

```text
Máy A có cột revoked_at.
Máy B không có cột revoked_at.
Entity Java giống nhau nhưng ứng dụng chạy khác nhau.
```

Liquibase giải quyết vấn đề bằng cách lưu mỗi thay đổi database thành một changeset.

---

## 2. Liquibase hoạt động như thế nào?

Khi Spring Boot khởi động:

```text
Spring Boot khởi động
        |
Liquibase đọc master changelog
        |
Liquibase đọc DATABASECHANGELOG
        |
Changeset chưa chạy? ---- Không ---> Bỏ qua
        |
       Có
        |
Chạy changeset
        |
Ghi lịch sử vào DATABASECHANGELOG
        |
JPA/Hibernate tiếp tục khởi tạo
```

Liquibase tự tạo hai bảng quản lý:

```text
DATABASECHANGELOG
DATABASECHANGELOGLOCK
```

`DATABASECHANGELOG` lưu những changeset đã chạy.

`DATABASECHANGELOGLOCK` ngăn hai tiến trình cùng sửa schema tại một thời điểm.

---

## 3. Changeset là gì?

Changeset là một đơn vị thay đổi database.

Mỗi changeset được nhận diện bởi bộ ba:

```text
id + author + file path
```

Ví dụ:

```yaml
- changeSet:
    id: 001-create-roles-table
    author: nguyen-xuan-nhan
```

Không nên dùng cùng một `id` và `author` cho hai changeset trong cùng file.

Một changeset nên thực hiện một thay đổi có mục đích rõ ràng:

```text
001-create-roles-table
002-create-users-table
003-create-products-table
004-create-refresh-tokens-table
005-add-revoked-at-to-refresh-tokens
```

---

## 4. Thêm dependency Liquibase

Mở `pom.xml` và thêm:

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

Không cần ghi version vì Spring Boot dependency management quản lý version phù hợp.

Sau đó chạy:

```powershell
.\mvnw.cmd clean compile
```

---

## 5. Cấu trúc thư mục changelog

Tạo cấu trúc:

```text
src/main/resources/
└── db/
    └── changelog/
        ├── db.changelog-master.yaml
        └── changes/
            ├── 001-create-roles-table.yaml
            ├── 002-create-categories-table.yaml
            ├── 003-create-suppliers-table.yaml
            ├── 004-create-users-table.yaml
            ├── 005-create-products-table.yaml
            └── 006-create-refresh-tokens-table.yaml
```

Tên file có số thứ tự giúp người đọc thấy trình tự thay đổi.

---

## 6. Master changelog

File:

```text
src/main/resources/db/changelog/db.changelog-master.yaml
```

Nội dung:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-create-roles-table.yaml

  - include:
      file: db/changelog/changes/002-create-categories-table.yaml

  - include:
      file: db/changelog/changes/003-create-suppliers-table.yaml

  - include:
      file: db/changelog/changes/004-create-users-table.yaml

  - include:
      file: db/changelog/changes/005-create-products-table.yaml

  - include:
      file: db/changelog/changes/006-create-refresh-tokens-table.yaml
```

Master changelog không nhất thiết chứa SQL trực tiếp. Nó đóng vai trò danh sách các file
thay đổi cần chạy.

---

## 7. Cấu hình application.properties

Thêm:

```properties
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
```

Giữ cấu hình:

```properties
spring.jpa.hibernate.ddl-auto=none
```

Ý nghĩa:

```text
Liquibase quản lý schema.
Hibernate không tự tạo hoặc tự sửa bảng.
```

Không nên dùng đồng thời:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Nếu Hibernate tự sửa schema, Liquibase không còn là nguồn lịch sử duy nhất.

Sau khi schema đã ổn định, có thể cân nhắc:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

`validate` chỉ kiểm tra Entity có khớp schema hay không, không tự sửa database.

---

## 8. Ví dụ tạo bảng Role

File:

```text
001-create-roles-table.yaml
```

```yaml
databaseChangeLog:
  - changeSet:
      id: 001-create-roles-table
      author: nguyen-xuan-nhan
      changes:
        - createTable:
            tableName: roles
            columns:
              - column:
                  name: role_id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false

              - column:
                  name: role_name
                  type: VARCHAR(50)
                  constraints:
                    nullable: false
                    unique: true

              - column:
                  name: description
                  type: VARCHAR(255)

              - column:
                  name: created_at
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP
                  constraints:
                    nullable: false

      rollback:
        - dropTable:
            tableName: roles
```

`rollback` mô tả cách hoàn tác changeset khi cần.

---

## 9. Ví dụ tạo bảng Refresh Token

File:

```text
006-create-refresh-tokens-table.yaml
```

```yaml
databaseChangeLog:
  - changeSet:
      id: 006-create-refresh-tokens-table
      author: nguyen-xuan-nhan
      changes:
        - createTable:
            tableName: refresh_tokens
            columns:
              - column:
                  name: refresh_token_id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false

              - column:
                  name: user_id
                  type: BIGINT
                  constraints:
                    nullable: false

              - column:
                  name: token_hash
                  type: VARCHAR(64)
                  constraints:
                    nullable: false
                    unique: true

              - column:
                  name: expires_at
                  type: TIMESTAMP
                  constraints:
                    nullable: false

              - column:
                  name: revoked
                  type: BOOLEAN
                  defaultValueBoolean: false
                  constraints:
                    nullable: false

              - column:
                  name: created_at
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP
                  constraints:
                    nullable: false

              - column:
                  name: revoked_at
                  type: TIMESTAMP

        - addForeignKeyConstraint:
            baseTableName: refresh_tokens
            baseColumnNames: user_id
            constraintName: fk_refresh_tokens_users
            referencedTableName: users
            referencedColumnNames: user_id
            onDelete: CASCADE

      rollback:
        - dropTable:
            tableName: refresh_tokens
```

Bảng `users` phải được tạo trước `refresh_tokens`, vì refresh token có foreign key đến
User.

---

## 10. Ví dụ thay đổi bảng đã tồn tại

Giả sử bảng Refresh Token ban đầu chưa có `revoked_at`. Không sửa changeset tạo bảng đã
chạy. Tạo file mới:

```text
007-add-revoked-at-to-refresh-tokens.yaml
```

```yaml
databaseChangeLog:
  - changeSet:
      id: 007-add-revoked-at-to-refresh-tokens
      author: nguyen-xuan-nhan
      changes:
        - addColumn:
            tableName: refresh_tokens
            columns:
              - column:
                  name: revoked_at
                  type: TIMESTAMP

      rollback:
        - dropColumn:
            tableName: refresh_tokens
            columnName: revoked_at
```

Nguyên tắc:

```text
Changeset đã chạy -> không sửa lại.
Thay đổi mới       -> tạo changeset mới.
```

Liquibase lưu checksum của changeset. Sửa một changeset đã chạy có thể gây lỗi checksum
khi ứng dụng khởi động.

---

## 11. Thứ tự tạo bảng theo khóa ngoại

Schema hiện tại nên đi theo thứ tự:

```text
1. roles
2. categories
3. suppliers
4. users
5. products
6. refresh_tokens
```

Quan hệ:

```text
users.role_id               -> roles.role_id
products.category_id        -> categories.category_id
products.supplier_id        -> suppliers.supplier_id
refresh_tokens.user_id      -> users.user_id
```

Bảng được tham chiếu phải tồn tại trước khi thêm foreign key.

---

## 12. Database hiện tại đã có bảng thì làm sao?

Đây là trường hợp của SuperMarketAPI. Database `supermarket_api` đã được tạo thủ công.

Nếu thêm changeset `CREATE TABLE` rồi chạy ngay trên database này, Liquibase có thể báo:

```text
Table already exists
```

Có hai hướng.

### Hướng A: Học và thử trên database mới

Đây là hướng dễ hiểu và an toàn nhất khi đang học.

Tạo database mới:

```sql
CREATE DATABASE supermarket_liquibase_test;
```

Trỏ datasource sang database mới rồi khởi động ứng dụng. Liquibase sẽ tạo toàn bộ schema
từ đầu.

Sau đó kiểm tra:

```sql
SHOW TABLES;

SELECT * FROM DATABASECHANGELOG;
```

### Hướng B: Baseline database đang tồn tại

Với database cần giữ dữ liệu:

```text
1. Viết changelog mô tả đúng schema hiện tại.
2. Sao lưu database.
3. Kiểm tra changelog trên database mới trước.
4. Đánh dấu các changeset baseline là đã chạy bằng changelog-sync.
5. Từ thay đổi tiếp theo, Liquibase chạy migration thật.
```

Không tự chạy `changelog-sync` trên database quan trọng khi chưa có backup và chưa kiểm
tra changelog, vì lệnh này đánh dấu lịch sử mà không thực thi SQL.

---

## 13. Preconditions

Precondition kiểm tra trạng thái database trước khi chạy changeset.

Ví dụ chỉ thêm `revoked_at` nếu bảng tồn tại và cột chưa tồn tại:

```yaml
databaseChangeLog:
  - changeSet:
      id: 007-add-revoked-at-to-refresh-tokens
      author: nguyen-xuan-nhan
      preConditions:
        - onFail: HALT
        - tableExists:
            tableName: refresh_tokens
        - not:
            columnExists:
              tableName: refresh_tokens
              columnName: revoked_at

      changes:
        - addColumn:
            tableName: refresh_tokens
            columns:
              - column:
                  name: revoked_at
                  type: TIMESTAMP
```

Precondition giúp lỗi migration rõ ràng hơn, nhưng không nên dùng để che giấu một schema
không thống nhất.

---

## 14. Kiểm tra Liquibase đã chạy chưa

Sau khi ứng dụng khởi động, chạy:

```sql
SELECT
    ID,
    AUTHOR,
    FILENAME,
    DATEEXECUTED,
    ORDEREXECUTED,
    EXECTYPE
FROM DATABASECHANGELOG
ORDER BY ORDEREXECUTED;
```

Nếu changeset thành công, `EXECTYPE` thường là `EXECUTED`.

Kiểm tra lock:

```sql
SELECT * FROM DATABASECHANGELOGLOCK;
```

Không tự xóa lock khi chưa chắc không còn tiến trình Liquibase nào đang chạy.

---

## 15. Rollback

Rollback hoàn tác một hoặc nhiều changeset.

Ví dụ tư duy:

```text
Changeset: thêm cột revoked_at
Rollback : xóa cột revoked_at
```

Không phải thay đổi nào cũng rollback an toàn. Xóa cột hoặc xóa bảng có thể làm mất dữ
liệu. Luôn backup trước khi rollback trên database quan trọng.

Rollback không thay thế backup database.

---

## 16. Liquibase và test

Có hai chiến lược test phổ biến.

### Test nhanh với H2

- Khởi động nhanh.
- Không cần MySQL thật.
- Có thể khác MySQL ở một số kiểu dữ liệu và cú pháp.

### Integration test với MySQL test container/database riêng

- Gần môi trường thật hơn.
- Kiểm tra chính xác migration MySQL.
- Chạy chậm và cấu hình phức tạp hơn.

Khi mới học, có thể kiểm tra changelog trên database MySQL mới, rồi giữ unit test hiện tại
chạy nhanh với H2.

---

## 17. Quy tắc đặt tên changeset

Nên dùng tên mô tả được mục đích:

```text
001-create-roles-table
002-create-users-table
007-add-revoked-at-to-refresh-tokens
008-add-index-to-products-sku-code
009-increase-user-email-length
```

Không nên dùng tên mơ hồ:

```text
update-table
fix-db
change-1
new-change
```

---

## 18. Những lỗi thường gặp

### Table already exists

Nguyên nhân: chạy changeset tạo bảng trên database đã có schema.

Giải pháp: dùng database mới hoặc baseline đúng cách.

### Validation Failed: checksum changed

Nguyên nhân: chỉnh sửa changeset đã chạy.

Giải pháp: khôi phục file cũ và tạo changeset mới.

### Foreign key creation failed

Nguyên nhân thường gặp:

- Bảng được tham chiếu chưa tồn tại.
- Kiểu dữ liệu hai cột không giống nhau.
- Dữ liệu hiện tại vi phạm foreign key.

### Waiting for changelog lock

Nguyên nhân: một tiến trình Liquibase khác đang chạy hoặc lần chạy trước dừng bất thường.

Không xóa lock tùy tiện trên môi trường có nhiều instance.

### Entity không khớp schema

Nguyên nhân: changelog và annotation Entity dùng tên cột hoặc kiểu dữ liệu khác nhau.

Có thể dùng:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

để phát hiện lỗi khi khởi động.

---

## 19. Những điều không nên làm

- Không sửa changeset đã chạy trên database được chia sẻ.
- Không dùng `ddl-auto=update` cùng Liquibase trong môi trường thật.
- Không xóa bảng `DATABASECHANGELOG` để chạy lại từ đầu.
- Không chạy migration trên database quan trọng khi chưa backup.
- Không đưa password database vào changelog.
- Không gộp quá nhiều thay đổi không liên quan vào một changeset.
- Không dùng `changelog-sync` khi chưa hiểu nó chỉ đánh dấu, không chạy SQL.

---

## 20. Checklist thực hành

### Thiết lập

- [ ] Thêm dependency `liquibase-core`.
- [ ] Tạo `db.changelog-master.yaml`.
- [ ] Tạo thư mục `changes`.
- [ ] Cấu hình `spring.liquibase.change-log`.
- [ ] Giữ `ddl-auto=none` hoặc chuyển sang `validate` sau khi ổn định.

### Baseline schema

- [ ] Tạo changeset cho Role.
- [ ] Tạo changeset cho Category.
- [ ] Tạo changeset cho Supplier.
- [ ] Tạo changeset cho User.
- [ ] Tạo changeset cho Product.
- [ ] Tạo changeset cho RefreshToken.
- [ ] Thêm đầy đủ unique constraint và foreign key.

### Kiểm thử

- [ ] Tạo một database MySQL mới, trống.
- [ ] Khởi động ứng dụng để Liquibase chạy.
- [ ] Kiểm tra tất cả bảng được tạo.
- [ ] Kiểm tra `DATABASECHANGELOG`.
- [ ] Chạy ứng dụng lần hai và xác nhận changeset không chạy lại.
- [ ] Thêm một changeset mới và xác nhận chỉ changeset mới được chạy.

---

## 21. Thứ tự áp dụng vào SuperMarketAPI

```text
1. Hoàn thiện Refresh Token và test.
2. Chụp lại schema MySQL hiện tại.
3. Thêm dependency Liquibase.
4. Viết master changelog.
5. Viết changeset theo đúng thứ tự khóa ngoại.
6. Tạo database supermarket_liquibase_test mới.
7. Cho Liquibase dựng schema từ đầu.
8. So sánh schema mới với schema hiện tại.
9. Sửa changelog nếu có sai khác.
10. Chỉ sau khi kiểm tra mới baseline database đang có dữ liệu.
11. Từ đó mọi thay đổi schema phải có changeset mới.
```

Nên hoàn thiện Refresh Token trước khi chốt baseline, vì bảng `refresh_tokens` vừa được
thay đổi và cần ổn định tên cột trước khi ghi thành migration chính thức.

