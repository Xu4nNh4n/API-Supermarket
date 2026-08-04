# JWT Authentication Flow Map

Tai lieu nay giai thich chi tiet phan login + JWT trong project `SuperMarketAPI`.
Muc tieu la doc tu tren xuong va biet sau khi mot doan code chay xong, request se di toi file nao, method nao tiep theo.

## 1. Buc tranh tong quan

Co 2 luong chinh:

1. Login de lay token:

```text
Client
  -> POST /api/auth/login
  -> AuthController.login()
  -> AuthServiceImpl.login()
  -> UserRepository.findByUserName()
  -> PasswordEncoder.matches()
  -> JwtService.generateToken()
  -> tra LoginResponse co token
```

2. Goi API khac bang token:

```text
Client
  -> GET/POST api khac voi header Authorization: Bearer <token>
  -> Spring Security filter chain
  -> JwtAuthenticationFilter.doFilterInternal()
  -> JwtService.isTokenValid()
  -> JwtService.extractUsername()
  -> UserRepository.findByUserName()
  -> SecurityContextHolder.setAuthentication()
  -> Controller cua API duoc goi
```

## 2. Login Flow: Tu request den token

### 2.1. Client goi API login

Request mau:

```http
POST /api/auth/login
Content-Type: application/json
```

Body:

```json
{
  "username": "admin",
  "password": "123456"
}
```

Request nay di vao file:

```text
src/main/java/com/api/supermarket/controller/AuthController.java
```

### 2.2. AuthController nhan request

Code lien quan:

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request){
        return authService.login(request);
    }
}
```

Giai thich:

- `@RestController`: bao voi Spring day la class controller tra du lieu JSON.
- `@RequestMapping("/api/auth")`: tat ca endpoint trong class nay bat dau bang `/api/auth`.
- `@PostMapping("/login")`: method nay xu ly `POST /api/auth/login`.
- `@RequestBody LoginRequest request`: Spring tu dong doc JSON body va map vao object `LoginRequest`.
- `return authService.login(request)`: controller khong tu xu ly login, ma chuyen logic sang service.

Sau do request di tiep toi:

```text
src/main/java/com/api/supermarket/service/Impl/AuthServiceImpl.java
```

Method:

```java
login(LoginRequest request)
```

## 3. AuthServiceImpl: Kiem tra dang nhap

File:

```text
src/main/java/com/api/supermarket/service/Impl/AuthServiceImpl.java
```

### 3.1. Cac dependency duoc inject vao service

Code:

```java
private final UserRepository userRepository;
private final JwtService jwtService;
private final PasswordEncoder passwordEncoder;

public AuthServiceImpl(
    UserRepository userRepository,
    JwtService jwtService,
    PasswordEncoder passwordEncoder){
    
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
}
```

Giai thich:

- `UserRepository`: dung de tim user trong database.
- `JwtService`: dung de tao token sau khi login thanh cong.
- `PasswordEncoder`: dung de so sanh password nguoi dung nhap voi BCrypt hash trong database.
- Constructor injection: Spring tu tao object va truyen cac bean can thiet vao service.

`PasswordEncoder` den tu file:

```text
src/main/java/com/api/supermarket/config/SecurityConfig.java
```

Bean:

```java
@Bean
public PasswordEncoder passwordEncoder(){
    return new BCryptPasswordEncoder();
}
```

### 3.2. Tim user theo username

Code:

```java
User user = userRepository.findByUserName(request.getUsername())
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Ten dang nhap hoac mat khau khong dung"
        ));
