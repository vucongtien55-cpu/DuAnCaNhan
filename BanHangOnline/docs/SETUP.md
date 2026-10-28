# Setup local

## Bien moi truong bat buoc

Ung dung khong con luu mat khau database mac dinh trong source. Truoc khi chay, cau hinh:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/marketplace_db"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<mat-khau-postgres>"
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Co the xem danh sach bien trong `.env.example`.

## Chay local

Profile mac dinh dung `spring.jpa.hibernate.ddl-auto=validate` va tat SQL log de tranh dung cau hinh dev trong production.
Khi chay local voi database chua co schema migration, dung profile `dev`:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\gradlew.bat bootRun
```

Mo `http://localhost:8080/`.

## Kiem thu

Test dung H2 in-memory, khong can PostgreSQL local:

```powershell
.\gradlew.bat clean test
```

