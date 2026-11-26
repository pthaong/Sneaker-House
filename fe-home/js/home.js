/* =========================================================
   Sneaker House — Trang chủ (FE1, issue #15). JS thuần, không thư viện.
   Cấu hình: js/home-config.js · Dữ liệu giả (khi mock=true): js/home-mock.js
   An toàn: mọi chuỗi từ server được chèn bằng textContent (không innerHTML); URL ảnh/liên kết được kiểm tra;
   không lưu mật khẩu/token ở trình duyệt.
   ========================================================= */
(() => {
  'use strict';

  /* ---------- Cấu hình ---------- */
  const user = window.HOME_CONFIG || {};
  const CFG = { apiBase: '/api', loginUrl: '../fe-auth/login.html', mock: false, links: {}, endpoints: {}, store: {},
    productUrl: 'product.html?id={id}', filterOptions: { sizes: [], colors: [], prices: [], pageSize: 8 }, ...user };
  const IS_LOCAL = ['localhost', '127.0.0.1', '[::1]'].includes(location.hostname);
  const MOCK = !!CFG.mock && IS_LOCAL && !!window.HOME_MOCK;
  if (CFG.mock && !IS_LOCAL) console.warn('[home] mock bị bỏ qua vì không chạy ở localhost — đang gọi API thật.');

  /* ---------- DOM ---------- */
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  function h(tag, attrs, ...kids) {
    const el = document.createElement(tag);
    Object.entries(attrs || {}).forEach(([k, v]) => {
      if (v == null || v === false) return;
      if (k === 'class') el.className = v;
      else if (k === 'html') el.innerHTML = v; // chỉ dùng cho SVG icon nội bộ
      else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
      else el.setAttribute(k, v === true ? '' : v);
    });
    kids.flat().forEach((c) => { if (c != null && c !== false) el.append(c.nodeType ? c : document.createTextNode(c)); });
    return el;
  }

  const ICONS = {
    search: '<circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/>',
    truck: '<path d="M1 3h15v13H1zM16 8h4l3 3v5h-7z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/>',
    refresh: '<path d="M23 4v6h-6M1 20v-6h6"/><path d="M3.5 9a9 9 0 0 1 14.9-3.4L23 10M1 14l4.6 4.4A9 9 0 0 0 20.5 15"/>',
    shieldCheck: '<path d="M12 3 4 6v6c0 4.5 3.2 8.3 8 9 4.8-.7 8-4.5 8-9V6z"/><path d="m9 12 2 2 4-4"/>',
    pin: '<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>',
    package: '<path d="m7.5 4.3 9 5.2M21 8v8l-9 5-9-5V8l9-5z"/><path d="M3.3 7 12 12l8.7-5M12 22V12"/>',
    headset: '<path d="M3 14v-2a9 9 0 0 1 18 0v2"/><path d="M21 15a2 2 0 0 1-2 2h-1v-6h1a2 2 0 0 1 2 2zM3 15a2 2 0 0 0 2 2h1v-6H5a2 2 0 0 0-2 2z"/><path d="M21 17v1a3 3 0 0 1-3 3h-4"/>',
    heart: '<path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z"/>',
    cart: '<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
    chevronDown: '<path d="m6 9 6 6 6-6"/>', chevronRight: '<path d="m9 6 6 6-6 6"/>', chevronLeft: '<path d="m15 6-6 6 6 6"/>',
    menu: '<path d="M3 6h18M3 12h18M3 18h18"/>', x: '<path d="M18 6 6 18M6 6l12 12"/>', minus: '<path d="M5 12h14"/>',
    star: '<path d="m12 2 3.1 6.3 6.9 1-5 4.9 1.2 6.8-6.2-3.3-6.2 3.3L8 14.2l-5-4.9 6.9-1z"/>',
    chat: '<path d="M21 15a2 2 0 0 1-2 2H8l-5 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>',
    send: '<path d="m22 2-7 20-4-9-9-4zM22 2 11 13"/>', flame: '<path d="M12 22c4 0 7-2.8 7-7 0-3.4-2-5.4-3.4-7.2C14.4 6.2 14 4.5 14 2c-3 1.6-6 5-6 9 0 .8.2 1.600.6 2.200C7.600 12.800 7 12 7 12c-1.200 1.200-2 2.800-2 4 0 3.400 3 6 7 6z"/>',
    cartPlus: '<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6M14 8v4M12 10h4"/>',
  };
  const icon = (n, cls) => h('span', { class: cls || null, html: `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">${ICONS[n] || ''}</svg>`, style: 'display:inline-grid' }).firstChild;
  function hydrateIcons(root = document) {
    $$('i[data-icon]', root).forEach((el) => { if (!el.firstChild) el.innerHTML = `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">${ICONS[el.dataset.icon] || ''}</svg>`; });
  }

  /* ---------- Tiện ích ---------- */
  const VND = new Intl.NumberFormat('vi-VN');
  const money = (n) => `${VND.format(Math.round(Number(n) || 0))}đ`;
  const plain = (s) => String(s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/gi, 'd').toLowerCase();
  const SAFE_URL = /^(https?:\/\/|\/(?!\/)|[\w.\-/]+\.(webp|png|jpe?g|svg|gif|avif)(\?.*)?$)/i;
  const origin = () => { try { return new URL(CFG.apiBase, location.href).origin; } catch (_) { return ''; } };
  function safeUrl(u) {
    if (typeof u !== 'string' || !SAFE_URL.test(u.trim())) return null;
    u = u.trim();
    return u.startsWith('/') ? origin() + u : u; // đường dẫn tuyệt đối trên server (vd /uploads/...) → ghép gốc API
  }
  const safeLink = (u, fallback = '#') => (typeof u === 'string' && /^(https?:\/\/|#|[\w./\-?=&%#]+$)/i.test(u.trim()) && !/^\s*javascript:/i.test(u) ? u.trim() : fallback);
  const linkFor = (key, fallback) => CFG.links[key] || fallback;
  const productHref = (p) => CFG.productUrl.replace('{id}', encodeURIComponent(p.id)).replace('{slug}', encodeURIComponent(p.slug || p.id));

  let toastTimer;
  function toast(text) {
    const t = $('[data-toast]'); t.textContent = text; t.hidden = false;
    clearTimeout(toastTimer); toastTimer = setTimeout(() => { t.hidden = true; }, 3600);
  }

  /* ---------- API ---------- */
  const EP = (k) => CFG.endpoints[k];
  let csrfCache = null;
  async function fetchCsrf() {
    const res = await fetch(CFG.apiBase + EP('csrf'), { credentials: 'include', headers: { Accept: 'application/json' } });
    if (!res.ok) throw new Error(`csrf ${res.status}`);
    csrfCache = await res.json(); return csrfCache;
  }
  // Trả về { ok, status, data, network }. Không bao giờ ném lỗi ra ngoài.
  async function api(key, { method = 'GET', body, params, path } = {}) {
    if (MOCK && key !== 'profile' && key !== 'logout' && key !== 'csrf') {
      const r = window.HOME_MOCK.call(key, { method, body, params }); if (r) return r;
    }
    const ep = EP(key);
    if (!ep) return { ok: false, status: 501, data: null, network: false };
    try {
      let url = CFG.apiBase + ep.replace('{id}', encodeURIComponent(path || ''));
      if (params) {
        const qs = new URLSearchParams();
        Object.entries(params).forEach(([k, v]) => { if (v !== '' && v != null) qs.set(k, v); });
        if ([...qs].length) url += `?${qs}`;
      }
      const headers = { Accept: 'application/json' };
      const opts = { method, credentials: 'include', headers };
      if (method !== 'GET') { headers['Content-Type'] = 'application/json'; if (body !== undefined) opts.body = JSON.stringify(body); }
      const send = async (fresh) => {
        if (method !== 'GET') { const c = !csrfCache || fresh ? await fetchCsrf() : csrfCache; headers[c.headerName || 'X-XSRF-TOKEN'] = c.token; }
        return fetch(url, opts);
      };
      let res = await send(false);
      if (res.status === 403 && method !== 'GET') res = await send(true);
      let data = null; try { data = await res.json(); } catch (_) { /* không phải JSON */ }
      return { ok: res.ok, status: res.status, data, network: false };
    } catch (err) { console.error('[home] API lỗi:', err); return { ok: false, status: 0, data: null, network: true }; }
  }
  const listOf = (d) => (Array.isArray(d) ? d : d && (d.items || d.content || d.data || d.products || d.results)) || [];
  const errText = (r, fb) => (r.network ? 'Không kết nối được tới máy chủ. Vui lòng kiểm tra mạng rồi thử lại.' : (r.data && typeof r.data.message === 'string' && r.data.message.trim()) || fb);

  /* ---------- Chuẩn hóa sản phẩm ---------- */

/*
 * Chuẩn hóa dữ liệu sản phẩm từ Backend.
 *
 * Backend PR #18 sử dụng:
 * originalPrice, salePrice, stockQuantity,
 * soldCount, flashSaleEnd.
 *
 * Giữ các tên price, sale, stock, sold ở giao diện
 * để không ảnh hưởng các component đã viết.
 */
function norm(p) {
  const price = Number(p.originalPrice ?? p.price ?? 0);

  const saleValue = Number(
    p.salePrice ?? p.discountPrice ?? 0
  );

  const sale =
    saleValue > 0 && saleValue < price
      ? saleValue
      : null;

  const stockValue = p.stockQuantity ?? p.stock;

  const stock =
    stockValue == null
      ? null
      : Number(stockValue);

  const soldValue = p.soldCount ?? p.sold;

  const sold =
    soldValue == null
      ? null
      : Number(soldValue);

  const brand =
    typeof p.brand === 'object' && p.brand
      ? p.brand.name
      : p.brand || '';

  const inStock =
    stock !== null
      ? stock > 0
      : p.inStock != null
        ? Boolean(p.inStock)
        : true;

  return {
    id: p.id,
    slug: p.slug,
    name: String(p.name || p.title || 'Sản phẩm'),
    code: p.sku || p.code || '',

    brand,

    price,
    sale,

    percent: sale
      ? Math.round((1 - sale / price) * 100)
      : 0,

    image: safeUrl(
      p.imageUrl || p.image || p.thumbnail
    ),

    rating: Number(
      p.rating ?? p.averageRating ?? 0
    ),

    reviews: Number(
      p.reviewCount ?? p.reviews ?? 0
    ),

    inStock,
    stock,
    sold,

    flashSaleEnd: p.flashSaleEnd ?? null
  };
}

  /* ---------- Thẻ sản phẩm ---------- */
  const wished = new Set();
  const stars = (r) => {
    const w = h('span', { class: 'stars', role: 'img', 'aria-label': `${r.toFixed(1)} trên 5 sao` });
    for (let i = 1; i <= 5; i++) { const s = icon('star'); if (i > Math.round(r)) s.classList.add('off'); w.append(s); }
    return w;
  };

function card(raw, { flash = false } = {}) {
  return window.SneakerProductCard.render(raw, {
    h,
    icon,
    norm,
    productHref,
    money,
    stars,
    toggleWish,
    addToCart,
    flash
  });
}

  async function toggleWish(p, btn) {
    if (!state.user) { toast('Vui lòng đăng nhập để dùng danh sách yêu thích.'); return; }
    const on = !wished.has(p.id);
    const r = await api('wishlist', { method: on ? 'POST' : 'DELETE', path: p.id });
    if (r.status === 404 || r.status === 501) { toast('Chức năng yêu thích chưa được kết nối với máy chủ.'); return; }
    if (!r.ok) { toast(errText(r, 'Không cập nhật được yêu thích. Vui lòng thử lại.')); return; }
    if (on) wished.add(p.id); else wished.delete(p.id);
    btn.setAttribute('aria-pressed', String(on));
    toast(on ? 'Đã thêm vào yêu thích.' : 'Đã bỏ khỏi yêu thích.');
  }
  async function addToCart(p) {
    if (!state.user) { toast('Vui lòng đăng nhập để thêm vào giỏ hàng.'); return; }
    const r = await api('cart', { method: 'POST', body: { productId: p.id, quantity: 1 } });
    if (r.status === 404 || r.status === 501) { toast('Giỏ hàng chưa được kết nối với máy chủ.'); return; }
    toast(r.ok ? 'Đã thêm vào giỏ hàng.' : errText(r, 'Không thêm được vào giỏ. Vui lòng chọn size ở trang chi tiết.'));
  }

  /* ---------- Tải một khu vực: loading → dữ liệu / rỗng / lỗi ---------- */
  function skeleton(slot, kind, n) {
    slot.replaceChildren(...Array.from({ length: n }, () => h('div', { class: `skel skel--${kind}` })));
    slot.setAttribute('aria-busy', 'true');
  }
  function stateBox(slot, text, { error = false, retry = null } = {}) {
    slot.removeAttribute('aria-busy');
    slot.replaceChildren(h('div', { class: `state${error ? ' state--error' : ''}`, role: error ? 'alert' : 'status', style: 'grid-column:1/-1' },
      h('p', null, text), retry ? h('button', { class: 'btn btn--small', type: 'button', onclick: retry }, 'Thử lại') : null));
  }
  async function loadSlot(name, { key, params, skel = ['card', 4], empty, render, hideIfEmpty = null, pick = listOf }) {
    const slot = $(`[data-slot="${name}"]`); if (!slot) return null;
    const run = async () => {
      skeleton(slot, skel[0], skel[1]);
      const r = await api(key, { params });
      if (!r.ok) {
        if (hideIfEmpty && (r.status === 404 || r.status === 501)) { hideIfEmpty(true); return null; }
        if (hideIfEmpty) hideIfEmpty(false); // lỗi thật (mạng/500): hiện khu vực kèm nút "Thử lại"
        stateBox(slot, errText(r, 'Không tải được dữ liệu. Vui lòng thử lại.'), { error: true, retry: run }); return null;
      }
      const items = pick(r.data);
      if (!items || !items.length) { if (hideIfEmpty) { hideIfEmpty(true); return null; } stateBox(slot, empty); return r.data; }
      if (hideIfEmpty) hideIfEmpty(false);
      slot.removeAttribute('aria-busy'); slot.replaceChildren(...render(items)); hydrateIcons(slot); return r.data;
    };
    return run();
  }
  const sectionToggle = (sel) => (hide) => { const s = $(sel); if (s) s.hidden = hide; };

  /* ---------- Phiên đăng nhập + menu tài khoản ---------- */
  const state = { user: null, categories: [], brands: [] };
  function renderAccount() {
    const list = $('[data-account-list]'); list.replaceChildren();
    const item = (href, text, key) => h('li', null, h('a', { href: key ? linkFor(key, href) : href }, text));
    if (state.user) {
      $('[data-user-name]').textContent = (state.user.fullName || state.user.email || 'Tài khoản').split(' ').slice(-2).join(' ');
      list.append(item('../fe-account/profile.html', 'Thông tin cá nhân', 'profile'), item('../fe-account/account-security.html', 'Bảo mật tài khoản', 'security'),
        item('orders.html', 'Đơn hàng của tôi', 'orders'),
        h('li', null, h('button', { type: 'button', onclick: logout }, 'Đăng xuất')));
    } else {
      $('[data-user-name]').textContent = 'Tài khoản';
      list.append(h('li', null, h('a', { href: CFG.loginUrl }, 'Đăng nhập')), h('li', null, h('a', { href: CFG.loginUrl.replace('login.html', 'register.html') }, 'Đăng ký')));
    }
  }
  async function loadUser() {
    const r = await api('profile');
    state.user = r.ok && r.data && r.data.email ? r.data : null; // 401/403/lỗi mạng ⇒ khách
    renderAccount();
  }
  async function logout() {
    const r = await api('logout', { method: 'POST' });
    if (r.ok || r.status === 401 || r.status === 403) { state.user = null; renderAccount(); toast('Bạn đã đăng xuất.'); } else toast(errText(r, 'Không đăng xuất được. Vui lòng thử lại.'));
  }
  function bindMenu() {
    const wrap = $('[data-menu]'), btn = $('[data-menu-btn]'), list = $('[data-account-list]');
    const set = (open) => { list.hidden = !open; btn.setAttribute('aria-expanded', String(open)); };
    btn.addEventListener('click', () => set(list.hidden));
    document.addEventListener('click', (e) => { if (!wrap.contains(e.target)) set(false); });
    wrap.addEventListener('keydown', (e) => { if (e.key === 'Escape') { set(false); btn.focus(); } });
  }

  /* ---------- Mega-menu ---------- */

const shopLink = (o) => {
  const q = new URLSearchParams(o).toString();

  // Danh muc san pham: chuyen sang trang products.html
  if (o.category && Object.keys(o).length === 1) {
    return `products.html?${q}`;
  }

  // Giu nguyen cac lien ket loc khac trong giai doan nay
  return `index.html?${q}#shop`;
};
  function bindMega() {
    const mega = $('[data-mega]'), btn = $('[data-mega-btn]'), scrim = $('[data-mega-scrim]');
    const main = $('[data-mega-main]'), body = $('[data-mega-body]');
    let active = 0, closeTimer;
    const set = (open) => {
      mega.hidden = !open; scrim.hidden = !open; btn.setAttribute('aria-expanded', String(open));
      document.body.style.overflow = open && matchMedia('(max-width:720px)').matches ? 'hidden' : '';
      document.body.classList.toggle('mega-open', open);
    };
    const showCat = (i) => {
      active = i; const c = state.categories[i]; if (!c) return;
      $$('button', main).forEach((b, j) => b.setAttribute('aria-selected', String(j === i)));
      const col = (title, links) => h('div', null, h('h3', null, title), h('ul', { class: 'mega__list' }, links.map((l) => h('li', null, h('a', { href: l.href }, l.text)))));
      const subs = (c.children || []).map((s) => ({ text: s.name, href: shopLink({ category: c.slug, q: s.name === 'Sneaker' ? '' : s.name }) }));
      subs.unshift({ text: `Tất cả ${c.name.toLowerCase()}`, href: shopLink({ category: c.slug }) });
      const types = (c.types || []).map((t) => ({ text: t, href: shopLink({ category: c.slug, q: t }) }));
      body.replaceChildren(col('Theo dòng sản phẩm', subs), types.length ? col('Loại giày', types) : h('div'));
    };
    const build = () => {
      main.replaceChildren(...state.categories.map((c, i) => h('li', { role: 'none' }, h('button', { type: 'button', role: 'menuitem', 'aria-selected': 'false',
        onmouseenter: () => showCat(i), onfocus: () => showCat(i), onclick: () => showCat(i) }, c.name, icon('chevronRight')))));
      hydrateIcons(main);
      const brands = $('[data-mega-brands]');
      brands.replaceChildren(h('h3', null, 'Thương hiệu'), h('div', { class: 'chips' }, state.brands.slice(0, 8).map((b) => { const lg = safeUrl(b.logoUrl); return h('a', { href: shopLink({ brand: b.slug }), 'aria-label': b.name }, lg ? h('img', { src: lg, alt: b.name }) : b.name); })));
      const prices = $('[data-mega-prices]');
      prices.replaceChildren(h('h3', null, 'Khoảng giá'), h('div', { class: 'chips' }, CFG.filterOptions.prices.map((p) => h('a', { href: shopLink({ minPrice: p.min || '', maxPrice: p.max || '' }) }, p.label))));
      showCat(0);
    };
    state.buildMega = build;
    const empty = () => { main.replaceChildren(h('li', null, 'Chưa có danh mục.')); };
    btn.addEventListener('click', () => set(mega.hidden));
    btn.addEventListener('mouseenter', () => { if (matchMedia('(hover:hover) and (min-width:1025px)').matches) { clearTimeout(closeTimer); set(true); } });
    [btn, mega].forEach((el) => el.addEventListener('mouseleave', () => { if (matchMedia('(hover:hover) and (min-width:1025px)').matches) closeTimer = setTimeout(() => set(false), 180); }));
    mega.addEventListener('mouseenter', () => clearTimeout(closeTimer));
    scrim.addEventListener('click', () => set(false));
    $('[data-mega-close]').addEventListener('click', () => { set(false); btn.focus(); });
    mega.addEventListener('click', (e) => { if (e.target.closest('a')) set(false); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && !mega.hidden) { set(false); btn.focus(); } });
    state.megaEmpty = empty; state.megaSet = set;
  }

  /* ---------- Hero ---------- */
  function renderHero(banners) {
    const stage = $('[data-hero-stage]'), dots = $('[data-hero-dots]');
    const list = banners.filter(
  (b) => String(b.bannerType || '').toUpperCase() === 'MAIN'
);
    if (!list.length) { $('[data-hero]').hidden = true; return; }
    let cur = 0, timer;
    $$('.hero__nav').forEach((b) => { b.hidden = false; }); dots.hidden = false;
    const slides = list.map((b, i) => {
      const img = safeUrl(b.imageUrl);
      return h('div', { class: 'slide', role: 'group', 'aria-roledescription': 'slide', 'aria-label': `${i + 1} / ${list.length}` },
        h('div', null, b.tag ? h('span', { class: 'slide__tag' }, b.tag) : null, h('h2', { class: 'slide__title' }, b.title || ''),
          b.subtitle ? h('p', { class: 'slide__sub' }, b.subtitle) : null,
          h('a', { class: 'btn', href: safeLink(b.targetUrl || b.link) }, b.ctaText || 'Khám phá ngay')),
        img ? h('img', { class: 'slide__img', src: img, alt: b.title || '', width: 560, height: 700, loading: i ? 'lazy' : 'eager' }) : null);
    });
    stage.replaceChildren(...slides);
    dots.replaceChildren(...list.map((b, i) => h('button', { type: 'button', role: 'tab', 'aria-label': `Banner ${i + 1}`, 'aria-selected': 'false', onclick: () => go(i, true) })));
    const go = (i, user) => {
      cur = (i + list.length) % list.length;
      slides.forEach((s, j) => { s.classList.toggle('is-active', j === cur); s.toggleAttribute('inert', j !== cur); });
      $$('button', dots).forEach((d, j) => d.setAttribute('aria-selected', String(j === cur)));
      if (user) restart();
    };
    const reduce = matchMedia('(prefers-reduced-motion:reduce)').matches;
    const restart = () => { clearInterval(timer); if (!reduce && list.length > 1) timer = setInterval(() => go(cur + 1), 6000); };
    $('[data-hero-prev]').onclick = () => go(cur - 1, true);
    $('[data-hero-next]').onclick = () => go(cur + 1, true);
    if (list.length < 2) { $$('.hero__nav').forEach((b) => { b.hidden = true; }); dots.hidden = true; }
    const hero = $('[data-hero]');
    hero.addEventListener('mouseenter', () => clearInterval(timer)); hero.addEventListener('mouseleave', restart);
    hero.addEventListener('focusin', () => clearInterval(timer)); hero.addEventListener('focusout', restart);
    go(0); restart();
  }
  function renderPromos(banners) {
    const slot = $('[data-slot="promos"]');
    const list = banners.filter(
  (b) => String(b.bannerType || '').toUpperCase() === 'AD'
);
    if (!list.length) { slot.closest('section').hidden = true; return; }
    slot.replaceChildren(...list.map((b) => { const img = safeUrl(b.imageUrl);
      return h('a', { class: 'promo-card', href: safeLink(b.targetUrl || b.link) }, h('strong', null, b.title || ''), b.subtitle ? h('span', null, b.subtitle) : null, h('em', null, b.ctaText || 'Xem ngay'),
        img ? h('img', { src: img, alt: '', loading: 'lazy', width: 160, height: 200 }) : null); }));
  }
  async function loadBanners() {
    const hero = $('[data-hero-stage]');
    const r = await api('banners', {
  params: { type: 'ALL' }
});
    if (!r.ok) {
      if (r.status === 404 || r.status === 501) { $('[data-hero]').hidden = true; $('[data-slot="promos"]').closest('section').hidden = true; return; }
      $$('.hero__nav').forEach((b) => { b.hidden = true; }); $('[data-hero-dots]').hidden = true;
      hero.replaceChildren(h('div', { class: 'state state--error', role: 'alert', style: 'position:absolute;inset:0;border-radius:0;align-content:center' }, h('p', null, errText(r, 'Không tải được banner.')),
        h('button', { class: 'btn btn--small', type: 'button', onclick: loadBanners }, 'Thử lại'))); return;
    }
    const list = listOf(r.data); renderHero(list); renderPromos(list);
  }

  /* ---------- Danh mục, thương hiệu ---------- */
  async function loadCategories() {
    const data = await loadSlot('categories', { key: 'categories', skel: ['tile', 4], empty: 'Chưa có danh mục nào.', render: (items) => {
      state.categories = items; return items.slice(0, 4).map((c) => { const img = safeUrl(c.imageUrl);
        return h('a', { class: 'cat-tile',  href: `products.html?category=${encodeURIComponent(c.slug)}` }, img ? h('img', { src: img, alt: '', loading: 'lazy', width: 200, height: 250 }) : null,
          h('strong', null, c.name), h('span', null, c.description || 'Xem sản phẩm')); }); } });
    if (data) state.categories = listOf(data); else state.megaEmpty && state.megaEmpty();
    if (state.brandsReady) state.buildMega(); else state.catsReady = true;
    if (state.categories.length) fillFilterOptions();
  }
  async function loadBrands() {
    const data = await loadSlot('brands', { key: 'brands', skel: ['line', 8], empty: 'Chưa có thương hiệu nào.', render: (items) => items.slice(0, 12).map((b) => { const img = safeUrl(b.logoUrl);
      return h('a', { class: 'brand', href: `products.html?brand=${encodeURIComponent(b.slug || b.id)}`, 'aria-label': b.name }, img ? h('img', { src: img, alt: b.name, loading: 'lazy' }) : b.name); }) });
    if (data) state.brands = listOf(data);
    state.brandsReady = true; if (state.catsReady && state.categories.length) state.buildMega();
    fillFilterOptions();
  }

  /* ---------- Flash Sale ---------- */

/*
 * Flash Sale - ket noi Backend PR #18.
 *
 * GET /api/flash-sales
 * Response: ProductResponse[]
 *
 * Moi san pham co:
 * originalPrice, salePrice, stockQuantity,
 * soldCount, flashSaleEnd.
 *
 * Khong su dung data.endsAt cua Mock.
 */

let flashTimer = null;

async function loadFlash() {
  clearInterval(flashTimer);
  flashTimer = null;

  const getEndTime = (product) => {
    if (!product.flashSaleEnd) return NaN;
    return new Date(product.flashSaleEnd).getTime();
  };

  // Chi hien thi san pham Flash Sale con thoi han.
  const activeProducts = (data) => {
    const now = Date.now();

    return listOf(data).filter((product) => {
      const end = getEndTime(product);
      return Number.isFinite(end) && end > now;
    });
  };

  const data = await loadSlot('flash', {
    key: 'flashSale',
    skel: ['card', 4],
    empty: 'Hiện chưa có sản phẩm Flash Sale.',
    pick: activeProducts,
    hideIfEmpty: sectionToggle('[data-flash]'),

    render: (items) =>
      items.slice(0, 8).map((product) =>
        card(product, { flash: true })
      )
  });

  if (!data) return;

  const products = activeProducts(data);
  if (!products.length) return;

  // Lay thoi diem ket thuc gan nhat.
  const endTimes = products
    .map(getEndTime)
    .filter(Number.isFinite);

  if (!endTimes.length) return;

  const nearestEnd = Math.min(...endTimes);

  const pad = (value) => String(value).padStart(2, '0');

  const tick = () => {
    const remaining = Math.max(
      0,
      nearestEnd - Date.now()
    );

    const hours = Math.floor(remaining / 3600000);
    const minutes = Math.floor(
      (remaining % 3600000) / 60000
    );
    const seconds = Math.floor(
      (remaining % 60000) / 1000
    );

    $('[data-cd="h"]').textContent = pad(hours);
    $('[data-cd="m"]').textContent = pad(minutes);
    $('[data-cd="s"]').textContent = pad(seconds);

    if (remaining <= 0) {
      clearInterval(flashTimer);
      flashTimer = null;

      // Lay lai danh sach khuyen mai con hieu luc.
      // Khong vo hieu hoa cac san pham van con Flash Sale.
      loadFlash();
    }
  };

  tick();

  if (nearestEnd > Date.now()) {
    flashTimer = setInterval(tick, 1000);
  }
}


  /* ---------- Bộ sưu tập, tin tức, đánh giá ---------- */
  const fmtDate = (d) => { const t = new Date(d); return Number.isNaN(t.getTime()) ? '' : t.toLocaleDateString('vi-VN'); };
  function loadCollections() {
    return loadSlot('collections', { key: 'collections', skel: ['tile', 4], empty: 'Chưa có bộ sưu tập nào.', render: (items) => items.slice(0, 4).map((c) => { const img = safeUrl(c.imageUrl);
      return h('a', { class: 'col-card', href: linkFor('collections', shopLink({ q: c.name })) }, img ? h('img', { src: img, alt: '', loading: 'lazy', width: 400, height: 300 }) : null,
        h('div', null, h('strong', null, c.name), h('span', null, c.description || ''))); }) });
  }
  function loadNews() {
    return loadSlot('news', { key: 'news', skel: ['tile', 3], empty: 'Chưa có bài viết nào.', render: (items) => items.slice(0, 3).map((n) => { const img = safeUrl(n.imageUrl);
      return h('a', { class: 'news', href: linkFor('news', '#news') }, h('div', { class: 'news__img' }, img ? h('img', { src: img, alt: '', loading: 'lazy' }) : 'SH'),
        h('div', { class: 'news__body' }, h('time', null, fmtDate(n.publishedAt)), h('strong', null, n.title || ''), n.summary ? h('p', null, n.summary) : null)); }) });
  }
  function loadReviews() {
    return loadSlot('reviews', { key: 'reviews', skel: ['tile', 3], empty: '', hideIfEmpty: sectionToggle('[data-reviews]'), render: (items) => items.slice(0, 3).map((v) =>
      h('figure', { class: 'rev' }, stars(Number(v.rating) || 0), h('blockquote', null, v.content || ''), h('small', null, `${v.customerName || 'Khách hàng'}${v.productName ? ` · ${v.productName}` : ''}`))) });
  }

  /* ---------- Tìm kiếm, lọc, sắp xếp ---------- */
  const filters = { form: null, page: 1, totalPages: 1, busy: false };
  function readQuery() {
    const q = new URLSearchParams(location.search); const f = filters.form;
    ['q', 'category', 'brand', 'sort'].forEach((k) => {
      const v = q.get(k); if (!v || !f.elements[k]) return;
      if (f.elements[k].tagName === 'SELECT' && ![...f.elements[k].options].some((o) => o.value === v)) f.elements[k].append(h('option', { value: v }, v)); // nhãn đẹp được điền khi API danh mục/thương hiệu trả về
      f.elements[k].value = v;
    });
    $('#q').value = f.elements.q.value;
    const min = q.get('minPrice') || '', max = q.get('maxPrice') || '';
    if (min || max) { const i = CFG.filterOptions.prices.findIndex((p) => String(p.min || '') === min && String(p.max || '') === max); if (i >= 0) f.elements.price.value = String(i); }
    if (q.get('size')) $$('input[name="size"]', f).forEach((c) => { c.checked = c.value === q.get('size'); });
    if (q.get('color')) $$('input[name="color"]', f).forEach((c) => { c.checked = c.value === q.get('color'); });
  }
  function fillFilterOptions() {
    const f = filters.form; if (!f) return;
    const fill = (name, items, val, label) => { const sel = f.elements[name]; const keep = sel.value; sel.length = 1; items.forEach((x) => sel.append(h('option', { value: val(x) }, label(x)))); if (keep) { if (![...sel.options].some((o) => o.value === keep)) sel.append(h('option', { value: keep }, keep)); sel.value = keep; } };

    const leafCategories = [];

    const collectCategories = (items) => {
      items.forEach((category) => {
        if (category.children?.length) {
          collectCategories(category.children);
        } else {
          leafCategories.push(category);
        }
      });
    };

    collectCategories(state.categories);

    fill(
      'category',
      leafCategories,
      (c) => c.slug,
      (c) => c.name
    );

    fill(
      'brand',
      state.brands,
      (b) => b.slug || String(b.id),
      (b) => b.name
    );
    const q = new URLSearchParams(location.search); const sc = f.elements.category, sb = f.elements.brand;
    if (q.get('category')) sc.value = q.get('category'); if (q.get('brand')) sb.value = q.get('brand');
  }
  function buildFilters() {
    const f = filters.form = $('[data-filters]'); const o = CFG.filterOptions;
    o.prices.forEach((p, i) => f.elements.price.append(h('option', { value: String(i) }, p.label)));
    $('[data-sizes]').replaceChildren(...o.sizes.map((s) => h('label', null, h('input', { type: 'radio', name: 'size', value: s }), h('span', null, s))));
    $('[data-colors]').replaceChildren(...o.colors.map((c) => h('label', { title: c.name }, h('input', { type: 'radio', name: 'color', value: c.name, 'aria-label': c.name }),
      h('span', { class: 'dot' }, h('i', { style: `background:${/^#[0-9a-f]{3,8}$/i.test(c.value) ? c.value : '#ccc'}` })))));
    // Bấm lại ô đang chọn để bỏ chọn size/màu
    $$('.chipset input', f).forEach((i) => { i.addEventListener('click', () => { if (i.dataset.was === '1') { i.checked = false; i.dataset.was = ''; } else { $$(`input[name="${i.name}"]`, f).forEach((x) => { x.dataset.was = ''; }); i.dataset.was = '1'; } }); });

    const shop = $('#shop');

    const revealShop = () => {
      if (shop) shop.hidden = false;
    };

    const showResults = () => {
  revealShop();
  runSearch(true);
};

    state.revealShop = revealShop;

    // Tìm kiếm bằng bộ lọc gốc
    f.addEventListener('submit', (e) => {
      e.preventDefault();
      $('#q').value = f.elements.q.value.trim();
      showResults();
    });

    // Thay đổi giá, size, màu sắc, danh mục, thương hiệu
    // Đặt lại bộ lọc
    $('[data-reset]').addEventListener('click', () => {
  f.reset();
  $('#q').value = '';

  // Chỉ xóa lựa chọn, chưa tìm kiếm
  // Người dùng cần nhấn Áp dụng để cập nhật kết quả
});

    // Xem thêm sản phẩm
    $('[data-loadmore] button').addEventListener('click', () => {
      revealShop();
      runSearch(false);
    });

    // Tìm kiếm từ thanh tìm kiếm trang chủ
    $('[data-search]').addEventListener('submit', (e) => {
      e.preventDefault();

      f.elements.q.value = $('#q').value.trim();
      f.elements.category.value = '';
      f.elements.brand.value = '';

      showResults();
    });

    bindHeaderSuggestions();
    readQuery();
  }

/* ===== GOI Y TIM KIEM SAN PHAM HEADER ===== */

function bindHeaderSuggestions() {
  const form = $('[data-search]');
  const input = $('#q');
  const panel = $('[data-search-suggest]');
  const list = $('[data-search-suggest-list]');
  const viewAll = $('[data-search-suggest-all]');

  if (!form || !input || !panel || !list || !viewAll) return;

  let timer = null;
  let requestId = 0;

  function closeSuggestions() {
    clearTimeout(timer);
    requestId++;
    panel.hidden = true;
    input.setAttribute('aria-expanded', 'false');
  }

  function openSuggestions() {
    panel.hidden = false;
    input.setAttribute('aria-expanded', 'true');
  }

  function showMessage(message) {
    list.replaceChildren(
      h('p', { class: 'search__suggest-message' }, message)
    );
    viewAll.hidden = true;
    openSuggestions();
  }

  function createSuggestion(product) {
    const p = norm(product);

    let image;

    if (p.image) {
      image = h('img', {
        class: 'search__suggest-image',
        src: p.image,
        alt: p.name,
        loading: 'lazy'
      });

      image.addEventListener('error', () => {
        image.replaceWith(
          h('span', {
            class: 'search__suggest-image search__suggest-image--empty'
          }, 'Chưa có ảnh')
        );
      }, { once: true });
    } else {
      image = h('span', {
        class: 'search__suggest-image search__suggest-image--empty'
      }, 'Chưa có ảnh');
    }

    const info = h(
      'span',
      { class: 'search__suggest-info' },
      h('span', { class: 'search__suggest-name' }, p.name),

      ...(p.sale
        ? [h('span', {
            class: 'search__suggest-old'
          }, money(p.price))]
        : []),

      h('span', {
        class: 'search__suggest-price'
      }, money(p.sale || p.price))
    );

    return h(
      'a',
      {
        class: 'search__suggest-item',
        href: safeLink(productHref(p))
      },
      image,
      info
    );
  }

  function searchSuggestions() {
    closeSuggestions();

    const keyword = input.value.trim();

    if (keyword.length < 2) return;

    const currentRequest = requestId;

    timer = setTimeout(async () => {
      const response = await api('products', {
        params: {
          keyword,
          page: 0,
          sizePerPage: 6
        }
      });

      // Bo qua ket qua cu khi nguoi dung tiep tuc go
      if (
        currentRequest !== requestId ||
        input.value.trim() !== keyword
      ) return;

      if (!response.ok) {
        showMessage('Không tải được gợi ý sản phẩm.');
        return;
      }

      const products = listOf(response.data)
        .filter(p => p && p.id != null)
        .slice(0, 6);

      if (!products.length) {
        showMessage('Không tìm thấy sản phẩm phù hợp.');
        return;
      }

      list.replaceChildren(
        ...products.map(createSuggestion)
      );

      viewAll.hidden = false;
      openSuggestions();

    }, 300);
  }

  // Tu dong goi y khi nhap tu khoa
  input.addEventListener('input', searchSuggestions);

  input.addEventListener('focus', () => {
    if (input.value.trim().length >= 2 && panel.hidden) {
      searchSuggestions();
    }
  });

  // Nhan ESC de dong
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      closeSuggestions();
    }
  });

  // Dong dropdown khi nhan tim kiem
  form.addEventListener('submit', closeSuggestions);

  // Xem tat ca ket qua bang chuc nang tim kiem cu
 viewAll.addEventListener('click', () => {
  closeSuggestions();

  // Thực hiện tìm kiếm đầy đủ
  form.requestSubmit();

  // Cuộn tới kết quả ngay dưới bộ lọc
  requestAnimationFrame(() => {
    $('#shop')?.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    });
  });
});

  // Click ra ngoai thi dong dropdown
  document.addEventListener('pointerdown', (e) => {
    if (!form.contains(e.target)) {
      closeSuggestions();
    }
  });
}
function currentParams() {
  const f = filters.form;

  const priceIndex = f.elements.price.value;

  const pr = priceIndex === ''
    ? null
    : CFG.filterOptions.prices[Number(priceIndex)];

  const val = (name) => {
    const selected = f.querySelector(
      `input[name="${name}"]:checked`
    );
    return selected ? selected.value : '';
  };

  // Giao diện đang lưu slug, Backend lọc bằng ID.
  const selectedCategory = f.elements.category.value;
  const selectedBrand = f.elements.brand.value;

  const findCategory = (items, slug) => {
    for (const item of items) {
      if (String(item.slug) === slug) return item;

      const child = findCategory(item.children || [], slug);
      if (child) return child;
    }

    return null;
  };

  const category = selectedCategory
    ? findCategory(state.categories, selectedCategory)
    : null;

  const brand = selectedBrand
    ? state.brands.find(
        (item) => String(item.slug || item.id) === selectedBrand
      )
    : null;

  return {
    keyword: f.elements.q.value.trim(),

    categoryId: category?.id ?? '',
    brandId: brand?.id ?? '',

    minPrice: pr ? pr.min : '',
    maxPrice: pr && pr.max > 0 ? pr.max : '',

    size: val('size'),
    color: val('color'),
    sort: f.elements.sort.value,

    sizePerPage: CFG.filterOptions.pageSize
  };
}

