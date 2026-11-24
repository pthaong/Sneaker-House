
/*
 * Sneaker House - Homepage Configuration
 * Module: Frontend Homepage
 * Phụ trách: FE1
 *
 * Chỉ lưu cấu hình giao diện và đường dẫn API tại đây.
 * Không lưu mật khẩu, access token hoặc API secret.
 *
 * Homepage dùng chung Backend với các module khác.
 * Admin sẽ sử dụng các API quản trị có phân quyền riêng.
 */

window.HOME_CONFIG = {

  // =====================================================
  // 1. BACKEND API
  // =====================================================

  // Môi trường phát triển tại máy local.
  // Không có dấu "/" ở cuối.
  apiBase: 'http://localhost:8080/api',

  // false = sử dụng Backend API thật.
  // true = sử dụng dữ liệu Mock khi phát triển tại localhost.
  // Không bật Mock khi kiểm thử tích hợp hoặc Merge PR.
  mock: false,

  // =====================================================
  // 2. AUTHENTICATION
  // =====================================================

  // Tái sử dụng trang đăng nhập đã hoàn thành.
  loginUrl: '../fe-auth/login.html',

  // =====================================================
  // 3. PAGE NAVIGATION
  // =====================================================

  // Chỉ ghi đè khi trang tương ứng đã được triển khai.
  // Không tự tạo hoặc thay đổi cấu trúc menu đã duyệt.
  links: {
    // products: 'products.html',
    // cart: 'cart.html',
    // wishlist: 'wishlist.html',
    // profile: '../fe-account/profile.html',
    // security: '../fe-account/account-security.html',
    // orders: 'orders.html',
    // brands: 'brands.html',
    // collections: 'collections.html',
    // sale: 'sale.html',
    // news: 'news.html',
    // contact: 'contact.html'
  },

  // Đường dẫn trang chi tiết sản phẩm.
  // {id} và {slug} được thay bằng dữ liệu sản phẩm.
  // Cần trỏ đến trang thật khi module chi tiết hoàn thành.
  productUrl: 'product.html?id={id}',

  // =====================================================
  // 4. BACKEND API ENDPOINTS
  // =====================================================

  // Endpoint được ghép với apiBase.
  // Ví dụ: http://localhost:8080/api/products/featured
  endpoints: {

    // -------------------------------
    // Authentication / Account
    // -------------------------------
    // Giữ nguyên hợp đồng API hiện có.
    csrf: '/auth/csrf',
    logout: '/auth/logout',
    profile: '/profile',

    // -------------------------------
    // Homepage - Categories / Brands
    // -------------------------------

    // GET /api/categories
    // Backend trả danh mục với id, name, slug, parent...
    categories: '/categories',

    // GET /api/brands
    // Backend trả thương hiệu với id, name, logoUrl...
    brands: '/brands',

    // -------------------------------
    // Homepage - Banners
    // -------------------------------

    // GET /api/banners
    // Mỗi banner có:
    // id, title, imageUrl, targetUrl,
    // bannerType ('MAIN' hoặc 'AD'), position, active.
    banners: '/banners',

    // -------------------------------
    // Homepage - Flash Sale
    // -------------------------------

    // GET /api/flash-sales
    // Trả mảng ProductResponse[].
    // Mỗi sản phẩm có flashSaleEnd riêng.
    flashSale: '/flash-sales',

    // -------------------------------
    // Homepage - Product Sections
    // -------------------------------

    // GET /api/products/featured
    featured: '/products/featured',

    // GET /api/products/new-arrivals
    newest: '/products/new-arrivals',

    // GET /api/products/best-sellers
    bestSellers: '/products/best-sellers',

    // -------------------------------
    // Homepage - Collections / News
    // -------------------------------

    // GET /api/collections
    // Dữ liệu có title, description, imageUrl, targetUrl.
    collections: '/collections',

    // GET /api/news
    // Dữ liệu có title, summary, imageUrl, publishedAt...
    news: '/news',

    // -------------------------------
    // Product Search / Filters
    // -------------------------------

    // GET /api/products
    //
    // Các tham số Backend hỗ trợ:
    // keyword
    // categoryId
    // brandId
    // minPrice
    // maxPrice
    // size
    // color
    // sort
    // page (bắt đầu từ 0)
    // sizePerPage
    //
    // Backend trả Page Response:
    // content, page, size,
    // totalElements, totalPages, last.
    products: '/products',

    // -------------------------------
    // Future / Other Modules
    // -------------------------------

    // Các API bên dưới chưa được xác nhận trong PR #18.
    // Giữ cấu hình để phát triển module tương ứng sau này.
    // Frontend cần hiển thị trạng thái chưa hỗ trợ nếu API thiếu.

    reviews: '/reviews/featured',

    newsletter: '/newsletter/subscribe',

    cart: '/cart/items',

    wishlist: '/wishlist/{id}',

    // Chatbox mới yêu cầu giao diện.
    // Chưa tích hợp OpenAI / Gemini.
    // Có thể cấu hình endpoint AI sau này.
    chat: null
  },

  // =====================================================
  // 5. PRODUCT FILTER OPTIONS
  // =====================================================

  // Các lựa chọn hiển thị trên giao diện.
  // Khi gửi yêu cầu đến Backend, phải sử dụng đúng
  // keyword, categoryId, brandId, sizePerPage...
  filterOptions: {

    // Size giày
    sizes: [
      '36', '37', '38', '39', '40',
      '41', '42', '43', '44', '45'
    ],

    // Màu sắc
    // name = giá trị gửi Backend.
    // value = mã màu hiển thị giao diện.
    colors: [
      { name: 'Đen', value: '#111111' },
      { name: 'Trắng', value: '#ffffff' },
      { name: 'Đỏ', value: '#e11d2a' },
      { name: 'Xanh dương', value: '#2563eb' },
      { name: 'Xám', value: '#9ca3af' },
      { name: 'Be', value: '#d6c3a3' }
    ],

    // Khoảng giá tính theo VND
    prices: [
      {
        label: 'Dưới 1 triệu',
        min: 0,
        max: 1000000
      },
      {
        label: '1 – 2 triệu',
        min: 1000000,
        max: 2000000
      },
      {
        label: '2 – 3 triệu',
        min: 2000000,
        max: 3000000
      },
      {
        label: 'Trên 3 triệu',
        min: 3000000,
        max: 0
      }
    ],

    // Số sản phẩm mỗi trang.
    // home.js phải ánh xạ thành sizePerPage.
    pageSize: 8
  },

  // =====================================================
  // 6. STORE INFORMATION
  // =====================================================

  // Thông tin tạm dùng cho giao diện.
  // Cần xác nhận trước khi công khai website.
  store: {
    hotline: '1900 1234',
    email: 'cskh@sneakerhouse.vn',
    address: 'Hà Nội, Việt Nam'
  }
};
