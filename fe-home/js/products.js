
/* Sneaker House - Trang danh muc san pham */
(() => {
  'use strict';

  const CFG = window.HOME_CONFIG || {};
  const base = (CFG.apiBase || 'http://localhost:8080/api')
    .replace(/\/+$/, '');

  const EP = CFG.endpoints || {};
  const $ = (s, root = document) => root.querySelector(s);

  const form = $('[data-catalog-filters]');
  const grid = $('[data-catalog-grid]');
  const message = $('[data-catalog-message]');
  const totalLabel = $('[data-catalog-total]');
  const pagination = $('[data-catalog-pagination]');

  if (!form || !grid || !window.SneakerProductCard) {
    console.error('[catalog] Thieu form, grid hoac product-card.js');
    return;
  }

  const query = new URLSearchParams(location.search);
  const categorySlug = query.get('category') || '';
  const keyword = query.get('keyword') || '';
  const brandSlug = query.get('brand') || '';
  const categoryNames = {
    nam: 'Giày nam',
    nu: 'Giày nữ',
    'tre-em': 'Giày trẻ em'
  };

  const categoryAliases = {
    nam: ['nam', 'giay-nam'],
    nu: ['nu', 'giay-nu'],
    'tre-em': ['tre-em', 'giay-tre-em']
  };

  const state = {
    categories: [],
    brands: [],
    categoryId: null,
    page: 0,
    totalPages: 0,
    busy: false,
    user: null,
    wished: new Set()
  };

  const money = value =>
    `${new Intl.NumberFormat('vi-VN').format(Number(value) || 0)}đ`;

  function h(tag, attrs = {}, ...children) {
    const el = document.createElement(tag);

    Object.entries(attrs || {}).forEach(([key, value]) => {
      if (value == null || value === false) return;

      if (key === 'class') el.className = value;
      else if (key.startsWith('on') && typeof value === 'function') {
        el.addEventListener(key.slice(2), value);
      } else {
        el.setAttribute(key, value === true ? '' : String(value));
      }
    });

    children.flat().forEach(child => {
      if (child == null || child === false) return;
      el.append(
        child instanceof Node ? child : document.createTextNode(String(child))
      );
    });

    return el;
  }

  const icons = {
    heart: '<path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z"/>',
    cartPlus: '<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2.2 1.6h9.5a2 2 0 0 0 2.1-1.6L23 6H6M14 8v4M12 10h4"/>'
  };

  // Icon dùng chung cho Header Sneaker House
  Object.assign(icons, {
    search: '<circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/>',
    truck: '<path d="M1 3h15v13H1zM16 8h4l3 3v5h-7z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/>',
    refresh: '<path d="M23 4v6h-6M1 20v-6h6"/><path d="M3.5 9a9 9 0 0 1 14.9-3.4L23 10M1 14l4.6 4.4A9 9 0 0 0 20.5 15"/>',
    shieldCheck: '<path d="M12 3 4 6v6c0 4.5 3.2 8.3 8 9 4.8-.7 8-4.5 8-9V6z"/><path d="m9 12 2 2 4-4"/>',
    pin: '<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>',
    package: '<path d="m7.5 4.3 9 5.2M21 8v8l-9 5-9-5V8l9-5z"/><path d="M3.3 7 12 12l8.7-5M12 22V12"/>',
    headset: '<path d="M3 14v-2a9 9 0 0 1 18 0v2"/><path d="M21 15a2 2 0 0 1-2 2h-1v-6h1a2 2 0 0 1 2 2zM3 15a2 2 0 0 0 2 2h1v-6H5a2 2 0 0 0-2 2zM21 17v1a3 3 0 0 1-3 3h-4"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
    cart: '<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2.2 1.6h9.5a2 2 0 0 0 2.1-1.6L23 6H6"/>',
    menu: '<path d="M3 6h18M3 12h18M3 18h18"/>',
    chevronDown: '<path d="m6 9 6 6 6-6"/>',
    chevronRight: '<path d="m9 6 6 6-6 6"/>',
    chevronLeft: '<path d="m15 6-6 6 6 6"/>',
    chat: '<path d="M21 15a2 2 0 0 1-2 2H8l-5 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>'
  });

  function hydrateHeaderIcons() {
    document.querySelectorAll('i[data-icon]').forEach(el => {
      if (el.querySelector('svg')) return;

      const name = el.dataset.icon;
      if (!icons[name]) return;

      const svg = icon(name);
      svg.setAttribute('aria-hidden', 'true');
      el.append(svg);
    });
  }

  function icon(name) {
    const svg = document.createElementNS(
      'http://www.w3.org/2000/svg', 'svg'
    );
    svg.setAttribute('class', 'icon');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('fill', 'none');
    svg.setAttribute('stroke', 'currentColor');
    svg.setAttribute('stroke-width', '2');
    svg.setAttribute('stroke-linecap', 'round');
    svg.setAttribute('stroke-linejoin', 'round');
    svg.innerHTML = icons[name] || '';
    return svg;
  }

  function stars(value) {
    const count = Math.max(0, Math.min(5, Math.round(Number(value) || 0)));
    return h('span', null, '★'.repeat(count) + '☆'.repeat(5 - count));
  }

  function listOf(data) {
    if (Array.isArray(data)) return data;
    return data?.content || data?.items || data?.data ||
      data?.products || data?.results || [];
  }

  async function request(key, params = {}, options = {}) {
    const endpoint = EP[key];
    if (!endpoint) throw new Error(`Chưa cấu hình API: ${key}`);

    const path = endpoint.replace(
      '{id}', encodeURIComponent(options.id ?? '')
    );

    const url = new URL(base + path, location.href);

    Object.entries(params).forEach(([key, value]) => {
      if (value !== '' && value != null) {
        url.searchParams.set(key, String(value));
      }
    });

    const response = await fetch(url, {
      credentials: 'include',
      headers: { Accept: 'application/json' }
    });

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      throw new Error(data?.message || `API trả về HTTP ${response.status}`);
    }

    return data;
  }

  function imageUrl(value) {
    if (typeof value !== 'string' || !value.trim()) return null;
    const path = value.trim();

    if (/^(data:|blob:|javascript:|\/\/)/i.test(path)) return null;

    try {
      if (path.startsWith('assets/')) return path;

      const url = /^https?:\/\//i.test(path)
        ? new URL(path)
        : new URL('/' + path.replace(/^\/+/, ''), base);

      return /^https?:$/.test(url.protocol) ? url.href : null;
    } catch (_) {
      return null;
    }
  }

  function norm(raw) {
    const price = Number(raw.originalPrice ?? raw.price ?? 0);
    const discount = Number(raw.salePrice ?? raw.discountPrice ?? 0);
    const sale = discount > 0 && discount < price ? discount : null;
    const stock = raw.stockQuantity ?? raw.stock;

    return {
      id: raw.id,
      slug: raw.slug,
      name: String(raw.name || raw.title || 'Sản phẩm'),
      brand: typeof raw.brand === 'object'
        ? raw.brand?.name || '' : raw.brand || '',
      price,
      sale,
      percent: sale ? Math.round((1 - sale / price) * 100) : 0,
      image: imageUrl(raw.imageUrl || raw.image || raw.thumbnail),
      inStock: stock == null
        ? raw.inStock !== false
        : Number(stock) > 0,
      stock: stock == null ? null : Number(stock),
      sold: Number(raw.soldCount ?? raw.sold ?? 0),
      rating: Number(raw.rating ?? raw.averageRating ?? 0),
      reviews: Number(raw.reviewCount ?? raw.reviews ?? 0)
    };
  }

  function productHref(product) {
    return (CFG.productUrl || 'product.html?id={id}')
      .replace('{id}', encodeURIComponent(product.id))
      .replace('{slug}', encodeURIComponent(product.slug || product.id));
  }

  function toast(text) {
    const el = $('[data-toast]');

    if (!el) {
      window.alert(text);
      return;
    }

    el.textContent = text;
    el.hidden = false;
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => { el.hidden = true; }, 3500);
  }

  async function mutate(key, method, body, id) {
    const endpoint = EP[key];
    if (!endpoint) throw new Error('Chức năng chưa được kết nối.');

    const csrf = await request('csrf');
    const url = new URL(
      base + endpoint.replace('{id}', encodeURIComponent(id ?? '')),
      location.href
    );

    const response = await fetch(url, {
      method,
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token
      },
      ...(body == null ? {} : { body: JSON.stringify(body) })
    });

    if (!response.ok) {
      const data = await response.json().catch(() => null);
      throw new Error(data?.message || `HTTP ${response.status}`);
    }
  }

  async function toggleWish(product, button) {
    if (!state.user) {
      toast('Vui lòng đăng nhập để sử dụng yêu thích.');
      return;
    }

    const selected = state.wished.has(product.id);

    try {
      await mutate(
        'wishlist',
        selected ? 'DELETE' : 'POST',
        null,
        product.id
      );

      if (selected) state.wished.delete(product.id);
      else state.wished.add(product.id);

      button.setAttribute('aria-pressed', String(!selected));
      toast(selected ? 'Đã bỏ yêu thích.' : 'Đã thêm vào yêu thích.');
    } catch (error) {
      toast(error.message);
    }
  }

  async function addToCart(product) {
    if (!state.user) {
      toast('Vui lòng đăng nhập để thêm vào giỏ hàng.');
      return;
    }

    try {
      await mutate('cart', 'POST', {
        productId: product.id,
        quantity: 1
      });
      toast('Đã thêm vào giỏ hàng.');
    } catch (error) {
      toast(error.message);
    }
  }

  function renderCard(raw) {
    return window.SneakerProductCard.render(raw, {
      h, icon, norm, money, stars, productHref,
      toggleWish, addToCart
    });
  }

  function flattenCategories(items) {
    const result = [];

    function visit(list) {
      list.forEach(category => {
        result.push(category);
        if (Array.isArray(category.children)) {
          visit(category.children);
        }
      });
    }

    visit(items);
    return result;
  }

  function findCategory() {
    const aliases = categoryAliases[categorySlug] || [categorySlug];
    const desiredName = categoryNames[categorySlug];

    return state.categories.find(category => {
      const slug = String(category.slug || '').toLowerCase();
      const name = String(category.name || '').toLowerCase();

      return aliases.includes(slug) ||
        (desiredName && name === desiredName.toLowerCase());
    });
  }

  function addOptions(select, values, getValue, getLabel) {
    if (!select) return;

    values.forEach((value, index) => {
  select.add(new Option(
    getLabel(value, index),
    getValue(value, index)
  ));
});
  }

  async function loadFilters() {
    const results = await Promise.allSettled([
      request('categories'),
      request('brands')
    ]);

    if (results[0].status === 'fulfilled') {
      state.categories = flattenCategories(listOf(results[0].value));
    } else if (categorySlug) {
      throw new Error('Không tải được danh mục từ Backend.');
    }

    if (results[1].status === 'fulfilled') {
      state.brands = listOf(results[1].value);
    }

    if (categorySlug) {
      const category = findCategory();

      if (!category) {
        throw new Error(
          `Không tìm thấy danh mục "${categorySlug}" trong dữ liệu Backend.`
        );
      }

      state.categoryId = category.id;
      const label = category.name || categoryNames[categorySlug];

      $('[data-catalog-title]').textContent = label.toUpperCase();
      $('[data-catalog-breadcrumb]').textContent = label;
      document.title = `${label} | Sneaker House`;
    } else {
      const label = keyword ? 'Kết quả tìm kiếm' : 'Tất cả sản phẩm';
      $('[data-catalog-title]').textContent = label.toUpperCase();
      $('[data-catalog-breadcrumb]').textContent = label;
      document.title = `${label} | Sneaker House`;
    }

    const options = CFG.filterOptions || {};

    addOptions(
      form.elements.size, options.sizes || [],
      value => String(value), value => String(value)
    );

    addOptions(
      form.elements.color, options.colors || [],
      value => value.name || value,
      value => value.name || value
    );

    addOptions(
      form.elements.brand, state.brands,
      value => value.slug || String(value.id),
      value => value.name
    );

    // Nhan thuong hieu tu URL khi click o trang chu
    if (brandSlug) {
      const brand = state.brands.find(
        b => String(b.slug || b.id).toLowerCase() ===
             brandSlug.toLowerCase()
      );

      if (!brand) {
        throw new Error(
          `Không tìm thấy thương hiệu "${brandSlug}".`
        );
      }

      // Tu dong chon thuong hieu trong bo loc
      form.elements.brand.value = String(brand.slug || brand.id);

      // Cap nhat tieu de neu khong chon danh muc
      if (!categorySlug) {
        const title = `Giày ${brand.name}`;

        $('[data-catalog-title]').textContent =
          title.toUpperCase();

        $('[data-catalog-breadcrumb]').textContent = title;

        document.title = `${title} | Sneaker House`;
      }
    }

    addOptions(
      form.elements.price, options.prices || [],
      (_, i) => String(i),
      value => value.label
    );
  }

  function currentParams() {
    const selectedBrand = state.brands.find(
      b => String(b.slug || b.id) === form.elements.brand.value
    );

    const prices = CFG.filterOptions?.prices || [];
    const priceIndex = form.elements.price.value;
    const price = priceIndex === ''
      ? null : prices[Number(priceIndex)];

    return {
      keyword,
      categoryId: state.categoryId,
      brandId: selectedBrand?.id,
      minPrice: price?.min ?? '',
      maxPrice: price?.max > 0 ? price.max : '',
      size: form.elements.size.value,
      color: form.elements.color.value,
      sort: form.elements.sort.value || 'newest',
      page: state.page,
      sizePerPage: CFG.filterOptions?.pageSize || 12
    };
  }

  function updatePagination() {
    pagination.hidden = state.totalPages <= 1;

    $('[data-catalog-page]').textContent =
      `Trang ${state.page + 1} / ${state.totalPages}`;

    $('[data-catalog-prev]').disabled = state.page === 0;
    $('[data-catalog-next]').disabled =
      state.page >= state.totalPages - 1;
  }

  async function loadProducts() {
    if (state.busy) return;

    state.busy = true;
    message.textContent = 'Đang tải sản phẩm...';
    totalLabel.textContent = 'Đang tải...';
    grid.replaceChildren();
    pagination.hidden = true;

    try {
      const data = await request('products', currentParams());
      const items = listOf(data);
      const total = Number(data.totalElements ?? items.length);

      state.totalPages = Number(data.totalPages ?? 1);

      grid.replaceChildren(...items.map(renderCard));

      message.textContent = items.length
        ? `Hiển thị ${items.length} sản phẩm trên trang này.`
        : 'Không có sản phẩm phù hợp.';

      totalLabel.textContent = `${total} sản phẩm`;

      updatePagination();
    } catch (error) {
      message.textContent = 'Không tải được sản phẩm.';
      totalLabel.textContent = 'Không tải được dữ liệu';

      grid.replaceChildren(
        h('div', { class: 'state state--error', role: 'alert' },
          h('p', null, error.message),
          h('button', {
            class: 'btn',
            type: 'button',
            onclick: () => loadProducts()
          }, 'Thử lại')
        )
      );
    } finally {
      state.busy = false;
    }
  }

  function bindEvents() {
    form.addEventListener('submit', event => {
      event.preventDefault();
      state.page = 0;
      loadProducts();
    });

    $('[data-catalog-reset]').addEventListener('click', () => {
      form.reset();
      state.page = 0;
      loadProducts();
    });

    $('[data-catalog-prev]').addEventListener('click', () => {
      if (state.page > 0) {
        state.page--;
        loadProducts();
      }
    });

    $('[data-catalog-next]').addEventListener('click', () => {
      if (state.page + 1 < state.totalPages) {
        state.page++;
        loadProducts();
      }
    });

    // Tim kiem tren Header: mo trang danh sach san pham
    const searchForm = $('[data-search]');
    searchForm?.addEventListener('submit', event => {
      event.preventDefault();

      const term = $('#q')?.value.trim();
      if (!term) return;

      location.href =
        `products.html?keyword=${encodeURIComponent(term)}`;
    });

    // Dung lai cac muc Header da duyet
    const links = {
      men: 'nam',
      women: 'nu',
      kids: 'tre-em'
    };
    Object.entries(links).forEach(([key, slug]) => {
      document.querySelectorAll(`[data-link="${key}"]`)
        .forEach(link => {
          link.href =
            `products.html?category=${encodeURIComponent(slug)}`;
        });
    });
// Danh dau menu dang duoc chon
document.querySelectorAll('.mainnav a').forEach(link => {
  link.removeAttribute('aria-current');
});

const activeKey = Object.keys(links).find(
  key => links[key] === categorySlug
);

if (activeKey) {
  document.querySelector(
    `.mainnav a[data-link="${activeKey}"]`
  )?.setAttribute('aria-current', 'page');
}
}

