/* =========================================================
   Cấu hình dùng chung cho 3 trang xác thực — chỉ sửa ở file này.
   Nạp TRƯỚC js/auth.js trong login.html, register.html, forgot-password.html.
   ========================================================= */
window.AUTH_CONFIG = {
  // true  = KHÔNG gọi backend, dùng dữ liệu giả (làm độc lập khi BE1 chưa xong).
  // false = gọi API thật tại baseUrl. Nhớ đặt false trước khi tạo PR / demo với BE1.
  // Ngoài localhost / 127.0.0.1, mock tự bị tắt (xem auth.js).
  mock: true,

  baseUrl: 'http://localhost:8080/api/auth',

  // Google OAuth Client ID (loại "Web application") — đây là giá trị CÔNG KHAI.
  // Tuyệt đối không đặt Client Secret ở Frontend.
  googleClientId: '',

  productsUrl: 'products.html',
};
