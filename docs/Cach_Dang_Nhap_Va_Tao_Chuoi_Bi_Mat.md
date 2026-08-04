$="mat-khau-mysql"
$env:JWT_SECRET="chuoi-bi-mat-jwt-phai-du-dai"
.\mvnw.cmd spring-boot:run

$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run

tạo chuỗi bí mật = powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)