/* ===== MENU TAI KHOAN ===== */

function renderAccountMenu() {
  const list = $('[data-account-list]');
  const name = $('[data-user-name]');

  if (!list || !name) return;

  const links = CFG.links || {};

  const item = (href, label) =>
    h('li', null, h('a', { href }, label));

  list.replaceChildren();

  if (state.user && state.user.email) {
    name.textContent = (
      state.user.fullName ||
      state.user.email
    ).split(' ').slice(-2).join(' ');

    list.append(
      item(
        links.profile || '../fe-account/profile.html',
        'Thông tin cá nhân'
      ),
      item(
        links.security || '../fe-account/account-security.html',
        'Bảo mật tài khoản'
      ),
      item(
        links.orders || 'orders.html',
        'Đơn hàng của tôi'
      ),
      h('li', null,
        h('button', {
          type: 'button',
          onclick: async () => {
            try {
              await mutate('logout', 'POST');
              state.user = null;
              renderAccountMenu();
              $('[data-account-list]').hidden = true;
              $('[data-menu-btn]').setAttribute(
                'aria-expanded', 'false'
              );
              toast('Đã đăng xuất.');
            } catch (error) {
              toast(error.message);
            }
          }
        }, 'Đăng xuất')
      )
    );
  } else {
    name.textContent = 'Tài khoản';

    const loginUrl = CFG.loginUrl || '../fe-auth/login.html';

    list.append(
      item(loginUrl, 'Đăng nhập'),
      item(
        loginUrl.replace('login.html', 'register.html'),
        'Đăng ký'
      )
    );
  }
}

function bindAccountMenu() {
  const wrap = $('[data-menu]');
  const btn = $('[data-menu-btn]');
  const list = $('[data-account-list]');

  if (!wrap || !btn || !list) return;

  const setOpen = (open) => {
    list.hidden = !open;
    btn.setAttribute('aria-expanded', String(open));
  };

  btn.addEventListener('click', () => {
    setOpen(list.hidden);
  });

  document.addEventListener('click', (event) => {
    if (!wrap.contains(event.target)) {
      setOpen(false);
    }
  });

  wrap.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      setOpen(false);
      btn.focus();
    }
  });
}
async function init() {
  hydrateHeaderIcons();
  bindEvents();
  bindAccountMenu();

  try {
    await loadFilters();
    await loadProducts();
    } catch (error) {
      totalLabel.textContent = 'Không tải được danh mục';
      message.textContent = error.message;
    }

    try {
      state.user = await request('profile');
    } catch (_) {
      state.user = null;
    }
    renderAccountMenu();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
