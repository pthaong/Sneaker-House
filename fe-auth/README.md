# FE1 — Authentication (Sneaker House)

Trang: `login.html`, `register.html`, `forgot-password.html` (+ `terms.html`, `privacy.html` là nội dung MẪU, cần thay trước khi vận hành).
JS/CSS thuần, không thêm thư viện. Chép cả thư mục vào repo (nhánh `feature/fe-auth`), giữ nguyên `css/`, `js/`, `assets/`.

## Chạy thử (PowerShell)
    cd fe-auth
    python -m http.server 5500
    # mở http://localhost:5500/login.html
Nên chạy qua server (hoặc Live Server của VS Code); mở trực tiếp file:// thì không gọi được API.

## Nối backend (BE1)
Đặt trước thẻ `<script src="js/auth.js">`:
    <script>
      window.AUTH_CONFIG = { baseUrl: 'http://localhost:8080/api/auth', productsUrl: 'products.html' };
    </script>
Endpoint mặc định: POST /login, /register, /forgot-password (JSON). Sửa tên trường ở `buildPayload()`.
Token/phiên: tự xử lý bằng `window.AUTH_HOOKS = { onLoginSuccess(data){...} }`.
`productsUrl` là trang danh sách sản phẩm mà nút "Xem sản phẩm" trên carousel trỏ tới.
Checkbox đồng ý điều khoản hiện chỉ kiểm tra ở FE; nếu BE cần lưu sự đồng ý thì thêm trường trong `buildPayload()`.

## Ảnh carousel
Mỗi slide cần 2 file WebP trong `assets/`: `<key>-720.webp` và `<key>-1122.webp` (tỉ lệ 4:5, không có chữ trong ảnh).
Thêm/đổi slide: sửa mảng `SLIDES` trong `js/auth.js` (`key`, `brand`, `name`, `desc`, `alt`).
Tên và mô tả hiện viết theo ảnh, hãy sửa cho khớp sản phẩm thật.

## Logo
`assets/logo-light.png` (chữ trắng, dùng trên nền tối) và `assets/logo-dark.png` (bản chữ đen tạo từ logo gốc, dùng trên nền trắng).
