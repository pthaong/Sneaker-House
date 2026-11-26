# FE1 — Authentication (Sneaker House)

Trang: `login.html`, `register.html`, `forgot-password.html` (+ `terms.html`, `privacy.html` là nội dung MẪU, cần thay trước khi vận hành).
JS/CSS thuần, không thêm thư viện. Chép cả thư mục vào repo (nhánh `feature/fe-auth`), giữ nguyên `css/`, `js/`, `assets/`.

## Chạy thử (PowerShell)
    cd fe-auth
    python -m http.server 5500
    # mở http://localhost:5500/login.html
Nên chạy qua server (hoặc Live Server của VS Code); mở trực tiếp file:// thì không gọi được API và Google không hoạt động.

## Cấu hình: `js/auth-config.js` (một chỗ duy nhất)
| Khóa | Ý nghĩa |
|---|---|
| `mock` | `true` = dữ liệu giả, KHÔNG gọi backend (làm độc lập). `false` = gọi API thật. **Đặt `false` trước khi tạo PR / tích hợp với BE1.** Mock chỉ chạy ở `localhost` / `127.0.0.1`; ở tên miền khác nó tự tắt. |
| `baseUrl` | Địa chỉ API, mặc định `http://localhost:8080/api/auth` |
| `googleClientId` | Google OAuth Client ID (công khai). **Không bao giờ để Client Secret ở Frontend.** |
| `productsUrl` | Trang danh sách sản phẩm cho nút "Xem sản phẩm" trên carousel |

## Dữ liệu mock (khi `mock: true`)
- Đăng nhập thành công: `demo@sneakerhouse.vn` / `Demo@1234`; sai thông tin => 401.
- Đăng ký: `taken@sneakerhouse.vn` => 409 (email đã tồn tại).
- Quên mật khẩu: `fail@sneakerhouse.vn` => 500.
- Google: chưa có Client ID thì hiện nút giả "(mô phỏng)". Thêm vào URL để thử lỗi: `?mockGoogle=conflict` (email trùng tài khoản mật khẩu), `?mockGoogle=invalid` (token không hợp lệ), `?mockGoogle=network` (mất kết nối).

## Đăng nhập / đăng ký bằng Google
Dùng Google Identity Services (`https://accounts.google.com/gsi/client`, tải khi vào trang).
1. Người dùng chọn tài khoản trên nút Google => GIS trả về `credential` (Google ID Token, dạng JWT).
2. FE gửi `POST {baseUrl}/google` với body `{ "idToken": "<credential>" }`, `credentials: 'include'` (BE đặt cookie phiên HttpOnly).
3. FE KHÔNG tự xác minh token. BE xác minh chữ ký, issuer, audience, hạn dùng.
Mã trạng thái FE đang xử lý: 200 thành công; 400/401 token không hợp lệ; 403 bị từ chối; 409 email trùng tài khoản đăng ký bằng mật khẩu (hiện thông điệp `message` của BE); lỗi mạng và lỗi khác có thông báo riêng.
Đổi tên trường (`idToken` => `credential`...) ở hàm `buildPayload()`.

Cấu hình Google Cloud Console (OAuth client loại *Web application*) — **Authorized JavaScript origins** phải khớp CHÍNH XÁC origin đang chạy, ví dụ:
`http://localhost:5500` và `http://127.0.0.1:5500` (Live Server hay dùng 127.0.0.1 — hai địa chỉ này được coi là khác nhau).

## Nối backend (BE1)
Endpoint mặc định: POST `/login`, `/register`, `/forgot-password`, `/google` (JSON).
Body: login `{identifier, password, rememberMe}` · register `{fullName, email, phone, password}` · forgot `{email}` · google `{idToken}`.
`phone` luôn gửi dạng `0xxxxxxxxx` (người dùng nhập `+84…` hoặc `84…` thì FE đổi về `0…`). Nếu BE1 muốn dạng `+84…`, sửa `toLocalPhone()` trong `js/auth.js`.
Lỗi trả về nên có dạng `{ message, errors?: { tenTruong: "..." } }`.
Token/phiên: tự xử lý bằng `window.AUTH_HOOKS = { onLoginSuccess(data, info){...} }` (`info.method === 'google'` khi đăng nhập bằng Google). Không có hook thì FE chuyển tới `index.html`.
Checkbox đồng ý điều khoản chỉ kiểm tra ở FE; nếu BE cần lưu sự đồng ý thì thêm trường trong `buildPayload()`.

## Ảnh carousel
Mỗi slide cần 2 file WebP trong `assets/`: `<key>-720.webp` và `<key>-1122.webp` (tỉ lệ 4:5, không có chữ trong ảnh).
Thêm/đổi slide: sửa mảng `SLIDES` trong `js/auth.js` (`key`, `brand`, `name`, `desc`, `alt`).
Tên và mô tả hiện viết theo ảnh, hãy sửa cho khớp sản phẩm thật.

## Logo
`assets/logo-light.png` (chữ trắng, dùng trên nền tối) và `assets/logo-dark.png` (bản chữ đen tạo từ logo gốc, dùng trên nền trắng).