```

Giai thich:

- `request.getUsername()` lay username client gui len.
- `userRepository.findByUserName(...)` query database bang username.
- Repository tra ve `Optional<User>`.
- Neu khong tim thay user, nem `ResponseStatusException(HttpStatus.UNAUTHORIZED, ...)`.
- `UNAUTHORIZED` la HTTP `401`, dung cho login that bai.
- Thong bao nen de chung chung: khong noi ro username sai hay password sai.

Sau dong nay co 2 truong hop:

- Khong tim thay user: request dung tai day va tra `401`.
- Tim thay user: code chay tiep sang check active.

### 3.3. Kiem tra tai khoan co bi khoa khong

Code:

```java
if(!Boolean.TRUE.equals(user.getIsActive())){
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tai khoan da bi khoa");
}
```

Giai thich:

- `user.getIsActive()` lay trang thai tai khoan.
- `Boolean.TRUE.equals(...)` an toan hon `user.getIsActive() == true` vi tranh loi `NullPointerException`.
- Neu user bi khoa, login dung tai day va tra `401`.
- Neu user active, code chay tiep sang check password.

### 3.4. Kiem tra password bang BCrypt

Code:

```java
if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())){
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ten dang nhap hoac mat khau khong dung");
}
```

Giai thich:

- `request.getPassword()` la password dang plain text nguoi dung nhap, vi du `123456`.
- `user.getPasswordHash()` la hash trong database, vi du `$2y$10$...`.
- Khong duoc dung `.equals()` de so sanh password voi hash.
- `passwordEncoder.matches(rawPassword, encodedPassword)` se:
  - lay raw password client gui len,
  - hash theo BCrypt metadata trong chuoi hash,
  - so sanh ket qua voi hash trong DB.
- Neu password sai, tra `401`.
- Neu dung, code chay tiep sang tao JWT token.

### 3.5. Tao token sau khi login thanh cong

Code:

```java
String token = jwtService.generateToken(user);
```

Dong nay chuyen sang file:

```text
src/main/java/com/api/supermarket/security/JwtService.java
```

Method:

```java
generateToken(User user)
```

Sau khi `JwtService.generateToken(...)` tra ve token, code quay lai `AuthServiceImpl`.

### 3.6. Tra response ve client

Code:

```java
return new LoginResponse(
    user.getUserId(),
    user.getUserName(),
    user.getRole().getRoleName(),
    "Dang nhap thanh cong",
    token
);
```

Giai thich:

- `user.getUserId()`: id user.
- `user.getUserName()`: username.
- `user.getRole().getRoleName()`: ten role, vi du `ADMIN`.
- `"Dang nhap thanh cong"`: message.
- `token`: JWT client se luu lai de goi API sau.

Response mau:

```json
{
  "userId": 1,
  "username": "admin",
  "role": "ADMIN",
  "message": "Dang nhap thanh cong",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

## 4. JwtService: Tao va doc token

File:

```text
src/main/java/com/api/supermarket/security/JwtService.java
```

Class nay co 2 nhiem vu:

- Tao token khi login thanh cong.
- Doc va validate token khi client goi API can dang nhap.

### 4.1. Lay cau hinh JWT tu application.properties

Code:

```java
@Value("${jwt.secret}")
private String secret;

@Value("${jwt.expiration}")
private long expiration;
```

Giai thich:

- `jwt.secret`: chuoi bi mat dung de ky token.
- `jwt.expiration`: thoi gian song cua token, tinh bang millisecond.
- Vi du `86400000` la 24 gio.

File cau hinh:

```text
src/main/resources/application.properties
```

Vi du:

```properties
jwt.secret=super-market-api-secret-key-super-long-please-change-later
jwt.expiration=86400000
```

Project hoc/test de secret trong properties duoc. Project that nen dua vao bien moi truong.

### 4.2. generateToken(User user)

Code:

```java
public String generateToken(User user){
    Date now = new Date();

    Date expiryDate = new Date(now.getTime() + expiration);

    return Jwts.builder()
    .subject(user.getUserName())
    .claim("userId", user.getUserId())
    .claim("role", user.getRole().getRoleName())
    .issuedAt(now)
    .expiration(expiryDate)
    .signWith(getSigningKey())
    .compact();
}
```

Giai thich tung doan:

- `Date now = new Date();`
  - lay thoi diem hien tai.

- `Date expiryDate = new Date(now.getTime() + expiration);`
  - tinh thoi diem token het han.
  - Neu `expiration = 86400000`, token het han sau 24 gio.

- `Jwts.builder()`
  - bat dau tao JWT.

- `.subject(user.getUserName())`
  - set `sub` cua token.
  - `subject` nen la danh tinh chinh cua user.
  - O day minh dung username.

- `.claim("userId", user.getUserId())`
  - them thong tin phu `userId` vao token.

- `.claim("role", user.getRole().getRoleName())`
  - them role vao token.
  - Sau nay co the dung role nay de phan quyen.

- `.issuedAt(now)`
  - set thoi diem tao token.

- `.expiration(expiryDate)`
  - set thoi diem het han.

- `.signWith(getSigningKey())`
  - ky token bang secret key.
  - Neu ai sua noi dung token, chu ky se sai.

- `.compact()`
  - bien JWT thanh chuoi string tra ve cho client.

Sau khi method nay xong, token quay lai:

```text
AuthServiceImpl.login()
```

Va duoc dat vao `LoginResponse`.

### 4.3. getSigningKey()

Code:

```java
private SecretKey getSigningKey(){
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
}
```

Giai thich:

- JWT can mot key de ky va verify token.
- `secret.getBytes(StandardCharsets.UTF_8)` bien chuoi secret thanh byte array.
- `Keys.hmacShaKeyFor(...)` tao `SecretKey` dung cho thuat toan HMAC.
- Cung mot secret phai duoc dung o 2 noi:
  - ky token khi generate,
  - verify token khi parse.

### 4.4. extractUsername(String token)

Code:

```java
public String extractUsername(String token){
    return extractAllClaims(token).getSubject();
}
```

Giai thich:

- Token co `subject` la username.
- Method nay parse token, lay claims, roi lay `subject`.
- Method nay duoc goi trong:

```text
JwtAuthenticationFilter.doFilterInternal()
```

Sau khi lay username, filter se query DB de lay user.

### 4.5. isTokenValid(String token)

Code:

```java
public boolean isTokenValid(String token){
    try{
        extractAllClaims(token);
        return true;
    }
    catch (Exception e){
        return false;
    }
}
```

Giai thich:

- Method nay kiem tra token co parse duoc khong.
- `extractAllClaims(token)` se verify chu ky va han su dung.
- Neu token:
  - sai format,
  - sai chu ky,
  - het han,
  - bi sua noi dung,
  thi thu vien `jjwt` se throw exception.
- Neu khong co exception, token hop le.

### 4.6. extractAllClaims(String token)

Code:

```java
private Claims extractAllClaims(String token){
    return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
}
```

Giai thich:

- `Jwts.parser()` tao parser de doc JWT.
- `.verifyWith(getSigningKey())` noi cho parser dung key nao de verify chu ky.
- `.build()` tao parser hoan chinh.
- `.parseSignedClaims(token)` parse token da ky.
- `.getPayload()` lay phan payload, tuc la claims nhu:
  - subject,
  - userId,
  - role,
  - issuedAt,
  - expiration.

Method nay la loi cua validate token.

## 5. SecurityConfig: Cau hinh ai duoc vao API nao

File:

```text
src/main/java/com/api/supermarket/config/SecurityConfig.java
```

### 5.1. Inject JwtAuthenticationFilter

Code:

```java
private final JwtAuthenticationFilter jwtAuthenticationFilter;

public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
}
```

Giai thich:

- `JwtAuthenticationFilter` la filter minh tu tao.
- Spring tao filter nay vi class co `@Component`.
- Spring truyen filter vao `SecurityConfig` qua constructor.
- Sau do `SecurityConfig` gan filter nay vao Spring Security filter chain.

### 5.2. Tao PasswordEncoder bean

Code:

```java
@Bean
public PasswordEncoder passwordEncoder(){
    return new BCryptPasswordEncoder();
}
```

Giai thich:

- Day la bean dung chung trong app.
- `AuthServiceImpl` inject bean nay de check password.
- Lam vay tot hon `new BCryptPasswordEncoder()` trong service vi:
  - de test hon,
  - de doi cau hinh hon,
  - dung theo cach Spring quan ly dependency.

### 5.3. securityFilterChain(HttpSecurity http)

Code:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
    return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/login").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
}
```

Giai thich tung doan:

- `.csrf(csrf -> csrf.disable())`
  - tat CSRF.
  - REST API dung JWT stateless thuong tat CSRF vi khong dung form session/cookie mac dinh.

- `.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`
  - noi voi Spring Security: khong tao session dang nhap tren server.
  - Moi request phai tu gui token.
  - Server khong nho user da login bang session.

- `.authorizeHttpRequests(...)`
  - cau hinh endpoint nao duoc vao, endpoint nao can dang nhap.

- `.requestMatchers("/api/auth/login").permitAll()`
  - cho phep goi login ma khong can token.
  - Neu login cung bat token thi user se khong bao gio dang nhap duoc.

- `.anyRequest().authenticated()`
  - tat ca request con lai phai authenticated.
  - Authenticated nghia la trong `SecurityContextHolder` da co authentication hop le.
  - Authentication nay se do `JwtAuthenticationFilter` set neu token hop le.

- `.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`
  - chen filter JWT cua minh vao truoc filter username/password mac dinh cua Spring.
  - Muc tieu: doc token som, set authentication som.

- `.build()`
  - tao ra `SecurityFilterChain` chinh thuc cho Spring Security dung.

## 6. JwtAuthenticationFilter: Xu ly request co token

File:

```text
src/main/java/com/api/supermarket/security/JwtAuthenticationFilter.java
```

Filter nay chay tren moi request di qua Spring Security.

### 6.1. extends OncePerRequestFilter

Code:

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter
```

