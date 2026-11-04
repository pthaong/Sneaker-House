/* Cấu hình trang Bảo mật tài khoản (FE1) — chỉ sửa ở file này.
   Không đặt mật khẩu, token hay Client Secret ở đây. */
window.ACCOUNT_CONFIG = {
  // Địa chỉ gốc của API Backend (không có dấu / ở cuối)
  apiBase: 'http://localhost:8080/api',

  // Trang đăng nhập (module fe-auth)
  loginUrl: '../fe-auth/login.html',

  // Liên kết sang các trang/module khác. Khai báo khóa nào thì ghi đè href mặc định trong HTML.
  // Chưa có trang thật thì để mặc định; khi ghép với module khác, sửa cho đúng đường dẫn.
  links: {
    // home: 'index.html', products: 'products.html', profile: 'profile.html', cart: 'cart.html',
    // orders: 'orders.html', wishlist: 'wishlist.html', addresses: 'addresses.html',
    // coupons: 'coupons.html', notifications: 'notifications.html', settings: 'settings.html',
    // men: 'products.html?category=nam', women: 'products.html?category=nu', kids: 'products.html?category=tre-em',
    // brands: 'brands.html', collections: 'collections.html', sale: 'sale.html', news: 'news.html', contact: 'contact.html',
  },

  // Các endpoint (ghép sau apiBase)
  endpoints: {
    csrf: '/auth/csrf',
    logout: '/auth/logout',
    profile: '/profile',                       // GET: email, loginMethod, googleLinked, fullName...
    forgotPassword: '/auth/forgot-password',   // POST { email } → gửi OTP
    verifyOtp: '/auth/verify-otp',             // POST { email, otp } → { resetToken }
    resendOtp: '/auth/resend-otp',             // POST { email }
    resetPassword: '/auth/reset-password',     // POST { token, newPassword }

    // BE1 chưa bàn giao (issue #11) — đây là ĐỀ XUẤT contract, sửa khi BE1 chốt:
    changePassword: '/account/password',       // POST { currentPassword, newPassword }
    sessions: '/account/sessions',             // GET → [{ id, device, browser, os, createdAt, current }]
    revokeSession: '/account/sessions/{id}',   // DELETE
  },
  changePasswordMethod: 'POST',                // BE1 có thể chọn PUT/PATCH

  /* Dữ liệu giả cho phần BE1 chưa làm xong. CHỈ chạy ở localhost / 127.0.0.1.
     Đặt cả hai thành false khi BE1 bàn giao API, TRƯỚC khi tạo PR và khi kiểm thử cuối. */
  mock: {
    changePassword: true,
    sessions: true,
  },
};
