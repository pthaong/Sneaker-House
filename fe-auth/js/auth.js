/* =========================================================
   Sneaker House — Auth module (FE1)
   JS thuần, không phụ thuộc thư viện.

   CẤU HÌNH: sửa trong js/auth-config.js (window.AUTH_CONFIG), nạp trước file này:

     window.AUTH_CONFIG = {
       mock: true,                       // true = dữ liệu giả, không gọi backend
       baseUrl: 'http://localhost:8080/api/auth',
       googleClientId: '...apps.googleusercontent.com',
       endpoints: { login: '/login', register: '/register', forgotPassword: '/forgot-password', google: '/google' }
     };

   Body gửi đi (JSON):
     login          { identifier, password, rememberMe }
     register       { fullName, email, phone, password }   // phone luôn dạng 0xxxxxxxxx
     forgotPassword { email }
     google         { idToken }          // Google ID Token (JWT) lấy từ Google Identity Services
   Lỗi trả về nên có dạng { message: "...", errors?: { tenTruong: "..." } }.
   Nếu BE1 dùng tên trường khác, sửa ở hàm buildPayload().
   ========================================================= */
(() => {
  'use strict';

  const CONFIG = Object.assign(
    {
      baseUrl: '/api/auth',
      endpoints: {
  login: '/login',
  register: '/register',
  forgotPassword: '/forgot-password',
  verifyOtp: '/verify-otp',
  resendOtp: '/resend-otp',
  resetPassword: '/reset-password',
  google: '/google'
},
      redirectAfterLogin: 'index.html',
      productsUrl: 'products.html', // trang danh sách sản phẩm (module khác) — nút trên carousel trỏ tới đây
      googleClientId: '',           // Google OAuth Client ID (công khai). Không bao giờ đặt Client Secret ở FE.
      mock: false,                  // true = không gọi backend, dùng dữ liệu giả
    },
    window.AUTH_CONFIG || {}
  );
  CONFIG.endpoints = Object.assign(
  {
    login: '/login',
    register: '/register',
    forgotPassword: '/forgot-password',
    verifyOtp: '/verify-otp',
    resendOtp: '/resend-otp',
    resetPassword: '/reset-password',
    google: '/google'
  },
  CONFIG.endpoints
);

  // Mock chỉ được chạy ở máy dev. Triển khai lên tên miền thật mà quên đặt mock:false vẫn gọi API thật.
  const IS_LOCAL = ['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname);
  if (CONFIG.mock && !IS_LOCAL) {
    console.warn('[auth] mock:true bị bỏ qua vì trang không chạy ở localhost — đang gọi API thật.');
    CONFIG.mock = false;
  }

  /* Hook để BE1/FE khác xử lý token hoặc phiên đăng nhập theo cách của dự án.
     Không lưu mật khẩu ở bất kỳ đâu. Việc lưu token (nếu có) do hook quyết định. */
  const hooks = Object.assign({ onLoginSuccess: null }, window.AUTH_HOOKS || {});

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

  /* ---------- Icon (nét 2px, cùng một bộ) ---------- */
  const ICONS = {
    eye: '<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
    eyeOff: '<path d="M17.94 17.94A10.4 10.4 0 0 1 12 19c-6.5 0-10-7-10-7a17.7 17.7 0 0 1 4.06-5.06M9.9 4.24A9.9 9.9 0 0 1 12 4c6.5 0 10 7 10 7a17.8 17.8 0 0 1-2.16 3.19M1 1l22 22"/><path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/>',
    alert: '<circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>',
    check: '<path d="M20 6 9 17l-5-5"/>',
    pause: '<path d="M8 5v14M16 5v14"/>',
    play: '<path d="M7 4v16l13-8z"/>',
    left: '<path d="M15 18l-6-6 6-6"/>',
    right: '<path d="M9 18l6-6-6-6"/>',
    arrow: '<path d="M5 12h14M13 6l6 6-6 6"/>',
    circle: '<circle cx="12" cy="12" r="9"/>',
    mail: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
    phone: '<path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 1.9.7 2.8a2 2 0 0 1-.5 2.1L8.1 9.9a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.8.7a2 2 0 0 1 1.7 2z"/>',
    lock: '<rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
    back: '<path d="M19 12H5M11 6l-6 6 6 6"/>',
  };
  const icon = (name) =>
    `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">${ICONS[name]}</svg>`;

  document.querySelectorAll('[data-icon]').forEach((el) => {
    el.innerHTML = icon(el.dataset.icon);
  });

  /* ---------- Carousel sneaker ---------- */
  // Thêm/đổi slide: sửa mảng này. Mỗi slide cần 2 file trong assets/: <key>-720.webp và <key>-1122.webp
  // (cùng tỉ lệ 4:5). Tên và mô tả viết theo ảnh — sửa lại cho đúng với sản phẩm thật nếu cần.
  const IMG_W = 1122;
  const IMG_H = 1402;
  const SLIDES = [
    { key: 'slide-1-samba', brand: 'Adidas', name: 'Samba',
      desc: 'Dáng sân cỏ kinh điển: da trắng, ba sọc đen, đế gum.',
      alt: 'Đôi giày Adidas Samba màu trắng, ba sọc đen, đế gum, đặt trên bục đá tối' },
    { key: 'slide-2-balenciaga', brand: 'Balenciaga', name: 'Triple S',
      desc: 'Tông đen toàn thân, đế nhiều lớp: nổi bật mà vẫn kín đáo.',
      alt: 'Đôi giày Balenciaga màu đen toàn thân, đế dày nhiều lớp, đặt trên bục đá tối' },
    { key: 'slide-3-air-force-1', brand: 'Nike', name: 'Air Force 1',
      desc: 'Da trắng gọn gàng, điểm nhấn xám, dễ phối với mọi outfit.',
      alt: 'Đôi giày Nike Air Force 1 màu trắng, logo xám, đặt trên bục đá tối' },
    { key: 'slide-4-jordan', brand: 'Nike', name: 'Air Jordan 1 Mid',
      desc: 'Cổ lửng phối đen, trắng, xám: phong cách bóng rổ đường phố.',
      alt: 'Đôi giày Nike Air Jordan 1 Mid phối đen, trắng và xám, đặt trên bục đá tối' },
  ];
  const imgSrc = (s, w) => `assets/${s.key}-${w}.webp`;

  function initCarousel(root) {
    const slides = SLIDES;
    const total = slides.length;
    const pad = (n) => String(n).padStart(2, '0');

    root.innerHTML = `
      <div class="carousel" role="region" aria-roledescription="carousel" aria-label="Giày sneaker nổi bật">
        <div data-track aria-live="off">
          ${slides
            .map(
              (s, i) => `
            <div class="slide" role="group" aria-roledescription="slide" aria-label="${i + 1} trên ${total}: ${s.brand} ${s.name}" style="--ratio:${(IMG_W / IMG_H).toFixed(4)}">
              <figure class="slide__frame">
                <img src="${imgSrc(s, IMG_W)}" srcset="${imgSrc(s, 720)} 720w, ${imgSrc(s, IMG_W)} ${IMG_W}w"
                     sizes="(min-width: 960px) 44vw, 100vw" alt="${s.alt}" width="${IMG_W}" height="${IMG_H}" decoding="async" draggable="false">
                <figcaption class="slide__caption">
                  <span class="slide__brand">${s.brand}</span>
                  <p class="slide__name">${s.name}</p>
                  <p class="slide__desc">${s.desc}</p>
                  <a class="slide__cta" href="${CONFIG.productsUrl}" aria-label="Xem sản phẩm ${s.brand} ${s.name}">Xem sản phẩm ${icon('arrow')}</a>
                </figcaption>
              </figure>
            </div>`
            )
            .join('')}
        </div>
        <div class="carousel__controls">
          <div class="carousel__buttons">
            <button type="button" class="carousel__btn" data-prev aria-label="Slide trước">${icon('left')}</button>
            <button type="button" class="carousel__btn" data-toggle aria-label="Tạm dừng tự chuyển slide">${icon('pause')}</button>
            <button type="button" class="carousel__btn" data-next aria-label="Slide kế tiếp">${icon('right')}</button>
          </div>
          <div class="carousel__dots">
            ${slides
              .map(
                (s, i) =>
                  `<button type="button" class="carousel__dot" data-dot="${i}" aria-label="Chuyển tới slide ${i + 1}: ${s.brand} ${s.name}"></button>`
              )
              .join('')}
          </div>
          <span class="carousel__count" aria-hidden="true"><span data-current>01</span> / ${pad(total)}</span>
        </div>
      </div>`;

    const els = root.querySelectorAll('.slide');
    const dots = root.querySelectorAll('[data-dot]');
    const track = root.querySelector('[data-track]');
    const toggleBtn = root.querySelector('[data-toggle]');
    const counter = root.querySelector('[data-current]');
    const INTERVAL = 6000;
    const SHIFT = 56; // px, độ trượt ngang khi chuyển slide

    let index = 0;
    let timer = null;
    let userPaused = reduceMotion.matches; // bật reduced-motion => không tự chạy
    let hovering = false;
    let focusing = false;

    // dir = 1: slide hiện tại trượt sang trái, slide mới đi vào từ bên phải. dir = -1: ngược lại.
    function show(next, dir = 1) {
      const prev = index;
      index = (next + total) % total;
      root.style.setProperty('--enter', `${dir >= 0 ? SHIFT : -SHIFT}px`);
      root.style.setProperty('--exit', `${dir >= 0 ? -SHIFT : SHIFT}px`);
      void root.offsetWidth; // chốt vị trí xuất phát trước khi chạy transition
      els.forEach((el, i) => {
        const active = i === index;
        el.classList.toggle('is-leaving', i === prev && prev !== index);
        el.classList.toggle('is-active', active);
        el.setAttribute('aria-hidden', active ? 'false' : 'true');
        el.toggleAttribute('inert', !active); // nút/link của slide ẩn không nhận focus
      });
      dots.forEach((d, i) => (i === index ? d.setAttribute('aria-current', 'true') : d.removeAttribute('aria-current')));
      counter.textContent = pad(index + 1);
    }

    function stop() {
      clearInterval(timer);
      timer = null;
    }
    function start() {
      stop();
      if (userPaused || hovering || focusing || reduceMotion.matches || document.hidden) return;
      timer = setInterval(() => show(index + 1, 1), INTERVAL);
    }
    function syncToggle() {
      toggleBtn.innerHTML = icon(userPaused ? 'play' : 'pause');
      toggleBtn.setAttribute('aria-label', userPaused ? 'Tiếp tục tự chuyển slide' : 'Tạm dừng tự chuyển slide');
      // Khi tự chạy: không đọc lại slide; khi dừng: đọc khi đổi slide thủ công
      track.setAttribute('aria-live', userPaused ? 'polite' : 'off');
    }

    root.querySelector('[data-prev]').addEventListener('click', () => { show(index - 1, -1); start(); });
    root.querySelector('[data-next]').addEventListener('click', () => { show(index + 1, 1); start(); });
    dots.forEach((d) =>
      d.addEventListener('click', () => {
        const to = Number(d.dataset.dot);
        show(to, to >= index ? 1 : -1);
        start();
      })
    );
    toggleBtn.addEventListener('click', () => {
      userPaused = !userPaused;
      syncToggle();
      start();
    });

    root.addEventListener('mouseenter', () => { hovering = true; stop(); });
    root.addEventListener('mouseleave', () => { hovering = false; start(); });
    root.addEventListener('focusin', () => { focusing = true; stop(); });
    root.addEventListener('focusout', () => { focusing = false; start(); });
    document.addEventListener('visibilitychange', start);
    reduceMotion.addEventListener('change', (e) => {
      if (e.matches) userPaused = true;
      syncToggle();
      start();
    });

    show(0);
    syncToggle();
    start();
  }

  const carouselRoot = document.querySelector('[data-carousel]');
  if (carouselRoot) initCarousel(carouselRoot);

  /* ---------- Chuyển trang mượt (fade nhẹ) ---------- */
  document.querySelectorAll('a[data-nav]').forEach((a) => {
    a.addEventListener('click', (e) => {
      if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
      if (reduceMotion.matches) return;
      e.preventDefault();
      document.body.classList.add('is-leaving');
      setTimeout(() => { window.location.href = a.href; }, 180);
    });
  });
  // Quay lại bằng nút Back (bfcache) không bị kẹt ở trạng thái mờ
  window.addEventListener('pageshow', () => document.body.classList.remove('is-leaving'));

  /* ---------- Hiện/ẩn mật khẩu ---------- */
  document.querySelectorAll('[data-toggle-pw]').forEach((btn) => {
    const input = document.getElementById(btn.dataset.togglePw);
    const render = () => {
      const shown = input.type === 'text';
      btn.innerHTML = icon(shown ? 'eyeOff' : 'eye');
      btn.setAttribute('aria-label', shown ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
      btn.setAttribute('aria-pressed', String(shown));
    };
    btn.addEventListener('click', () => {
      input.type = input.type === 'password' ? 'text' : 'password';
      render();
    });
    render();

    // Cảnh báo Caps Lock
    const hint = document.createElement('p');
    hint.className = 'field__hint field__hint--warn';
    hint.id = `${input.id}-caps`;
    hint.hidden = true;
    hint.setAttribute('role', 'status');
    hint.innerHTML = `${icon('alert')}<span>Caps Lock đang bật.</span>`;
    input.closest('.control').after(hint);
    const syncCaps = (e) => { hint.hidden = !(e.getModifierState && e.getModifierState('CapsLock')); };
    input.addEventListener('keydown', syncCaps);
    input.addEventListener('keyup', syncCaps);
    input.addEventListener('blur', () => { hint.hidden = true; });
  });

  /* ---------- Validation ---------- */
  const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  // Di động Việt Nam: 0xxxxxxxxx hoặc +84xxxxxxxxx / 84xxxxxxxxx (đầu 3, 5, 7, 8, 9)
  const PHONE_RE = /^(?:\+?84|0)[35789]\d{8}$/;
  const normalizePhone = (v) => v.replace(/[\s.\-()]/g, '');
  // Dạng gửi cho BE: luôn 0xxxxxxxxx (đổi +84 / 84 ở đầu thành 0) để cùng một số không bị lưu nhiều dạng
  const toLocalPhone = (v) => normalizePhone(v).replace(/^\+?84/, '0');
  const PW_RULES = {
    len: (v) => v.length >= 8,
    upper: (v) => /[A-Z]/.test(v),
    lower: (v) => /[a-z]/.test(v),
    digit: (v) => /\d/.test(v),
  };

  const validators = {
    otp: (v) => {
  if (!v.trim()) return 'Vui lòng nhập mã OTP.';
  return /^[0-9]{6}$/.test(v.trim())
    ? ''
    : 'Mã OTP phải gồm đúng 6 chữ số.';
},

newPassword: (v) => {
  if (!v) return 'Vui lòng nhập mật khẩu mới.';
  if (v.length < 8 || v.length > 72) {
    return 'Mật khẩu phải có từ 8 đến 72 ký tự.';
  }
  return '';
},
    identifier: (v) => (v.trim() ? '' : 'Vui lòng nhập email hoặc tên đăng nhập.'),
    fullName: (v) => {
      if (!v.trim()) return 'Vui lòng nhập họ và tên.';
      return v.trim().length < 2 ? 'Họ và tên cần ít nhất 2 ký tự.' : '';
    },
    email: (v) => {
      if (!v.trim()) return 'Vui lòng nhập email.';
      return EMAIL_RE.test(v.trim()) ? '' : 'Email chưa đúng định dạng, ví dụ ten@gmail.com.';
    },
    phone: (v) => {
      if (!v.trim()) return 'Vui lòng nhập số điện thoại.';
      return PHONE_RE.test(normalizePhone(v)) ? '' : 'Số điện thoại chưa đúng, ví dụ 0912345678 hoặc +84912345678.';
    },
    password: (v, form) => {
      if (!v) return 'Vui lòng nhập mật khẩu.';
      if (form.dataset.form !== 'register') return '';
      return Object.values(PW_RULES).every((fn) => fn(v))
        ? ''
        : 'Mật khẩu chưa đủ mạnh: cần ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số.';
    },
    terms: (_v, _form, input) => (input.checked ? '' : 'Vui lòng đồng ý với Điều khoản sử dụng và Chính sách bảo mật.'),
    confirmPassword: (v, form) => {
  if (!v) return 'Vui lòng nhập lại mật khẩu.';

  const originalPassword =
    form.elements.newPassword || form.elements.password;

  if (!originalPassword) {
    return 'Không tìm thấy trường mật khẩu.';
  }

  return v === originalPassword.value
    ? ''
    : 'Mật khẩu nhập lại chưa khớp.';
},
  };

  const errorId = (input) => `${input.id}-error`;

  function setFieldError(input, message) {
    const err = document.getElementById(errorId(input));
    if (message) {
      input.setAttribute('aria-invalid', 'true');
      err.innerHTML = `${icon('alert')}<span></span>`;
      err.lastChild.textContent = message;
      err.hidden = false;
    } else {
      input.removeAttribute('aria-invalid');
      err.hidden = true;
      err.textContent = '';
    }
  }

  function validateField(input, form) {
    const fn = validators[input.name];
    if (!fn) return '';
    const message = fn(input.value, form, input);
    setFieldError(input, message);
    return message;
  }

  function renderSummary(form, items) {
    const box = form.querySelector('[data-summary]');
    if (!box) return;
    if (!items.length) {
      box.hidden = true;
      box.querySelector('ul').innerHTML = '';
      return;
    }
    const list = box.querySelector('ul');
    list.innerHTML = '';
    items.forEach(({ input, message }) => {
      const li = document.createElement('li');
      const a = document.createElement('a');
      a.href = `#${input.id}`;
      a.textContent = message;
      a.addEventListener('click', (e) => { e.preventDefault(); input.focus(); });
      li.appendChild(a);
      list.appendChild(li);
    });
    box.hidden = false;
  }

  function setNotice(form, type, message) {
    const box = form.querySelector('[data-notice]');
    if (!box) return;
    if (!message) { box.hidden = true; box.textContent = ''; return; }
    box.className = `notice notice--${type}`;
    box.setAttribute('role', type === 'error' ? 'alert' : 'status');
    box.innerHTML = `${icon(type === 'success' ? 'check' : type === 'error' ? 'alert' : 'alert')}<span></span>`;
    box.lastChild.textContent = message;
    box.hidden = false;
  }

  function setLoading(form, loading) {
    const btn = form.querySelector('[type="submit"]');
    const label = btn.querySelector('[data-label]');
    const spinner = btn.querySelector('.spinner');
    btn.disabled = loading;
    btn.setAttribute('aria-busy', String(loading));
    spinner.hidden = !loading;
    if (!btn.dataset.idle) btn.dataset.idle = label.textContent;
    label.textContent = loading ? btn.dataset.loading || 'Đang xử lý…' : btn.dataset.idle;
  }

  /* ---------- Độ mạnh mật khẩu (trang đăng ký) ---------- */
  function initStrength(form) {
    const pw = form.elements.password;
    const meter = form.querySelector('[data-strength]');
    if (!pw || !meter) return;
    const label = meter.querySelector('.strength__label');
    const reqs = form.querySelectorAll('[data-req]');
    const names = ['', 'Yếu', 'Yếu', 'Trung bình', 'Mạnh'];

    const update = () => {
      const v = pw.value;
      let met = 0;
      reqs.forEach((li) => {
        const ok = PW_RULES[li.dataset.req](v);
        li.dataset.met = String(ok);
        li.querySelector('.sr-only').textContent = ok ? 'Đã đạt: ' : 'Chưa đạt: ';
        li.querySelector('[data-req-icon]').innerHTML = icon(ok ? 'check' : 'circle');
        if (ok) met += 1;
      });
      meter.hidden = v.length === 0;
      meter.dataset.level = String(met);
      label.textContent = met ? `Độ mạnh mật khẩu: ${names[met]}` : '';
    };
    pw.addEventListener('input', update);
    update();
  }

  /* ---------- Mock backend (CONFIG.mock = true) ----------
     Để FE làm độc lập khi BE1 chưa xong. Trả về đúng "hình dạng" kết quả của request().
       login     demo@sneakerhouse.vn / Demo@1234 => thành công, còn lại => 401
       register  taken@sneakerhouse.vn => 409 (email đã tồn tại), còn lại => thành công
       forgot    fail@sneakerhouse.vn => 500, còn lại => thành công
       google    nút giả; thêm ?mockGoogle=conflict | invalid | network vào URL để thử các lỗi */
  const MOCK_DELAY = 700;
  function mockRequest(path, body) {
    const E = CONFIG.endpoints;
    const reply = (status, data, network = false) =>
      new Promise((resolve) =>
        setTimeout(() => resolve({ ok: !network && status >= 200 && status < 300, network, status: network ? 0 : status, data }), MOCK_DELAY)
      );
    const user = { id: 1, fullName: 'Khách Demo', email: 'demo@sneakerhouse.vn', role: 'CUSTOMER' };

    if (path === E.login) {
      const ok = String(body.identifier).toLowerCase() === 'demo@sneakerhouse.vn' && body.password === 'Demo@1234';
      return ok ? reply(200, { user }) : reply(401, { message: 'Email/tên đăng nhập hoặc mật khẩu không đúng.' });
    }
    if (path === E.register) {
      return String(body.email).toLowerCase() === 'taken@sneakerhouse.vn'
        ? reply(409, { message: 'Email này đã được đăng ký.' })
        : reply(201, { message: 'Created' });
    }
    if (path === E.forgotPassword) {
      return String(body.email).toLowerCase() === 'fail@sneakerhouse.vn'
        ? reply(500, { message: 'Máy chủ gặp lỗi. Vui lòng thử lại sau.' })
        : reply(200, { message: 'OK' });
    }
    if (path === E.google) {
      if (body.idToken === 'mock-network') return reply(0, null, true);
      if (body.idToken === 'mock-conflict') return reply(409, { message: 'Email này đã đăng ký bằng mật khẩu. Hãy đăng nhập bằng email và mật khẩu.' });
      if (body.idToken === 'mock-invalid') return reply(401, { message: 'Không xác minh được tài khoản Google.' });
      return reply(200, { user, isNewUser: false });
    }
    return reply(404, { message: 'Mock: endpoint không tồn tại.' });
  }

  if (CONFIG.mock) {
    console.info('[auth] Đang ở chế độ MOCK — không gọi backend. Đặt mock:false trong js/auth-config.js để dùng API thật.');
    const badge = document.createElement('p');
    badge.className = 'mock-badge';
    badge.textContent = 'Chế độ mock: chưa gọi backend';
    document.body.appendChild(badge);
  }

  /* ---------- Gọi API ---------- */
  /* ---------- Gọi API Backend có CSRF ---------- */
async function request(path, body) {
  if (CONFIG.mock) return mockRequest(path, body);

  try {
    // Bước 1: Lấy CSRF Token từ Backend
    const csrfResponse = await fetch(CONFIG.baseUrl + '/csrf', {
      method: 'GET',
      credentials: 'include',
      headers: {
        Accept: 'application/json'
      }
    });

    if (!csrfResponse.ok) {
      return {
        ok: false,
        network: false,
        status: csrfResponse.status,
        data: { message: 'Không lấy được CSRF Token từ Backend.' }
      };
    }

    const csrfData = await csrfResponse.json();

    // Bước 2: Gửi yêu cầu POST kèm CSRF Token
    const response = await fetch(CONFIG.baseUrl + path, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        [csrfData.headerName || 'X-XSRF-TOKEN']: csrfData.token
      },
      body: JSON.stringify(body)
    });

    let data = null;

    try {
      data = await response.json();
    } catch (_) {
      // Backend có thể trả response không phải JSON
    }

    return {
      ok: response.ok,
      network: false,
      status: response.status,
      data
    };

  } catch (error) {
    console.error('[auth] API request failed:', error);

    return {
      ok: false,
      network: true,
      status: 0,
      data: null
    };
  }
}

  function buildPayload(type, form, credential) {
    const f = form.elements;
    if (type === 'login') {
  return {
    email: f.identifier.value.trim(),
    password: f.password.value
  };
}
    if (type === 'register') {
      // Việc đồng ý điều khoản (f.terms) chỉ kiểm tra ở FE; nếu BE cần lưu sự đồng ý, thêm trường tại đây.
      return { fullName: f.fullName.value.trim(), email: f.email.value.trim(), phone: toLocalPhone(f.phone.value), password: f.password.value };
    }
    if (type === 'google') return { idToken: credential }; // Google ID Token (JWT); BE xác minh chữ ký, issuer, audience, hạn dùng
    return { email: f.email.value.trim() };
  }

  const msgFrom = (r, fallback) => (r.data && typeof r.data.message === 'string' && r.data.message) || fallback;

  function applyServerFieldErrors(form, data) {
    if (!data || typeof data.errors !== 'object' || !data.errors) return false;
    const items = [];
    Object.entries(data.errors).forEach(([name, message]) => {
      const input = form.elements[name];
      if (input && typeof message === 'string') {
        setFieldError(input, message);
        items.push({ input, message });
      }
    });
    renderSummary(form, items);
    if (items.length) items[0].input.focus();
    return items.length > 0;
  }

  /* Thay form bằng màn hình trạng thái thành công. Trả về false nếu trang không có khung trạng thái. */
  function showStatus(form, { title, text }) {
    const box = document.querySelector('[data-status]');
    if (!box) return false;
    box.querySelector('[data-status-title]').textContent = title;
    box.querySelector('[data-status-text]').textContent = text; // textContent: an toàn với dữ liệu người dùng nhập
    const intro = document.querySelector('[data-intro]');
    if (intro) intro.hidden = true;
    form.hidden = true;
    box.hidden = false;
    box.querySelector('[data-status-title]').focus();
    return true;
  }

  document.querySelectorAll('[data-status-reset]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const form = document.querySelector('form[data-form]');
      document.querySelector('[data-status]').hidden = true;
      const intro = document.querySelector('[data-intro]');
      if (intro) intro.hidden = false;
      form.hidden = false;
      form.reset();
      form.querySelector('.input').focus();
    });
  });

  const handlers = {
    async login(form) {
      const r = await request(CONFIG.endpoints.login, buildPayload('login', form));
      if (r.ok) {
        setNotice(form, 'success', 'Đăng nhập thành công. Đang chuyển hướng…');
        if (typeof hooks.onLoginSuccess === 'function') hooks.onLoginSuccess(r.data);
        else setTimeout(() => { window.location.href = CONFIG.redirectAfterLogin; }, 700);
        return;
      }
      if (r.network) return setNotice(form, 'error', 'Không kết nối được tới máy chủ. Vui lòng thử lại sau.');
      if (applyServerFieldErrors(form, r.data)) return;
      if (r.status === 401 || r.status === 403) return setNotice(form, 'error', msgFrom(r, 'Email/tên đăng nhập hoặc mật khẩu không đúng.'));
      setNotice(form, 'error', msgFrom(r, 'Đăng nhập chưa thành công. Vui lòng thử lại.'));
    },

    async register(form) {
      const r = await request(CONFIG.endpoints.register, buildPayload('register', form));
      if (r.ok) {
        showStatus(form, {
          title: 'Tạo tài khoản thành công',
          text: `Tài khoản đã được tạo. Bạn có thể đăng nhập bằng email ${form.elements.email.value.trim()}.`,
        });
        return;
      }
      if (r.network) return setNotice(form, 'error', 'Không kết nối được tới máy chủ. Vui lòng thử lại sau.');
      if (applyServerFieldErrors(form, r.data)) return;
      if (r.status === 409) {
        setFieldError(form.elements.email, msgFrom(r, 'Email này đã được đăng ký.'));
        return form.elements.email.focus();
      }
      setNotice(form, 'error', msgFrom(r, 'Đăng ký chưa thành công. Vui lòng thử lại.'));
    },

    async google(form, credential) {
      const r = await request(CONFIG.endpoints.google, buildPayload('google', form, credential));
      if (r.ok) {
        setGoogleMessage(form, 'success', 'Xác thực Google thành công. Đang chuyển hướng…');
        if (typeof hooks.onLoginSuccess === 'function') hooks.onLoginSuccess(r.data, { method: 'google' });
        else setTimeout(() => { window.location.href = CONFIG.redirectAfterLogin; }, 700);
        return;
      }
      if (r.network) setGoogleMessage(form, 'error', 'Không kết nối được tới máy chủ. Vui lòng thử lại sau.');
      else if (r.status === 409) setGoogleMessage(form, 'error', msgFrom(r, 'Email Google này đã đăng ký bằng mật khẩu. Hãy đăng nhập bằng email và mật khẩu.'));
      else if (r.status === 400 || r.status === 401) setGoogleMessage(form, 'error', msgFrom(r, 'Không xác minh được tài khoản Google. Vui lòng thử lại.'));
      else if (r.status === 403) setGoogleMessage(form, 'error', msgFrom(r, 'Tài khoản này không được phép đăng nhập.'));
      else setGoogleMessage(form, 'error', msgFrom(r, 'Đăng nhập bằng Google chưa thành công. Vui lòng thử lại.'));
    },

    async forgot(form) {
      const r = await request(CONFIG.endpoints.forgotPassword, buildPayload('forgot', form));
      if (r.ok) {
  // Lưu email trong bộ nhớ để sử dụng khi xác minh OTP
  const email = form.elements.email.value.trim();

  window.forgotPasswordEmail = email;

  // Ẩn form nhập email
  form.hidden = true;

  // Hiển thị form nhập OTP
  const otpForm = document.querySelector('[data-otp-form]');

  if (otpForm) {
    otpForm.hidden = false;
    otpForm.elements.otp.focus();
  }

  // Cập nhật hướng dẫn
  const intro = document.querySelector('[data-intro]');

  if (intro) {
    intro.querySelector('.title').textContent = 'Xác minh OTP';
    intro.querySelector('.lead').textContent =
      'Nếu email có trong hệ thống, mã OTP đã được gửi. Vui lòng kiểm tra hộp thư và thư mục spam.';
  }

  return;
}
      if (r.network || r.status === 404 || r.status === 501) {
        return setNotice(form, 'info', 'Chức năng khôi phục mật khẩu hiện chưa được kết nối với máy chủ nên chưa thể gửi email. Vui lòng quay lại sau.');
      }
      setNotice(form, 'error', msgFrom(r, 'Chưa gửi được yêu cầu. Vui lòng thử lại.'));
},

async verifyOtp(form) {
  const email = window.forgotPasswordEmail;
  const otp = form.elements.otp.value.trim();

  if (!email) {
    return setNotice(form, 'error', 'Không tìm thấy email. Vui lòng nhập lại email.');
  }

  const r = await request(CONFIG.endpoints.verifyOtp, {
    email,
    otp
  });

  if (r.ok) {
    const resetToken = r.data && r.data.resetToken;

    if (!resetToken) {
      return setNotice(form, 'error', 'Máy chủ không trả về mã đặt lại mật khẩu.');
    }

    // Chỉ giữ token trong bộ nhớ của trang
    window.passwordResetToken = resetToken;

    // Ẩn form OTP
    form.hidden = true;

    // Hiển thị form mật khẩu mới
    const resetForm = document.querySelector('[data-reset-form]');

    if (resetForm) {
      resetForm.hidden = false;
      resetForm.elements.newPassword.focus();
    }

    // Cập nhật tiêu đề
    const intro = document.querySelector('[data-intro]');

    if (intro) {
      intro.querySelector('.title').textContent = 'Đặt lại mật khẩu';
      intro.querySelector('.lead').textContent =
        'Tạo mật khẩu mới có từ 8 đến 72 ký tự để bảo vệ tài khoản của bạn.';
    }

    return;
  }

  if (r.network) {
    return setNotice(form, 'error', 'Không kết nối được tới máy chủ. Vui lòng thử lại.');
  }

  if (applyServerFieldErrors(form, r.data)) return;

 setNotice(form, 'error', msgFrom(r, 'Mã OTP không hợp lệ hoặc đã hết hạn.'));
},
async resetPassword(form) {
  const token = window.passwordResetToken;
  const newPassword = form.elements.newPassword.value;

  if (!token) {
    return setNotice(
      form,
      'error',
      'Phiên đặt lại mật khẩu không hợp lệ. Vui lòng yêu cầu mã OTP mới.'
    );
  }

  const r = await request(CONFIG.endpoints.resetPassword, {
    token,
    newPassword
  });

  if (r.ok) {
    // Xóa token và email khỏi bộ nhớ sau khi đổi mật khẩu
    delete window.passwordResetToken;
    delete window.forgotPasswordEmail;

    form.reset();

    showStatus(form, {
      title: 'Đổi mật khẩu thành công',
      text: 'Mật khẩu của bạn đã được cập nhật. Hãy đăng nhập bằng mật khẩu mới.'
    });

    return;
  }

  if (r.network) {
    return setNotice(
      form,
      'error',
      'Không kết nối được tới máy chủ. Vui lòng thử lại.'
    );
  }

  if (applyServerFieldErrors(form, r.data)) return;

  setNotice(
    form,
    'error',
    msgFrom(r, 'Không thể đặt lại mật khẩu. Mã xác thực có thể đã hết hạn.')
  );
},
async resendOtp(form) {
  const email = window.forgotPasswordEmail;

  if (!email) {
    return setNotice(
      form,
      'error',
      'Không tìm thấy email. Vui lòng quay lại và nhập email.'
    );
  }

  const r = await request(CONFIG.endpoints.resendOtp, {
    email
  });

  if (r.ok) {
    form.elements.otp.value = '';

    setNotice(
      form,
      'success',
      'Nếu email có trong hệ thống, mã OTP mới đã được gửi. Vui lòng kiểm tra hộp thư.'
    );

    form.elements.otp.focus();
    return;
  }

  if (r.network) {
    return setNotice(
      form,
      'error',
      'Không kết nối được tới máy chủ. Vui lòng thử lại.'
    );
  }

  setNotice(
    form,
    'error',
    msgFrom(r, 'Không thể gửi lại mã OTP. Vui lòng thử lại sau.')
  );
},
};

  /* ---------- Đăng nhập / đăng ký bằng Google (Google Identity Services) ----------
     Luồng: GIS trả về credential (= Google ID Token) => FE gửi { idToken } tới POST /google.
     FE KHÔNG tự tin tưởng token: việc xác minh chữ ký/issuer/audience/hạn dùng là của Backend.
     Lưu ý: nút chính thức của Google (renderButton) không báo sự kiện khi người dùng đóng cửa sổ chọn
     tài khoản, nên khi đó giao diện đơn giản giữ nguyên trạng thái chờ, không bị kẹt "đang xử lý". */
  const GIS_SRC = 'https://accounts.google.com/gsi/client';
  const GOOGLE_G =
    '<svg class="gbtn__logo" viewBox="0 0 48 48" aria-hidden="true" focusable="false">' +
    '<path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9.1 3.6l6.8-6.8C35.9 2.4 30.4 0 24 0 14.6 0 6.5 5.4 2.6 13.2l7.9 6.1C12.4 13.6 17.7 9.5 24 9.5z"/>' +
    '<path fill="#4285F4" d="M46.5 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.7c-.6 3-2.3 5.5-4.8 7.2l7.5 5.8c4.4-4.1 7.1-10.1 7.1-17.5z"/>' +
    '<path fill="#FBBC05" d="M10.5 28.7a14.5 14.5 0 0 1 0-9.4l-7.9-6.1a24 24 0 0 0 0 21.6l7.9-6.1z"/>' +
    '<path fill="#34A853" d="M24 48c6.5 0 11.9-2.1 15.9-5.8l-7.5-5.8c-2.1 1.4-4.8 2.3-8.4 2.3-6.3 0-11.6-4.1-13.5-9.8l-7.9 6.1C6.5 42.6 14.6 48 24 48z"/></svg>';

  let gisPromise = null;
  function loadGis() {
    if (window.google && window.google.accounts && window.google.accounts.id) return Promise.resolve();
    if (!gisPromise) {
      gisPromise = new Promise((resolve, reject) => {
        const el = document.createElement('script');
        el.src = GIS_SRC;
        el.async = true;
        el.defer = true;
        el.onload = () => (window.google && window.google.accounts && window.google.accounts.id ? resolve() : reject(new Error('gis')));
        el.onerror = () => { gisPromise = null; reject(new Error('gis')); }; // cho phép thử lại
        document.head.appendChild(el);
      });
    }
    return gisPromise;
  }

  /* Thông báo ngay dưới nút Google (gần chỗ người dùng đang nhìn). type: info | success | error | busy */
  function setGoogleMessage(form, type, message) {
    const slot = form.querySelector('[data-google]');
    if (!slot) return;
    const box = slot.querySelector('[data-google-status]');
    if (!message) { box.hidden = true; box.textContent = ''; return; }
    const kind = type === 'busy' ? 'info' : type;
    box.className = `social__status notice notice--${kind}`;
    box.setAttribute('role', type === 'error' ? 'alert' : 'status');
    box.innerHTML = type === 'busy'
      ? '<span class="spinner spinner--dark" aria-hidden="true"></span><span></span>'
      : `${icon(type === 'success' ? 'check' : 'alert')}<span></span>`;
    box.lastChild.textContent = message;
    box.hidden = false;
  }

  function setGoogleBusy(form, busy) {
    const slot = form.querySelector('[data-google]');
    slot.classList.toggle('is-busy', busy);
    slot.setAttribute('aria-busy', String(busy));
    form.querySelector('[type="submit"]').disabled = busy;
    if (busy) setGoogleMessage(form, 'busy', 'Đang xác thực với Google…');
  }

  async function processGoogle(form, credential) {
    if (form.dataset.busy === 'true') return;
    if (!credential) {
      setGoogleMessage(form, 'error', 'Google không trả về thông tin đăng nhập. Vui lòng thử lại.');
      return;
    }
    form.dataset.busy = 'true';
    setNotice(form, 'info', '');
    renderSummary(form, []);
    setGoogleBusy(form, true);
    try {
      await handlers.google(form, credential);
    } finally {
      form.dataset.busy = 'false';
      setGoogleBusy(form, false);
    }
  }

  function initGoogle(form) {
    const slot = form.querySelector('[data-google]');
    if (!slot) return;
    const isRegister = form.dataset.form === 'register';
    const holder = slot.querySelector('[data-google-btn]');
    const labelText = isRegister ? 'Đăng ký bằng Google' : 'Đăng nhập bằng Google';

    const unavailable = (message, retry) => {
      holder.innerHTML = '';
      setGoogleMessage(form, 'info', message);
      if (retry) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn btn--ghost btn--small';
        btn.textContent = 'Thử lại';
        btn.addEventListener('click', () => { setGoogleMessage(form, 'info', ''); holder.innerHTML = ''; start(); });
        holder.appendChild(btn);
      }
    };

    // Chế độ mock không có Client ID: nút giả để thử luồng thành công/lỗi mà không cần Google
    if (!CONFIG.googleClientId) {
      if (!CONFIG.mock) return unavailable('Đăng nhập bằng Google chưa được cấu hình (thiếu googleClientId).');
      const variant = new URLSearchParams(window.location.search).get('mockGoogle');
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'gbtn';
      btn.innerHTML = `${GOOGLE_G}<span>${labelText} (mô phỏng)</span>`;
      btn.addEventListener('click', () => processGoogle(form, variant ? `mock-${variant}` : 'mock-google-token'));
      holder.appendChild(btn);
      return;
    }

    let initialized = false;
    let lastWidth = 0;
    const render = () => {
      const width = Math.max(200, Math.min(400, Math.floor(holder.clientWidth || 320)));
      if (width === lastWidth && holder.childElementCount) return;
      lastWidth = width;
      holder.innerHTML = '';
      window.google.accounts.id.renderButton(holder, {
        type: 'standard', theme: 'outline', size: 'large', shape: 'rectangular', logo_alignment: 'left',
        text: isRegister ? 'signup_with' : 'signin_with', locale: 'vi', width,
      });
    };

    function start() {
      holder.setAttribute('aria-busy', 'true');
      loadGis()
        .then(() => {
          if (!initialized) {
            window.google.accounts.id.initialize({
              client_id: CONFIG.googleClientId,
              callback: (resp) => processGoogle(form, resp && resp.credential),
              auto_select: false,
              cancel_on_tap_outside: true,
              ux_mode: 'popup',
            });
            initialized = true;
          }
          render();
          let t = null;
          window.addEventListener('resize', () => { clearTimeout(t); t = setTimeout(render, 150); });
        })
        .catch(() => unavailable('Không tải được Google. Kiểm tra kết nối mạng rồi thử lại, hoặc dùng email và mật khẩu.', true))
        .finally(() => holder.removeAttribute('aria-busy'));
    }
    start();
  }

  /* ---------- Gắn vào form ---------- */
  // Xử lý nút Gửi lại mã OTP
