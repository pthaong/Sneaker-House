/* Dữ liệu GIẢ cho trang chủ — chỉ dùng khi HOME_CONFIG.mock = true và chạy ở localhost.
   Mô phỏng các API mà BE1 chưa bàn giao (#15). Hình dạng dữ liệu = hợp đồng đề xuất trong README. */
window.HOME_MOCK = (() => {
  const img = (k) => `assets/p-${k}.webp`;
  const P = (id, name, brand, code, price, sale, im, cats, extra = {}) => ({
    id, slug: name.toLowerCase().replace(/[^a-z0-9]+/g, '-'), name, brand: brand.name, brandSlug: brand.slug, code,
    price, salePrice: sale, imageUrl: img(im), categories: cats, rating: 4 + ((id * 7) % 10) / 10, reviewCount: 12 + id * 9,
    inStock: true, sizes: ['38', '39', '40', '41', '42', '43'], colors: ['Đen', 'Trắng'], sold: 200 - id * 11,
    createdAt: new Date(Date.UTC(2026, 10, 18 - id)).toISOString(), ...extra,
  });
  const B = { nike: { name: 'Nike', slug: 'nike' }, adidas: { name: 'Adidas', slug: 'adidas' }, jordan: { name: 'Jordan', slug: 'jordan' },
    bal: { name: 'Balenciaga', slug: 'balenciaga' }, puma: { name: 'Puma', slug: 'puma' }, nb: { name: 'New Balance', slug: 'new-balance' },
    conv: { name: 'Converse', slug: 'converse' }, vans: { name: 'Vans', slug: 'vans' } };

  const PRODUCTS = [
    P(1, 'Adidas Samba OG', B.adidas, 'AD-SMB-001', 2790000, 2290000, 'samba', ['nam', 'nu'], { colors: ['Trắng', 'Đen'] }),
    P(2, 'Nike Air Force 1 \'07', B.nike, 'NK-AF1-007', 3239000, null, 'airforce', ['nam', 'nu'], { colors: ['Trắng'] }),
    P(3, 'Air Jordan 1 Retro High', B.jordan, 'JD-AJ1-010', 4500000, 3990000, 'jordan', ['nam'], { colors: ['Đỏ', 'Đen'] }),
    P(4, 'Balenciaga Runner', B.bal, 'BL-RUN-004', 12500000, null, 'balenciaga', ['nam', 'nu'], { colors: ['Xám'], inStock: false }),
    P(5, 'Nike Pegasus 41', B.nike, 'NK-PEG-041', 3519000, 2989000, 'airforce', ['nam', 'nu', 'chay-bo'], { colors: ['Xanh dương', 'Đen'] }),
    P(6, 'Adidas Ultraboost Light', B.adidas, 'AD-UBL-023', 4200000, null, 'samba', ['nam', 'chay-bo'], { colors: ['Đen'] }),
    P(7, 'Puma Suede Classic', B.puma, 'PM-SDE-110', 1890000, 1490000, 'jordan', ['nam', 'nu'], { colors: ['Đỏ', 'Be'] }),
    P(8, 'New Balance 550', B.nb, 'NB-550-008', 3090000, null, 'airforce', ['nam', 'nu'], { colors: ['Trắng', 'Xanh dương'] }),
    P(9, 'Converse Chuck 70', B.conv, 'CV-C70-070', 1990000, 1690000, 'balenciaga', ['nu', 'tre-em'], { sizes: ['30', '32', '34', '36', '38', '40'], colors: ['Đen', 'Be'] }),
    P(10, 'Vans Old Skool', B.vans, 'VN-OLD-001', 1790000, null, 'samba', ['nam', 'nu', 'tre-em'], { sizes: ['32', '34', '36', '38', '40', '42'], colors: ['Đen', 'Trắng'] }),
    P(11, 'Nike Court Borough Low (Trẻ em)', B.nike, 'NK-CBL-011', 1390000, 1090000, 'jordan', ['tre-em'], { sizes: ['28', '30', '32', '34', '36'], colors: ['Trắng', 'Xanh dương'] }),
    P(12, 'Adidas Adizero SL', B.adidas, 'AD-ADZ-012', 2690000, null, 'balenciaga', ['nam', 'nu', 'chay-bo'], { colors: ['Đỏ', 'Xám'] }),
  ];

  const CATEGORIES = [
    { id: 1, name: 'Giày nam', slug: 'nam', imageUrl: img('jordan'), description: 'Sneaker, chạy bộ, bóng rổ',
      children: ['Sneaker', 'Chạy bộ', 'Bóng rổ', 'Giày lười', 'Sandal'].map((n, i) => ({ id: 10 + i, name: n, slug: 'nam' })),
      types: ['Lifestyle', 'Running', 'Basketball', 'Training'] },
    { id: 2, name: 'Giày nữ', slug: 'nu', imageUrl: img('samba'), description: 'Thời trang, năng động',
      children: ['Sneaker', 'Chạy bộ', 'Giày thể thao', 'Sandal'].map((n, i) => ({ id: 20 + i, name: n, slug: 'nu' })),
      types: ['Lifestyle', 'Running', 'Training'] },
    { id: 3, name: 'Giày trẻ em', slug: 'tre-em', imageUrl: img('airforce'), description: 'Nhẹ, êm, bền',
      children: ['Bé trai', 'Bé gái', 'Đi học'].map((n, i) => ({ id: 30 + i, name: n, slug: 'tre-em' })), types: ['Lifestyle', 'Đi học'] },
    { id: 4, name: 'Giày chạy bộ', slug: 'chay-bo', imageUrl: img('balenciaga'), description: 'Êm chân, bám đường',
      children: ['Road', 'Trail', 'Tập gym'].map((n, i) => ({ id: 40 + i, name: n, slug: 'chay-bo' })), types: ['Road', 'Trail'] },
  ];
  const BRANDS = Object.values(B).map((b, i) => ({ id: i + 1, ...b, logoUrl: null }));
  const BANNERS = [
    { id: 1, position: 'HERO', title: 'Samba OG — Biểu tượng trở lại', subtitle: 'Bộ sưu tập mới về, số lượng có hạn. Miễn phí giao hàng từ 500.000đ.', ctaText: 'Khám phá ngay', link: '#new', imageUrl: 'assets/h-samba.webp', tag: 'Hàng mới về' },
    { id: 2, position: 'HERO', title: 'Flash Sale — Giảm đến 50%', subtitle: 'Săn deal giày chính hãng mỗi ngày, kết thúc là hết giá.', ctaText: 'Mua ngay', link: '#flash', imageUrl: 'assets/h-jordan.webp', tag: 'Khuyến mãi' },
    { id: 3, position: 'HERO', title: 'Air Force 1 — Trắng tinh tế', subtitle: 'Phối đồ dễ dàng, đi êm cả ngày.', ctaText: 'Khám phá ngay', link: '#featured', imageUrl: 'assets/h-airforce.webp', tag: 'Bán chạy' },
    { id: 4, position: 'PROMO', title: 'Đồng giá giày trẻ em', subtitle: 'Giảm 20% cho bộ sưu tập back-to-school.', ctaText: 'Xem ngay', link: 'index.html?category=tre-em#shop', imageUrl: 'assets/p-airforce.webp' },
    { id: 5, position: 'PROMO', title: 'Thành viên mới giảm 10%', subtitle: 'Đăng ký tài khoản để nhận mã ưu đãi đầu tiên.', ctaText: 'Đăng ký', link: '../fe-auth/register.html', imageUrl: 'assets/p-samba.webp' },
  ];
  const COLLECTIONS = [
    { id: 1, name: 'Retro Basketball', slug: 'retro', description: 'Di sản sân bóng rổ', imageUrl: img('jordan') },
    { id: 2, name: 'Everyday Classic', slug: 'classic', description: 'Dễ phối, đi mỗi ngày', imageUrl: img('airforce') },
    { id: 3, name: 'Terrace Culture', slug: 'terrace', description: 'Phong cách đường phố châu Âu', imageUrl: img('samba') },
    { id: 4, name: 'Luxury Street', slug: 'luxury', description: 'Cao cấp, nổi bật', imageUrl: img('balenciaga') },
  ];
  const NEWS = [
    { id: 1, title: 'Cách chọn size giày sneaker chuẩn khi mua online', summary: 'Đo chân đúng cách và đối chiếu bảng size từng hãng để không phải đổi trả.', publishedAt: '2026-11-12' },
    { id: 2, title: 'Phối đồ với Samba: 5 công thức dễ áp dụng', summary: 'Từ quần jean ống rộng đến váy midi, Samba hợp với nhiều phong cách.', publishedAt: '2026-11-08' },
    { id: 3, title: 'Bảo quản giày trắng không ố vàng', summary: 'Vệ sinh, phơi và cất giữ đúng cách giúp giày bền màu hơn.', publishedAt: '2026-11-02' },
  ];
  const REVIEWS = [
    { id: 1, customerName: 'Nguyễn Minh Anh', rating: 5, content: 'Giày chính hãng, đóng gói cẩn thận, giao nhanh hơn dự kiến.', productName: 'Adidas Samba OG' },
    { id: 2, customerName: 'Trần Quốc Huy', rating: 5, content: 'Tư vấn size chuẩn, đi vừa chân ngay lần đầu. Sẽ ủng hộ tiếp.', productName: 'Air Jordan 1 Retro High' },
    { id: 3, customerName: 'Lê Thu Hà', rating: 4, content: 'Mẫu đẹp như hình, đổi size dễ dàng trong 7 ngày.', productName: 'Nike Air Force 1' },
  ];

  const plain = (s) => String(s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/gi, 'd').toLowerCase();
  const delay = (data, ms = 350) => new Promise((r) => setTimeout(() => r({ ok: true, status: 200, data, network: false }), ms));

  function search(p) {
    let list = PRODUCTS.slice();
    const q = plain(p.q).trim();
    if (q) list = list.filter((x) => plain(`${x.name} ${x.code} ${x.brand}`).includes(q));
    if (p.category) list = list.filter((x) => x.categories.includes(p.category));
    if (p.brand) list = list.filter((x) => x.brandSlug === p.brand);
    const eff = (x) => x.salePrice || x.price;
    if (p.minPrice) list = list.filter((x) => eff(x) >= +p.minPrice);
    if (p.maxPrice) list = list.filter((x) => eff(x) <= +p.maxPrice);
    if (p.size) list = list.filter((x) => x.sizes.includes(String(p.size)));
    if (p.color) list = list.filter((x) => x.colors.includes(p.color));
    const sorts = { price_asc: (a, b) => eff(a) - eff(b), price_desc: (a, b) => eff(b) - eff(a),
      newest: (a, b) => b.createdAt.localeCompare(a.createdAt), bestseller: (a, b) => b.sold - a.sold };
    if (sorts[p.sort]) list.sort(sorts[p.sort]);
    const size = +p.pageSize || 8, page = Math.max(1, +p.page || 1);
    return { items: list.slice((page - 1) * size, page * size), total: list.length, page, totalPages: Math.max(1, Math.ceil(list.length / size)) };
  }

  function flash() {
    const end = new Date(Date.now() + 6 * 3600e3 + 24 * 60e3 + 37e3).toISOString();
    const items = [PRODUCTS[0], PRODUCTS[2], PRODUCTS[4], PRODUCTS[6]].map((x, i) => ({ ...x, stock: 40, sold: [31, 36, 18, 12][i] }));
    items.push({ ...PRODUCTS[8], stock: 20, sold: 20, inStock: false });
    return { endsAt: end, serverTime: new Date().toISOString(), items };
  }

  const byKey = {
    categories: () => CATEGORIES, brands: () => BRANDS, banners: () => BANNERS, flashSale: flash,
    featured: () => [PRODUCTS[0], PRODUCTS[1], PRODUCTS[2], PRODUCTS[7]], newest: () => PRODUCTS.slice().sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 4),
    bestSellers: () => PRODUCTS.slice().sort((a, b) => b.sold - a.sold).slice(0, 4), collections: () => COLLECTIONS, news: () => NEWS, reviews: () => REVIEWS,
    products: (o) => search(o.params || {}), newsletter: () => ({ message: 'Đăng ký thành công' }), cart: () => ({ message: 'OK' }), wishlist: () => ({ message: 'OK' }),
  };
  return { call: (key, o) => (byKey[key] ? delay(byKey[key](o || {})) : null) };
})();
