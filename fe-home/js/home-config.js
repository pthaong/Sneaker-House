/* Cấu hình trang chủ (FE1, issue #15) — chỉ sửa ở file này.
   Không đặt mật khẩu, token hay secret ở đây. */
window.HOME_CONFIG = {
  // Gốc API Backend (không có dấu / ở cuối)
  apiBase: 'http://localhost:8080/api',

  // true = dùng dữ liệu giả cho các API BE1 CHƯA bàn giao. Chỉ chạy ở localhost.
  // Khi bật, góc dưới trái có nhãn "Dữ liệu giả". ĐẶT false trước khi tạo PR / kiểm thử cuối.
  mock: true,

  loginUrl: '../fe-auth/login.html',

  // Ghi đè đường dẫn các trang khác (khóa = giá trị data-link trong index.html)
  links: {
    // products: 'products.html', cart: 'cart.html', wishlist: 'wishlist.html', profile: '../fe-account/profile.html',
    // security: '../fe-account/account-security.html', orders: 'orders.html', brands: 'brands.html',
    // collections: 'collections.html', sale: 'sale.html', news: 'news.html', contact: 'contact.html'
  },

  // Trang chi tiết sản phẩm: {id} và {slug} được thay bằng giá trị thật
  productUrl: 'product.html?id={id}',

  // Endpoint (ghép sau apiBase). BE1 đổi tên thì chỉ sửa ở đây.
  endpoints: {
    csrf: '/auth/csrf',
    logout: '/auth/logout',
    profile: '/profile',
    categories: '/categories',          // GET → [{ id, name, slug, children:[{id,name,slug}], types:[...] }]
    brands: '/brands',                  // GET → [{ id, name, slug, logoUrl }]
    banners: '/home/banners',           // GET → [{ id, title, subtitle, ctaText, link, imageUrl, position: 'HERO'|'PROMO' }]
    flashSale: '/flash-sale/current',   // GET → { endsAt, serverTime?, items:[product + { stock, sold }] }
    featured: '/products/featured',     // GET → [product]
    newest: '/products/new',            // GET → [product]
    bestSellers: '/products/best-sellers',
    collections: '/collections',        // GET → [{ id, name, slug, description, imageUrl }]
    products: '/products',              // GET ?q&category&brand&minPrice&maxPrice&size&color&sort&page&size
    news: '/news',                      // GET → [{ id, title, summary, imageUrl, publishedAt, slug }]
    reviews: '/reviews/featured',       // GET → [{ id, customerName, rating, content, productName }]
    newsletter: '/newsletter/subscribe',// POST { email }
    cart: '/cart/items',                // POST { productId, quantity }
    wishlist: '/wishlist/{id}',         // POST thêm · DELETE bỏ
    chat: null                          // Chuẩn bị cho AI sau này, ví dụ '/ai/chat'. null = chưa kết nối
  },

  // Bộ lọc size/màu/khoảng giá (dùng khi API chưa trả danh sách riêng)
  filterOptions: {
    sizes: ['36', '37', '38', '39', '40', '41', '42', '43', '44', '45'],
    colors: [
      { name: 'Đen', value: '#111111' }, { name: 'Trắng', value: '#ffffff' }, { name: 'Đỏ', value: '#e11d2a' },
      { name: 'Xanh dương', value: '#2563eb' }, { name: 'Xám', value: '#9ca3af' }, { name: 'Be', value: '#d6c3a3' }
    ],
    prices: [
      { label: 'Dưới 1 triệu', min: 0, max: 1000000 },
      { label: '1 – 2 triệu', min: 1000000, max: 2000000 },
      { label: '2 – 3 triệu', min: 2000000, max: 3000000 },
      { label: 'Trên 3 triệu', min: 3000000, max: 0 }
    ],
    pageSize: 8
  },

  store: { hotline: '1900 1234', email: 'cskh@sneakerhouse.vn', address: 'Hà Nội, Việt Nam' }
};