document.querySelectorAll('[data-resend-otp]').forEach((button) => {
  button.addEventListener('click', async () => {
    const form = button.closest('form');

    if (!form || form.dataset.busy === 'true') return;

    form.dataset.busy = 'true';
    button.disabled = true;

    setNotice(form, 'info', 'Đang gửi lại mã OTP...');

    try {
      await handlers.resendOtp(form);
    } finally {
      button.disabled = false;
      form.dataset.busy = 'false';
    }
  });
});
  document.querySelectorAll('form[data-form]').forEach((form) => {
    const type = form.dataset.form;
    const inputs = Array.from(form.querySelectorAll('.input, [data-validate]'));
    initStrength(form);
    initGoogle(form);

    inputs.forEach((input) => {
      if (input.type === 'checkbox') {
        input.addEventListener('change', () => { if (input.hasAttribute('aria-invalid')) validateField(input, form); });
        return;
      }
      input.addEventListener('blur', () => { if (input.value || input.hasAttribute('aria-invalid')) validateField(input, form); });
      input.addEventListener('input', () => {
        if (input.hasAttribute('aria-invalid')) validateField(input, form);
        // Đổi mật khẩu thì kiểm tra lại ô xác nhận nếu đã nhập
        if (input.name === 'password' && form.elements.confirmPassword && form.elements.confirmPassword.value) {
          validateField(form.elements.confirmPassword, form);
        }
      });
    });

    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (form.dataset.busy === 'true') return;
      setNotice(form, 'info', '');

      const problems = [];
      inputs.forEach((input) => {
        const message = validateField(input, form);
        if (message) problems.push({ input, message });
      });
      renderSummary(form, problems);
      if (problems.length) {
        const box = form.querySelector('[data-summary]');
        (box || problems[0].input).focus();
        return;
      }

      form.dataset.busy = 'true';
      setLoading(form, true);
      try {
        await handlers[type](form);
      } finally {
        form.dataset.busy = 'false';
        setLoading(form, false);
      }
    });
  });
})();