Giai thich:

- `OncePerRequestFilter` dam bao filter chi chay mot lan cho moi request.
- Neu khong extends class nay, method `doFilterInternal` se khong override dung va filter khong hoat dong dung.

### 6.2. Inject JwtService va UserRepository

Code:

```java
private final JwtService jwtService;
private final UserRepository userRepository;

public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
    this.jwtService = jwtService;
    this.userRepository = userRepository;
}
```

Giai thich:

- `JwtService`: dung de validate token va lay username.
- `UserRepository`: dung de lay user tu database theo username trong token.

### 6.3. Doc header Authorization

Code:

```java
String authHeader = request.getHeader("Authorization");
```

Giai thich:

- Client phai gui token trong header:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

- `Authorization` la ten header.
- `Bearer` la convention noi rang phan sau la access token.

### 6.4. Neu khong co token thi bo qua filter

Code:

```java
if(authHeader == null || !authHeader.startsWith("Bearer ")){
    filterChain.doFilter(request, response);
    return;
}
```

Giai thich:

- Neu khong co header Authorization, filter khong lam gi.
- Neu header khong bat dau bang `"Bearer "`, filter cung khong lam gi.
- `filterChain.doFilter(request, response)` nghia la cho request di tiep sang filter/handler tiep theo.
- Sau do `return` de dung filter hien tai.