/*
 * Tim kiem va phan trang san pham.
 *
 * Backend PR #18:
 * GET /api/products
 *
 * Query:
 * keyword, categoryId, brandId,
 * minPrice, maxPrice, size, color,
 * sort, page, sizePerPage.
 *
 * Response:
 * content, page, size,
 * totalElements, totalPages, last.
 *
 * Luu y: Backend bat dau page tu 0.
 */
async function runSearch(reset) {
  if (filters.busy) return;

  filters.busy = true;

  const slot = $('[data-slot="shop"]');
  const line = $('[data-resultline]');
  const more = $('[data-loadmore]');

  // Trang dau tien cua Backend la page = 0.
  const requestedPage = reset
    ? 0
    : filters.page + 1;

  if (reset) {
    skeleton(slot, 'card', 4);
    line.textContent = 'Đang tìm sản phẩm...';
    more.hidden = true;
  }

  const r = await api('products', {
    params: {
      ...currentParams(),
      page: requestedPage
    }
  });

  filters.busy = false;

  // Xu ly loi API / mang.
  if (!r.ok) {
    if (reset) {
      stateBox(
        slot,
        errText(
          r,
          'Không tìm được sản phẩm. Vui lòng thử lại.'
        ),
        {
          error: true,
          retry: () => runSearch(true)
        }
      );

      line.textContent = 'Không tải được sản phẩm.';
      more.hidden = true;
    } else {
      toast(
        errText(
          r,
          'Không tải thêm được sản phẩm.'
        )
      );
    }

    return;
  }

  // Backend tra danh sach trong content.
  const items = listOf(r.data);

  // Tong so san pham tu SQL Server.
  const total = Number(
    r.data?.totalElements ?? items.length
  );

  const totalPages = Number(
    r.data?.totalPages ?? 1
  );

  // Cap nhat trang hien tai khi API thanh cong.
  filters.page = requestedPage;
  filters.totalPages = totalPages;

  if (reset) {
    slot.replaceChildren();
  }

  slot.removeAttribute('aria-busy');

  // Khong co san pham phu hop.
  if (!items.length && reset) {
    stateBox(
      slot,
      'Không có sản phẩm phù hợp. Hãy thử đổi từ khóa hoặc bỏ bớt bộ lọc.'
    );

    line.textContent = 'Không tìm thấy sản phẩm nào.';
    more.hidden = true;
    return;
  }

  // Hien thi san pham.
  items.forEach((product) => {
    slot.append(card(product));
  });

  hydrateIcons(slot);

  // So san pham da hien thi.
  const displayed = slot.querySelectorAll(
    '.pcard'
  ).length;

  line.textContent =
    `Hiển thị ${displayed} / ${total} sản phẩm`;

  // Chi hien thi nut Xem them khi con trang.
  more.hidden =
    requestedPage + 1 >= totalPages ||
    items.length === 0;
}

  /* ---------- Đăng ký nhận ưu đãi ---------- */
  function bindSubscribe() {
    const f = $('[data-subscribe]'), msg = $('[data-subscribe-msg]'), inp = f.elements.email;
    f.addEventListener('submit', async (e) => {
      e.preventDefault(); msg.className = 'subscribe__msg'; inp.removeAttribute('aria-invalid');
      const email = inp.value.trim();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email)) { inp.setAttribute('aria-invalid', 'true'); msg.textContent = 'Vui lòng nhập email hợp lệ.'; msg.classList.add('is-error'); inp.focus(); return; }
      const btn = $('button', f); btn.disabled = true;
      const r = await api('newsletter', { method: 'POST', body: { email } });
      btn.disabled = false;
      if (r.ok) { msg.textContent = 'Đăng ký thành công! Cảm ơn bạn.'; msg.classList.add('is-ok'); f.reset(); }
      else if (r.status === 404 || r.status === 501) { msg.textContent = 'Chức năng đăng ký chưa được kết nối với máy chủ.'; msg.classList.add('is-error'); }
      else { msg.textContent = errText(r, r.status === 409 ? 'Email này đã đăng ký rồi.' : 'Không đăng ký được. Vui lòng thử lại.'); msg.classList.add('is-error'); }
    });
  }

  /* ---------- Chatbox AI (giao diện; chưa tích hợp AI) ---------- */
  // Hook cho giai đoạn sau: window.SNEAKER_CHAT = { onSend: async (text) => 'câu trả lời' }
  function bindChat() {
    const root = $('[data-chat]'), fab = $('[data-chat-open]'), panel = $('.chat__panel', root), bodyEl = $('[data-chat-body]'), chips = $('[data-chat-chips]'), form = $('[data-chat-form]'), input = $('#chat-input');
    const say = (text, who = 'bot') => { bodyEl.append(h('div', { class: `msg msg--${who}` }, text)); bodyEl.scrollTop = bodyEl.scrollHeight; };
    const open = () => { panel.hidden = false; panel.classList.remove('is-min'); fab.hidden = true; fab.setAttribute('aria-expanded', 'true'); if (!bodyEl.children.length) say('Xin chào! Mình là trợ lý Sneaker House. Bạn muốn tìm giày theo ngân sách, thương hiệu hay mẫu mới về?'); input.focus(); };
    const close = () => { panel.hidden = true; fab.hidden = false; fab.setAttribute('aria-expanded', 'false'); fab.focus(); };
    fab.addEventListener('click', open); $('[data-chat-close]').addEventListener('click', close);
    $('[data-chat-min]').addEventListener('click', () => panel.classList.toggle('is-min'));
    panel.addEventListener('keydown', (e) => { if (e.key === 'Escape') close(); });
    const go = (href) => { say('Mình đưa bạn tới phần đó nhé.'); if (href.startsWith('#')) { document.querySelector(href).scrollIntoView({ behavior: 'smooth' }); } else location.href = href; };
    const quick = {
      'Tìm giày theo giá': () => { say('Bạn muốn khoảng giá nào?'); setChips(CFG.filterOptions.prices.map((p) => [p.label, () => { say(p.label, 'me'); filters.form.elements.price.value = String(CFG.filterOptions.prices.indexOf(p)); runSearch(true); go('#shop'); setChips(base); }])); },
      'Tìm theo hãng': () => { say('Bạn thích hãng nào?'); setChips(state.brands.slice(0, 6).map((b) => [b.name, () => { say(b.name, 'me'); filters.form.elements.brand.value = b.slug; runSearch(true); go('#shop'); setChips(base); }])); },
      'Giày mới về': () => go('#new'),
      'Tra cứu đơn hàng': () => go(linkFor('orders', 'orders.html')),
    };
    const setChips = (list) => chips.replaceChildren(...list.map(([t, fn]) => h('button', { type: 'button', onclick: fn }, t)));
    const base = Object.keys(quick).map((t) => [t, () => { say(t, 'me'); quick[t](); }]);
    setChips(base);
    form.addEventListener('submit', async (e) => {
      e.preventDefault(); const text = input.value.trim(); if (!text) return; input.value = ''; say(text, 'me');
      const hook = window.SNEAKER_CHAT && window.SNEAKER_CHAT.onSend;
      if (!hook) { say('Trợ lý AI đang được kết nối nên chưa trả lời tự do được. Bạn có thể dùng các gợi ý bên dưới hoặc ô tìm kiếm ở đầu trang.'); return; }
      try { say(String(await hook(text))); } catch (_) { say('Mình chưa trả lời được lúc này. Vui lòng thử lại sau.'); }
    });
  }

  /* ---------- Khởi tạo ---------- */
  function applyStore() {
    const s = CFG.store || {};
    $$('[data-hotline]').forEach((e) => { if (s.hotline) e.textContent = s.hotline; });
    $$('[data-store="address"]').forEach((e) => { if (s.address) e.textContent = s.address; });
    $$('[data-store="hotline"]').forEach((e) => { if (s.hotline) { e.textContent = s.hotline; e.href = `tel:${s.hotline.replace(/\D/g, '')}`; } });
    $$('[data-store="email"]').forEach((e) => { if (s.email) { e.textContent = s.email; e.href = `mailto:${s.email}`; } });
    $$('[data-link]').forEach((a) => { const u = CFG.links[a.dataset.link]; if (u) a.setAttribute('href', u); });
  }
  function init() {
    hydrateIcons(); applyStore(); bindMenu(); bindMega(); bindSubscribe(); bindChat(); buildFilters();
    if (MOCK) document.body.append(h('div', { class: 'mock-badge', role: 'note' }, 'Dữ liệu giả: sản phẩm, danh mục, banner, flash sale…'));
    loadUser();
    loadBanners(); loadCategories(); loadBrands(); loadFlash();
    loadSlot('featured', { key: 'featured', empty: 'Chưa có sản phẩm nổi bật.', render: (it) => it.slice(0, 8).map((p) => card(p)) });
    loadSlot('newest', { key: 'newest', empty: 'Chưa có sản phẩm mới.', render: (it) => it.slice(0, 8).map((p) => card(p)) });
    loadSlot('bestSellers', { key: 'bestSellers', empty: 'Chưa có sản phẩm bán chạy.', render: (it) => it.slice(0, 8).map((p) => card(p)) });
    loadCollections(); loadNews(); loadReviews();
    runSearch(true);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
