/* =========================================================
   Sneaker House — Auth module (FE1)
   JS thuần, không phụ thuộc thư viện.

   ĐIỂM TÍCH HỢP VỚI BACKEND (BE1) — chỉ cần chỉnh ở đây hoặc đặt
   window.AUTH_CONFIG TRƯỚC khi nạp file này:

     window.AUTH_CONFIG = {
       baseUrl: 'http://localhost:8080/api/auth',
       endpoints: { login: '/login', register: '/register', forgotPassword: '/forgot-password' }
     };

   Body gửi đi (JSON):
     login          { identifier, password, rememberMe }
     register       { fullName, email, password }
     forgotPassword { email }
   Lỗi trả về nên có dạng { message: "...", errors?: { tenTruong: "..." } }.
   Nếu BE1 dùng tên trường khác, sửa ở hàm buildPayload().
   ========================================================= */
(() => {
  'use strict';

  const CONFIG = Object.assign(
    {
      baseUrl: '/api/auth',
      endpoints: { login: '/login', register: '/register', forgotPassword: '/forgot-password' },
      redirectAfterLogin: 'index.html',
      productsUrl: 'products.html', // trang danh sách sản phẩm (module khác) — nút trên carousel trỏ tới đây
    },
    window.AUTH_CONFIG || {}
  );
  CONFIG.endpoints = Object.assign(
    { login: '/login', register: '/register', forgotPassword: '/forgot-password' },
    CONFIG.endpoints
  );

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
    input.closest('.pw').after(hint);
    const syncCaps = (e) => { hint.hidden = !(e.getModifierState && e.getModifierState('CapsLock')); };
    input.addEventListener('keydown', syncCaps);
    input.addEventListener('keyup', syncCaps);
    input.addEventListener('blur', () => { hint.hidden = true; });
  });

  /* ---------- Validation ---------- */
  const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const PW_RULES = {
    len: (v) => v.length >= 8,
    upper: (v) => /[A-Z]/.test(v),
    lower: (v) => /[a-z]/.test(v),
    digit: (v) => /\d/.test(v),
  };

  const validators = {
    identifier: (v) => (v.trim() ? '' : 'Vui lòng nhập email hoặc tên đăng nhập.'),
    fullName: (v) => {
      if (!v.trim()) return 'Vui lòng nhập họ và tên.';
      return v.trim().length < 2 ? 'Họ và tên cần ít nhất 2 ký tự.' : '';
    },
    email: (v) => {
      if (!v.trim()) return 'Vui lòng nhập email.';
      return EMAIL_RE.test(v.trim()) ? '' : 'Email chưa đúng định dạng, ví dụ ten@gmail.com.';
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
      return v === form.elements.password.value ? '' : 'Mật khẩu nhập lại chưa khớp.';
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

  /* ---------- Gọi API ---------- */
  async function request(path, body) {
    let res;
    try {
      res = await fetch(CONFIG.baseUrl + path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        credentials: 'include',
        body: JSON.stringify(body),
      });
    } catch (_) {
      return { ok: false, network: true, status: 0, data: null };
    }
    let data = null;
    try { data = await res.json(); } catch (_) { /* phản hồi không phải JSON */ }
    return { ok: res.ok, network: false, status: res.status, data };
  }

  function buildPayload(type, form) {
    const f = form.elements;
    if (type === 'login') {
      return { identifier: f.identifier.value.trim(), password: f.password.value, rememberMe: f.rememberMe.checked };
    }
    if (type === 'register') {
      // Việc đồng ý điều khoản (f.terms) chỉ kiểm tra ở FE; nếu BE cần lưu sự đồng ý, thêm trường tại đây.
      return { fullName: f.fullName.value.trim(), email: f.email.value.trim(), password: f.password.value };
    }
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

    async forgot(form) {
      const r = await request(CONFIG.endpoints.forgotPassword, buildPayload('forgot', form));
      if (r.ok) {
        // Thông điệp trung tính, không tiết lộ email có tồn tại hay không
        const email = form.elements.email.value.trim();
        return showStatus(form, {
          title: 'Kiểm tra hộp thư của bạn',
          text: `Nếu ${email} có trong hệ thống, hướng dẫn đặt lại mật khẩu sẽ được gửi tới địa chỉ này. Chưa thấy email? Hãy kiểm tra cả thư mục spam.`,
        });
      }
      if (r.network || r.status === 404 || r.status === 501) {
        return setNotice(form, 'info', 'Chức năng khôi phục mật khẩu hiện chưa được kết nối với máy chủ nên chưa thể gửi email. Vui lòng quay lại sau.');
      }
      setNotice(form, 'error', msgFrom(r, 'Chưa gửi được yêu cầu. Vui lòng thử lại.'));
    },
  };

  /* ---------- Gắn vào form ---------- */
  document.querySelectorAll('form[data-form]').forEach((form) => {
    const type = form.dataset.form;
    const inputs = Array.from(form.querySelectorAll('.input, [data-validate]'));
    initStrength(form);

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