Ket qua sau do:

- Neu endpoint la `/api/auth/login`: duoc vao vi `SecurityConfig` permitAll.
- Neu endpoint khac: se bi chan vi `.anyRequest().authenticated()` ma chua co authentication.

### 6.5. Cat token ra khoi header

Code:

```java
String token = authHeader.substring(7);
```

Giai thich:

- Chuoi `"Bearer "` co 7 ky tu.
- `substring(7)` bo phan `"Bearer "` va giu lai JWT that.
- Vi du:

```text
Bearer abc.def.ghi
```

Sau substring:

```text
abc.def.ghi
```

### 6.6. Validate token

Code:

```java
if(!jwtService.isTokenValid(token)){
    filterChain.doFilter(request, response);
    return;
}
```

Giai thich:

- Goi `JwtService.isTokenValid(token)`.
- Neu token sai, het han, bi sua, filter khong set authentication.
- Request van di tiep, nhung vi chua authenticated nen API can login se bi Spring Security chan.

Sau dong nay, neu token hop le, code chay tiep sang lay username.

### 6.7. Lay username tu token

Code:

```java
String username = jwtService.extractUsername(token);
```

Giai thich:

- Token hop le thi co the doc payload.
- `extractUsername` lay `subject`.
- Subject duoc set trong `JwtService.generateToken()`:

```java
.subject(user.getUserName())
```

Sau do filter co username de tim user trong DB.

### 6.8. Tim user trong database

Code:

```java
User user = userRepository.findByUserName(username).orElse(null);
```

Giai thich:

- Token noi user la ai, nhung filter van query DB lai.
- Viec nay giup dam bao user van con ton tai.
- Sau nay ban co the check them:

```java
Boolean.TRUE.equals(user.getIsActive())
```

de user bi khoa thi token cu cung khong dung duoc.

### 6.9. Tao Authentication va set vao SecurityContext

Code:

```java
if(user != null && SecurityContextHolder.getContext().getAuthentication() == null){
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            user.getUserName(),
            null,
            Collections.emptyList()
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
}
```

Giai thich:

- `SecurityContextHolder` la noi Spring Security luu thong tin request hien tai da dang nhap hay chua.
- Neu chua co authentication, minh tao moi.
- `UsernamePasswordAuthenticationToken(...)` gom:
  - principal: `user.getUserName()`, tuc user dang dang nhap.
  - credentials: `null`, vi request nay khong dang login bang password nua.
  - authorities: `Collections.emptyList()`, hien tai chua phan quyen theo role.
- `setAuthentication(authentication)` noi voi Spring Security:
  - request nay da authenticated,
  - cho phep vao cac endpoint `.authenticated()`.

Sau dong nay, request tiep tuc di vao controller cua API ma client dang goi.

### 6.10. Cho request di tiep

Code:

```java
filterChain.doFilter(request, response);
```

Giai thich:

- Filter xong viec cua minh.
- Request duoc chuyen sang filter tiep theo hoac controller.
- Neu da set authentication, request se qua duoc API can dang nhap.

## 7. Map thu tu chay thuc te

### 7.1. Khi login

```text
Client POST /api/auth/login
  -> SecurityConfig cho phep /api/auth/login permitAll
  -> JwtAuthenticationFilter thay khong co Bearer token thi bo qua
  -> AuthController.login()
  -> AuthServiceImpl.login()
  -> UserRepository.findByUserName()
  -> PasswordEncoder.matches()
  -> JwtService.generateToken()
  -> LoginResponse tra ve token
```

### 7.2. Khi goi API khac khong co token

```text
Client GET /api/products
  -> JwtAuthenticationFilter khong thay Authorization Bearer
  -> khong set Authentication
  -> SecurityConfig yeu cau anyRequest().authenticated()
  -> bi chan 401/403
```

### 7.3. Khi goi API khac co token dung

```text
Client GET /api/products
Header Authorization: Bearer <token>
  -> JwtAuthenticationFilter doc header
  -> cat token
  -> JwtService.isTokenValid(token)
  -> JwtService.extractUsername(token)
  -> UserRepository.findByUserName(username)
  -> tao UsernamePasswordAuthenticationToken
  -> SecurityContextHolder.setAuthentication(authentication)
  -> SecurityConfig thay request da authenticated
  -> cho vao ProductController hoac controller tuong ung
```

### 7.4. Khi goi API khac co token sai

```text
Client GET /api/products
Header Authorization: Bearer <token-sai>
  -> JwtAuthenticationFilter doc header
  -> JwtService.isTokenValid(token) tra false
  -> khong set Authentication
  -> SecurityConfig yeu cau authenticated
  -> bi chan 401/403
```

## 8. Cach test bang Postman/Swagger

### 8.1. Test login

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

Body:

```json
{
  "username": "admin",
  "password": "123456"
}
```

Ket qua mong doi:

```json
{
  "userId": 1,
  "username": "admin",
  "role": "ADMIN",
  "message": "Dang nhap thanh cong",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### 8.2. Test API khac khong token

Goi mot endpoint bat ky khac `/api/auth/login`.

Ket qua mong doi:

```text
401 Unauthorized hoac 403 Forbidden
```

### 8.3. Test API khac co token

Them header:

```http
Authorization: Bearer <token lay tu login response>
```

Ket qua mong doi:

```text
Request duoc vao controller
```

## 9. Nhung diem co the nang cap tiep

### 9.1. Gan role vao authorities

Hien tai filter dang de:

```java
Collections.emptyList()
```

Nghia la user da authenticated, nhung chua co quyen `ROLE_ADMIN`, `ROLE_MANAGER`.

Sau nay co the doi thanh:

```java
List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName()))
```

Luc do co the dung:

```java
@PreAuthorize("hasRole('ADMIN')")
```

hoac cau hinh endpoint theo role trong `SecurityConfig`.

### 9.2. Check isActive trong JwtAuthenticationFilter

Hien tai login da check `isActive`.
Nhung neu user dang login, da co token, sau do admin khoa user, token cu van co the con dung den khi het han.

Co the them check trong filter:

```java
if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
    filterChain.doFilter(request, response);
    return;
}
```

### 9.3. Dua jwt.secret vao bien moi truong

Hien tai:

```properties
jwt.secret=super-market-api-secret-key-super-long-please-change-later
```

Project that nen doi thanh:

```properties
jwt.secret=${JWT_SECRET}
```

Va set bien moi truong `JWT_SECRET` tren server.

### 9.4. Them refresh token

Access token nen song ngan hon, vi du 15 phut.
Refresh token dung de cap access token moi.

Phan nay lam sau khi login/JWT co ban da on dinh.

## 10. Tom tat ngan gon

- `AuthController`: nhan request login.
- `AuthServiceImpl`: kiem tra username/password va tao token.
- `JwtService`: tao token, doc token, validate token.
- `JwtAuthenticationFilter`: doc token trong request va set authentication.
- `SecurityConfig`: quy dinh `/api/auth/login` duoc mo, API khac phai dang nhap.
- `SecurityContextHolder`: noi Spring Security nhin vao de biet request hien tai da authenticated hay chua.

