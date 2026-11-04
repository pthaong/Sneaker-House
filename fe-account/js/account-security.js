/* =========================================================
   Sneaker House — Bảo mật tài khoản (FE1, issue #10)
   JS thuần, không thư viện. Cấu hình ở js/account-config.js.

   5 khu vực: đổi mật khẩu · phương thức đăng nhập · khôi phục mật khẩu (OTP)
              · phiên đăng nhập · lưu ý bảo mật.

   An toàn: không lưu mật khẩu/OTP/token vào localStorage, sessionStorage hay cookie.
   Mật khẩu chỉ nằm trong ô nhập và bị xóa sau khi gửi thành công; resetToken chỉ giữ
   trong biến JS (mất khi tải lại trang). Mọi nội dung từ server đều chèn bằng textContent.
   ========================================================= */
(() => {
  'use strict';

  /* ---------- Cấu hình ---------- */
  const DEFAULTS = {
    apiBase: '/api',
    loginUrl: '../fe-auth/login.html',
    links: {},
    endpoints: {},
    changePasswordMethod: 'POST',
    mock: {},
  };
  const user = window.ACCOUNT_CONFIG || {};
  const CONFIG = {
    ...DEFAULTS,
    ...user,
    links: { ...DEFAULTS.links, ...user.links },
    endpoints: { ...DEFAULTS.endpoints, ...user.endpoints },
    mock: { ...DEFAULTS.mock, ...user.mock },
  };

  // Mock chỉ chạy ở máy dev. Lên tên miền thật mà quên tắt thì vẫn gọi API thật.
  const IS_LOCAL = ['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname);
  const MOCK = {};
  Object.keys(CONFIG.mock).forEach((k) => { MOCK[k] = !!CONFIG.mock[k] && IS_LOCAL; });
  if (Object.values(CONFIG.mock).some(Boolean) && !IS_LOCAL) {
    console.warn('[account] mock bị bỏ qua vì trang không chạy ở localhost — đang gọi API thật.');
  }

  /* ---------- Tiện ích DOM ---------- */
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

  function h(tag, attrs, ...kids) {
    const el = document.createElement(tag);
    Object.entries(attrs || {}).forEach(([k, v]) => {
      if (v == null || v === false) return;
      if (k === 'class') el.className = v;
      else if (k === 'html') el.innerHTML = v;
      else el.setAttribute(k, v === true ? '' : v);
    });
    kids.flat().forEach((c) => { if (c != null) el.append(c.nodeType ? c : document.createTextNode(c)); });
    return el;
  }

  /* ---------- Icon (nét 2px, cùng bộ với trang đăng nhập) ---------- */
  const ICONS = {
    eye: '<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
    eyeOff: '<path d="M17.94 17.94A10.4 10.4 0 0 1 12 19c-6.5 0-10-7-10-7a17.7 17.7 0 0 1 4.06-5.06M9.9 4.24A9.9 9.9 0 0 1 12 4c6.5 0 10 7 10 7a17.8 17.8 0 0 1-2.16 3.19M1 1l22 22"/><path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/>',
    alert: '<circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>',
    check: '<path d="M20 6 9 17l-5-5"/>',
    circle: '<circle cx="12" cy="12" r="9"/>',
    mail: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
    lock: '<rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
    shield: '<path d="M12 3 4 6v6c0 4.5 3.2 8.3 8 9 4.8-.7 8-4.5 8-9V6z"/><path d="m9 12 2 2 4-4"/>',
    monitor: '<rect x="2" y="4" width="20" height="13" rx="2"/><path d="M8 21h8M12 17v4"/>',
    phone: '<rect x="7" y="2" width="10" height="20" rx="2"/><path d="M11 18h2"/>',
    truck: '<path d="M1 3h15v13H1zM16 8h4l3 3v5h-7z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/>',
    refresh: '<path d="M23 4v6h-6M1 20v-6h6"/><path d="M3.5 9a9 9 0 0 1 14.9-3.4L23 10M1 14l4.6 4.4A9 9 0 0 0 20.5 15"/>',
    shieldCheck: '<path d="M12 3 4 6v6c0 4.5 3.2 8.3 8 9 4.8-.7 8-4.5 8-9V6z"/><path d="m9 12 2 2 4-4"/>',
    pin: '<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>',
    package: '<path d="m7.5 4.3 9 5.2M21 8v8l-9 5-9-5V8l9-5z"/><path d="M3.3 7 12 12l8.7-5M12 22V12"/>',
    headset: '<path d="M3 14v-2a9 9 0 0 1 18 0v2"/><path d="M21 15a2 2 0 0 1-2 2h-1v-6h1a2 2 0 0 1 2 2zM3 15a2 2 0 0 0 2 2h1v-6H5a2 2 0 0 0-2 2z"/><path d="M21 17v1a3 3 0 0 1-3 3h-4"/>',
    heart: '<path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z"/>',
    cart: '<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/>',
    chevronDown: '<path d="m6 9 6 6 6-6"/>',
    chevronRight: '<path d="m9 6 6 6-6 6"/>',
    menu: '<path d="M3 6h18M3 12h18M3 18h18"/>',
    search: '<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
    key: '<circle cx="7.5" cy="15.5" r="5.5"/><path d="m21 2-9.6 9.6M15.5 7.5l3 3L22 7l-3-3"/>',
    file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>',
    bell: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0"/>',
    gear: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 0 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 0 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 0 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 0 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/>',
    logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>',
    info: '<circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/>',
    users: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8"/>',
    ticket: '<path d="M3 9a2 2 0 0 0 0 6v3a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-3a2 2 0 0 1 0-6V6a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1z"/><path d="M13 5v2M13 17v2M13 11v2"/>',
  };
  const icon = (name) =>
    `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">${ICONS[name]}</svg>`;
  const GOOGLE_G =
    '<svg class="gicon" viewBox="0 0 48 48" aria-hidden="true" focusable="false">' +
    '<path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9.1 3.6l6.8-6.8C35.9 2.4 30.4 0 24 0 14.6 0 6.5 5.4 2.6 13.2l7.9 6.1C12.4 13.6 17.7 9.5 24 9.5z"/>' +
    '<path fill="#4285F4" d="M46.5 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.7c-.6 3-2.3 5.5-4.8 7.2l7.5 5.8c4.4-4.1 7.1-10.1 7.1-17.5z"/>' +
    '<path fill="#FBBC05" d="M10.5 28.7a14.5 14.5 0 0 1 0-9.4l-7.9-6.1a24 24 0 0 0 0 21.6l7.9-6.1z"/>' +
    '<path fill="#34A853" d="M24 48c6.5 0 11.9-2.1 15.9-5.8l-7.5-5.8c-2.1 1.4-4.8 2.3-8.4 2.3-6.3 0-11.6-4.1-13.5-9.8l-7.9 6.1C6.5 42.6 14.6 48 24 48z"/></svg>';

  /* ---------- Thông báo, lỗi từng ô, nút đang xử lý ---------- */
  function setNotice(name, kind, text) {
    const el = $(`[data-notice="${name}"]`);
    if (!el) return;
    if (!text) { el.hidden = true; el.textContent = ''; return; }
    el.className = `notice notice--${kind}`;
    el.setAttribute('role', kind === 'error' ? 'alert' : 'status');
    el.replaceChildren(
      h('span', { class: 'notice__icon', html: icon(kind === 'success' ? 'check' : 'alert') }),
      h('div', { class: 'notice__body' }, h('p', null, text))
    );
    el.hidden = false;
  }

  function setFieldError(input, message) {
    const p = document.getElementById(`${input.id}-error`);
    if (!p) return;
    if (!message) {
      p.hidden = true; p.textContent = '';
      input.removeAttribute('aria-invalid');
      return;
    }
    p.replaceChildren(h('span', { html: icon('alert') }), h('span', null, message));
    p.hidden = false;
    input.setAttribute('aria-invalid', 'true');
  }

  function clearErrors(form) {
    $$('.input', form).forEach((i) => setFieldError(i, ''));
  }

  function focusFirstInvalid(form) {
    const bad = $('[aria-invalid="true"]', form);
    if (bad) bad.focus();
  }

  function setLoading(btn, on) {
    btn.disabled = on;
    btn.classList.toggle('is-loading', on);
    btn.setAttribute('aria-busy', String(on));
    const sp = $('.spinner', btn);
    if (sp) sp.hidden = !on;
    const label = $('[data-label]', btn);
    if (label) {
      if (on) {
        label.dataset.orig = label.textContent;
        label.textContent = btn.dataset.loading || label.textContent;
      } else if (label.dataset.orig) {
        label.textContent = label.dataset.orig;
      }
    }
  }

  /* ---------- Ẩn/hiện mật khẩu ---------- */
  function setPwVisible(btn, show) {
    const input = document.getElementById(btn.dataset.togglePw);
    if (!input) return;
    input.type = show ? 'text' : 'password';
    btn.innerHTML = icon(show ? 'eye' : 'eyeOff');
    btn.setAttribute('aria-label', 'Hiện mật khẩu');
    btn.setAttribute('aria-pressed', String(show));
  }
  const hidePasswords = (scope) => $$('[data-toggle-pw]', scope).forEach((b) => setPwVisible(b, false));

  /* ---------- Quy tắc mật khẩu + thanh độ mạnh ---------- */
  // Khớp với backend: 8–72 ký tự; thêm yêu cầu chữ hoa, chữ thường, số như trang đăng ký.
  const PW_RULES = [
    { key: 'len', text: 'Ít nhất 8 ký tự', test: (v) => v.length >= 8 },
    { key: 'upper', text: 'Có chữ hoa (A–Z)', test: (v) => /[A-Z]/.test(v) },
    { key: 'lower', text: 'Có chữ thường (a–z)', test: (v) => /[a-z]/.test(v) },
    { key: 'digit', text: 'Có chữ số (0–9)', test: (v) => /\d/.test(v) },
  ];

  function validateNewPassword(v) {
    if (!v) return 'Vui lòng nhập mật khẩu mới.';
    if (v.length > 72) return 'Mật khẩu tối đa 72 ký tự.';
    return PW_RULES.every((r) => r.test(v))
      ? ''
      : 'Mật khẩu chưa đủ mạnh: cần ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số.';
  }

  function bindStrength(slot) {
    const input = document.getElementById(slot.dataset.strengthFor);
    slot.append(
      h('ul', { class: 'reqs' },
        PW_RULES.map((r) => h('li', { 'data-req': r.key, 'data-met': 'false' },
          h('span', { html: icon('check') }), h('span', { class: 'sr-only' }, 'Chưa đạt: '), r.text)))
    );
    const update = () => {
      PW_RULES.forEach((r) => {
        const ok = r.test(input.value);
        const li = $(`[data-req="${r.key}"]`, slot);
        li.dataset.met = String(ok);
        $('.sr-only', li).textContent = ok ? 'Đã đạt: ' : 'Chưa đạt: ';
      });
    };
    input.addEventListener('input', update);
    slot.refresh = update;
    update();
  }
  const refreshStrength = (form) => $$('[data-strength-slot]', form).forEach((s) => s.refresh && s.refresh());

  function resetForm(form) {
    form.reset();
    clearErrors(form);
    hidePasswords(form);
    refreshStrength(form);
  }

  /* ---------- Gọi API (cookie phiên + CSRF) ---------- */
  const endpoint = (key, params) =>
    CONFIG.apiBase + CONFIG.endpoints[key].replace('{id}', encodeURIComponent((params && params.id) || ''));

  let csrfCache = null;
  async function fetchCsrf() {
    const res = await fetch(CONFIG.apiBase + CONFIG.endpoints.csrf, {
      credentials: 'include',
      headers: { Accept: 'application/json' },
    });
    if (!res.ok) throw new Error(`csrf ${res.status}`);
    csrfCache = await res.json();
    return csrfCache;
  }

  // Trả về { ok, status, data, network }. Không bao giờ ném lỗi ra ngoài.
  async function api(key, { method = 'GET', body, params } = {}) {
    // Thu hồi phiên đi cùng cờ mock của danh sách phiên
    if (MOCK[key === 'revokeSession' ? 'sessions' : key]) return mockApi(key, { method, body, params });
    try {
      const headers = { Accept: 'application/json' };
      const opts = { method, credentials: 'include', headers };
      if (method !== 'GET') {
        headers['Content-Type'] = 'application/json';
        if (body !== undefined) opts.body = JSON.stringify(body);
      }
      const send = async (fresh) => {
        if (method !== 'GET') {
          const c = !csrfCache || fresh ? await fetchCsrf() : csrfCache;
          headers[c.headerName || 'X-XSRF-TOKEN'] = c.token;
        }
        return fetch(endpoint(key, params), opts);
      };
      let res = await send(false);
      if (res.status === 403 && method !== 'GET') res = await send(true); // token CSRF có thể đã đổi
      let data = null;
      try { data = await res.json(); } catch (_) { /* phản hồi không phải JSON */ }
      return { ok: res.ok, status: res.status, data, network: false };
    } catch (err) {
      console.error('[account] API lỗi:', err);
      return { ok: false, status: 0, data: null, network: true };
    }
  }

  const errCode = (r) => String((r.data && (r.data.error || r.data.code)) || '');
  const errMsg = (r, fallback) =>
    r.data && typeof r.data.message === 'string' && r.data.message.trim() ? r.data.message : fallback;
  const NETWORK_MSG = 'Không kết nối được tới máy chủ. Vui lòng kiểm tra mạng rồi thử lại.';

  /* ---------- Thông báo đầu trang + phiên hết hạn ---------- */
  let pageRetry = null;
  function showPageAlert(text, { login = false, retry = null } = {}) {
    $('[data-page-alert-text]').textContent = text;
    $('[data-page-alert-login]').hidden = !login;
    $('[data-page-alert-retry]').hidden = !retry;
    pageRetry = retry;
    const box = $('[data-page-alert]');
    box.hidden = false;
    box.scrollIntoView({ block: 'nearest' });
  }
  const hidePageAlert = () => { $('[data-page-alert]').hidden = true; pageRetry = null; };
  $('[data-page-alert-retry]').addEventListener('click', () => { if (pageRetry) pageRetry(); });

  // 401/403 có thể là hết phiên HOẶC chỉ là bị từ chối thao tác này → kiểm tra lại bằng GET profile.
  async function sessionExpired(res) {
    if (res.status !== 401 && res.status !== 403) return false;
    const check = await api('profile');
    if (check.status === 401 || check.status === 403) {
      showPageAlert('Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại để tiếp tục.', { login: true });
      return true;
    }
    return false;
  }

  /* =========================================================
     Hồ sơ người dùng → header, phương thức đăng nhập, email khôi phục
     ========================================================= */
  const state = { profile: null };

  function renderProfile(p) {
    state.profile = p;

    $('[data-recovery-email]').textContent = p.email || '';
    $('[data-recovery-email-echo]').textContent = p.email || '';
    $('[data-recovery-send]').disabled = !p.email;

    const hasPassword = p.loginMethod === 'EMAIL_PASSWORD' || p.loginMethod === 'BOTH';
    const hasGoogle = !!p.googleLinked || p.loginMethod === 'GOOGLE' || p.loginMethod === 'BOTH';

    const badge = (on, onText, offText) =>
      h('span', { class: `badge${on ? '' : ' badge--off'}`, html: icon(on ? 'check' : 'circle') }, on ? onText : offText);

    const list = $('[data-methods]');
    list.replaceChildren(
      h('li', null,
        h('span', { class: 'list__icon', html: icon('mail') }),
        h('div', null,
          h('p', { class: 'list__title' }, 'Email và mật khẩu'),
          h('p', { class: 'list__sub' }, hasPassword ? 'Đăng nhập bằng email và mật khẩu của bạn.' : 'Chưa thiết lập mật khẩu.')),
        h('div', { class: 'row__side' }, badge(hasPassword, 'Đang sử dụng', 'Chưa thiết lập'))),
      h('li', null,
        h('span', { class: 'list__icon', html: GOOGLE_G }),
        h('div', null,
          h('p', { class: 'list__title' }, 'Google'),
          h('p', { class: 'list__sub' }, hasGoogle ? 'Đăng nhập nhanh bằng tài khoản Google.' : 'Chưa liên kết với tài khoản Google.')),
        h('div', { class: 'row__side' }, badge(hasGoogle, 'Đã liên kết', 'Chưa liên kết')))
    );
    list.setAttribute('aria-busy', 'false');
    $('[data-methods-note]').textContent =
      hasPassword && hasGoogle ? 'Tài khoản hiện đã liên kết với Google. Bạn có thể sử dụng cả email/mật khẩu và Google để đăng nhập.' :
      hasGoogle ? 'Tài khoản của bạn đăng nhập bằng Google và chưa có mật khẩu. Bạn có thể đặt mật khẩu ở mục Khôi phục mật khẩu.' :
      'Tài khoản của bạn chưa liên kết với Google. Bạn đăng nhập bằng email và mật khẩu.';

    // Tài khoản chỉ đăng nhập bằng Google chưa có mật khẩu → không có mật khẩu hiện tại để nhập
    $('[data-google-only]').hidden = hasPassword;
    $('[data-form="password"]').hidden = !hasPassword;
  }

  function renderProfileFailed(text) {
    $('[data-methods]').replaceChildren(h('li', { class: 'placeholder' }, text));
    $('[data-methods]').setAttribute('aria-busy', 'false');
    $('[data-recovery-email]').textContent = 'Không tải được';
    $('[data-methods-note]').textContent = 'Không tải được dữ liệu.';
  }

  async function loadProfile({ silent = false } = {}) {
    if (!silent) hidePageAlert();
    const r = await api('profile');
    if (r.network) {
      if (!silent) {
        renderProfileFailed('Không tải được dữ liệu.');
        showPageAlert(NETWORK_MSG, { retry: () => init() });
      }
      return false;
    }
    if (r.status === 401 || r.status === 403) {
      renderProfileFailed('Cần đăng nhập để xem.');
      showPageAlert('Bạn cần đăng nhập để xem trang này. Phiên đăng nhập có thể đã hết hạn.', { login: true });
      return false;
    }
    if (!r.ok || !r.data) {
      if (!silent) {
        renderProfileFailed('Không tải được dữ liệu.');
        showPageAlert(errMsg(r, 'Không tải được thông tin tài khoản. Vui lòng thử lại.'), { retry: () => init() });
      }
      return false;
    }
    renderProfile(r.data);
    return true;
  }

  /* ---------- Đăng xuất (nút ở header, không thay cho phần Phiên đăng nhập) ---------- */
  $$('[data-logout]').forEach((el) => el.addEventListener('click', async (e) => {
    const btn = e.currentTarget;
    btn.disabled = true;
    const r = await api('logout', { method: 'POST' });
    if (r.ok || r.status === 401 || r.status === 403) {
      window.location.href = CONFIG.loginUrl;
      return;
    }
    btn.disabled = false;
    showPageAlert(r.network ? NETWORK_MSG : errMsg(r, 'Chưa đăng xuất được. Vui lòng thử lại.'));
  }));

  /* =========================================================
     1. Đổi mật khẩu
     ========================================================= */
  const pwForm = $('[data-form="password"]');

  function mapChangePasswordError(r) {
    const code = errCode(r);
    const msg = errMsg(r, '');
    const wrongCurrent =
      /CURRENT|WRONG|INCORRECT|INVALID_PASSWORD/i.test(code) || /hiện tại|current/i.test(msg);
    const sameAsOld = /SAME|REUSE/i.test(code) || /trùng|khác mật khẩu cũ|same/i.test(msg);
    return { wrongCurrent, sameAsOld, msg };
  }

  pwForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    setNotice('password', null);
    clearErrors(pwForm);
    const f = pwForm.elements;

    const newErr = validateNewPassword(f.newPassword.value);
    if (!f.currentPassword.value) setFieldError(f.currentPassword, 'Vui lòng nhập mật khẩu hiện tại.');
    if (newErr) setFieldError(f.newPassword, newErr);
    else if (f.newPassword.value === f.currentPassword.value) setFieldError(f.newPassword, 'Mật khẩu mới phải khác mật khẩu hiện tại.');
    if (!f.confirmPassword.value) setFieldError(f.confirmPassword, 'Vui lòng nhập lại mật khẩu mới.');
    else if (f.confirmPassword.value !== f.newPassword.value) setFieldError(f.confirmPassword, 'Mật khẩu nhập lại chưa khớp.');
    if ($('[aria-invalid="true"]', pwForm)) return focusFirstInvalid(pwForm);

    const btn = $('button[type="submit"]', pwForm);
    setLoading(btn, true);
    const r = await api('changePassword', {
      method: CONFIG.changePasswordMethod,
      body: { currentPassword: f.currentPassword.value, newPassword: f.newPassword.value },
    });
    setLoading(btn, false);

    if (r.ok) {
      resetForm(pwForm);
      setNotice('password', 'success', 'Đã cập nhật mật khẩu. Lần đăng nhập tiếp theo, hãy dùng mật khẩu mới.');
      loadSessions({ quiet: true }); // server có thể đã thu hồi các phiên khác
      return;
    }
    if (r.network) return setNotice('password', 'error', NETWORK_MSG);
    if (await sessionExpired(r)) return;
    if (r.status === 404 || r.status === 501) {
      return setNotice('password', 'info', 'Chức năng đổi mật khẩu chưa được kết nối với máy chủ. Vui lòng quay lại sau.');
    }
    const { wrongCurrent, sameAsOld, msg } = mapChangePasswordError(r);
    if (wrongCurrent) {
      setFieldError(f.currentPassword, 'Mật khẩu hiện tại chưa đúng.');
      return f.currentPassword.focus();
    }
    if (sameAsOld) {
      setFieldError(f.newPassword, 'Mật khẩu mới phải khác mật khẩu hiện tại.');
      return f.newPassword.focus();
    }
    if (r.status === 429) return setNotice('password', 'error', msg || 'Bạn thử quá nhiều lần. Vui lòng đợi một lúc rồi thử lại.');
    setNotice('password', 'error', msg || 'Chưa đổi được mật khẩu. Vui lòng thử lại.');
  });

  $('[data-cancel="password"]').addEventListener('click', () => {
    resetForm(pwForm);
    setNotice('password', null);
  });

  // Xóa lỗi của ô khi người dùng bắt đầu sửa
  $$('.input', pwForm).forEach((i) => i.addEventListener('input', () => setFieldError(i, '')));

  /* =========================================================
     3. Khôi phục mật khẩu bằng OTP (dùng API sẵn có của BE1)
        request → otp → reset → done
     ========================================================= */
  const recovery = $('[data-recovery]');
  const otpForm = $('[data-form="otp"]');
  const resetFormEl = $('[data-form="reset"]');
  const resendBtn = $('[data-recovery-resend]');
  const rec = { token: null, timer: null };

  function showStep(step) {
    recovery.dataset.step = step;
    $$('[data-step-panel]', recovery).forEach((p) => { p.hidden = p.dataset.stepPanel !== step; });
    const panel = $(`[data-step-panel="${step}"]`, recovery);
    const target =
      step === 'otp' ? $('#otp') :
      step === 'reset' ? $('#resetPassword') :
      step === 'done' ? $('[data-step-heading]', panel) :
      $('[data-recovery-send]', panel);
    if (target) target.focus();
  }

  function stopCooldown() {
    clearInterval(rec.timer);
    rec.timer = null;
    resendBtn.disabled = false;
    resendBtn.textContent = 'Gửi lại mã';
  }
  function startCooldown(seconds) {
    clearInterval(rec.timer);
    let left = seconds;
    resendBtn.disabled = true;
    resendBtn.textContent = `Gửi lại mã (${left}s)`;
    rec.timer = setInterval(() => {
      left -= 1;
      if (left <= 0) return stopCooldown();
      resendBtn.textContent = `Gửi lại mã (${left}s)`;
    }, 1000);
  }

  function resetRecovery() {
    rec.token = null;
    stopCooldown();
    resetForm(otpForm);
    resetForm(resetFormEl);
    setNotice('recovery', null);
    showStep('request');
  }

  async function requestOtp(btn, resend) {
    const email = state.profile && state.profile.email;
    if (!email) return;
    setNotice('recovery', null);
    if (resend) btn.disabled = true; else setLoading(btn, true);
    const r = await api(resend ? 'resendOtp' : 'forgotPassword', { method: 'POST', body: { email } });
    if (!resend) setLoading(btn, false);

    if (r.ok) {
      showStep('otp');
      startCooldown(60);
      return setNotice('recovery', 'info', 'Nếu email đã đăng ký, mã OTP đã được gửi. Hãy kiểm tra hộp thư và cả thư mục spam.');
    }
    if (r.network) { if (resend) btn.disabled = false; return setNotice('recovery', 'error', NETWORK_MSG); }
    if (r.status === 429) {
      // Vừa yêu cầu gần đây: mã trước đó vẫn còn dùng được, chỉ cần chờ để gửi lại
      showStep('otp');
      startCooldown(60);
      return setNotice('recovery', 'info', errMsg(r, 'Bạn vừa yêu cầu mã OTP. Vui lòng chờ 60 giây trước khi gửi lại.'));
    }
    if (resend) btn.disabled = false;
    setNotice('recovery', 'error', errMsg(r, 'Chưa gửi được mã OTP. Vui lòng thử lại sau.'));
  }

  $('[data-recovery-send]').addEventListener('click', (e) => requestOtp(e.currentTarget, false));
  resendBtn.addEventListener('click', (e) => requestOtp(e.currentTarget, true));

  // Ô OTP chỉ nhận chữ số
  $('#otp').addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6);
    setFieldError(e.target, '');
  });

  otpForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    setNotice('recovery', null);
    const input = $('#otp');
    const v = input.value.trim();
    setFieldError(input, '');
    if (!v) { setFieldError(input, 'Vui lòng nhập mã OTP.'); return input.focus(); }
    if (!/^\d{6}$/.test(v)) { setFieldError(input, 'Mã OTP gồm đúng 6 chữ số.'); return input.focus(); }

    const btn = $('button[type="submit"]', otpForm);
    setLoading(btn, true);
    const r = await api('verifyOtp', { method: 'POST', body: { email: state.profile.email, otp: v } });
    setLoading(btn, false);

    if (r.ok) {
      const token = r.data && r.data.resetToken;
      if (!token) return setNotice('recovery', 'error', 'Chưa xác minh được mã OTP. Vui lòng thử lại.');
      rec.token = token;
      input.value = '';
      stopCooldown();
      showStep('reset');
      return;
    }
    if (r.network) return setNotice('recovery', 'error', NETWORK_MSG);
    if (r.status === 429) {
      input.value = '';
      return setNotice('recovery', 'error', errMsg(r, 'Bạn đã nhập sai quá nhiều lần. Hãy gửi lại mã mới để tiếp tục.'));
    }
    if (r.status === 400) {
      setFieldError(input, errMsg(r, 'Mã OTP sai hoặc đã hết hạn. Hãy kiểm tra lại hoặc gửi lại mã mới.'));
      return input.focus();
    }
    setNotice('recovery', 'error', errMsg(r, 'Chưa xác minh được mã OTP. Vui lòng thử lại.'));
  });

  resetFormEl.addEventListener('submit', async (e) => {
    e.preventDefault();
    setNotice('recovery', null);
    clearErrors(resetFormEl);
    const f = resetFormEl.elements;

    const err = validateNewPassword(f.newPassword.value);
    if (err) setFieldError(f.newPassword, err);
    if (!f.confirmPassword.value) setFieldError(f.confirmPassword, 'Vui lòng nhập lại mật khẩu mới.');
    else if (f.confirmPassword.value !== f.newPassword.value) setFieldError(f.confirmPassword, 'Mật khẩu nhập lại chưa khớp.');
    if ($('[aria-invalid="true"]', resetFormEl)) return focusFirstInvalid(resetFormEl);

    const btn = $('button[type="submit"]', resetFormEl);
    setLoading(btn, true);
    const r = await api('resetPassword', { method: 'POST', body: { token: rec.token, newPassword: f.newPassword.value } });
    setLoading(btn, false);

    if (r.ok) {
      rec.token = null;
      resetForm(resetFormEl);
      showStep('done');
      loadProfile({ silent: true }); // tài khoản chỉ có Google giờ đã có mật khẩu
      return;
    }
    if (r.network) return setNotice('recovery', 'error', NETWORK_MSG);
    if (/RESET_TOKEN/i.test(errCode(r))) {
      rec.token = null;
      resetForm(resetFormEl);
      showStep('request');
      return setNotice('recovery', 'error', 'Phiên xác minh đã hết hạn hoặc đã được dùng. Hãy yêu cầu mã OTP mới.');
    }
    if (r.status === 400) {
      setFieldError(f.newPassword, errMsg(r, 'Mật khẩu mới chưa hợp lệ. Vui lòng kiểm tra lại.'));
      return f.newPassword.focus();
    }
    setNotice('recovery', 'error', errMsg(r, 'Chưa đặt lại được mật khẩu. Vui lòng thử lại.'));
  });

  $$('[data-cancel="recovery"]').forEach((b) => b.addEventListener('click', resetRecovery));
  $$('.input', resetFormEl).forEach((i) => i.addEventListener('input', () => setFieldError(i, '')));

  /* =========================================================
     4. Phiên đăng nhập
     ========================================================= */
  const sessionsList = $('[data-sessions]');
  const pad2 = (n) => String(n).padStart(2, '0');
  const fmtDate = (d) => `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;

  // Chấp nhận vài tên trường khác nhau để BE1 đổi tên cũng không phải sửa FE
  function normalizeSession(s) {
    return {
      id: s.id != null ? s.id : s.sessionId,
      current: !!(s.current != null ? s.current : s.isCurrent),
      device: s.device || s.deviceName || '',
      browser: s.browser || '',
      os: s.os || s.platform || '',
      createdAt: s.createdAt || s.loginAt || s.loginTime || null,
    };
  }

  function sessionTitle(s) {
    if (s.browser && s.os) return `${s.os} • ${s.browser}`;
    return s.browser || s.os || s.device || 'Thiết bị không xác định';
  }

  function sessionRow(s) {
    const mobile = /iphone|android|mobile|phone|ipad|tablet/i.test(`${s.device} ${s.os}`);
    const when = s.createdAt ? new Date(s.createdAt) : null;
    const whenText = when && !isNaN(when) ? `Đăng nhập: ${fmtDate(when)}` : 'Không rõ thời gian đăng nhập';
    const title = sessionTitle(s);
    const side = s.current
      ? h('span', { class: 'badge' }, 'Phiên hiện tại')
      : h('button', {
          class: 'btn btn--small btn--ghost', type: 'button',
          'data-revoke': s.id, 'aria-label': `Đăng xuất thiết bị: ${title}`,
        }, 'Đăng xuất');
    return h('li', null,
      h('span', { class: 'list__icon', html: icon(mobile ? 'phone' : 'monitor') }),
      h('div', null,
        h('p', { class: 'list__title' }, title),
        h('p', { class: 'list__sub' }, whenText)),
      h('div', { class: 'row__side' }, side));
  }

  function renderSessions(items) {
    const sessions = items.map(normalizeSession).sort((a, b) =>
      (b.current - a.current) || (new Date(b.createdAt || 0) - new Date(a.createdAt || 0)));
    if (!sessions.length) {
      sessionsList.replaceChildren(h('li', { class: 'placeholder' }, 'Chưa có phiên đăng nhập nào.'));
    } else {
      const rows = sessions.map(sessionRow);
      if (!sessions.some((s) => !s.current)) {
        rows.push(h('li', { class: 'placeholder' }, 'Không có thiết bị nào khác đang đăng nhập.'));
      }
      sessionsList.replaceChildren(...rows);
    }
    sessionsList.setAttribute('aria-busy', 'false');
  }

  function sessionsMessage(text) {
    sessionsList.replaceChildren(h('li', { class: 'placeholder' }, text));
    sessionsList.setAttribute('aria-busy', 'false');
  }

  async function loadSessions({ quiet = false } = {}) {
    if (!quiet) { sessionsList.setAttribute('aria-busy', 'true'); setNotice('sessions', null); }
    const r = await api('sessions');
    if (r.ok) {
      const items = Array.isArray(r.data) ? r.data : (r.data && r.data.sessions) || [];
      return renderSessions(items);
    }
    if (quiet) return;
    if (r.network) { sessionsMessage('Không tải được danh sách.'); return setNotice('sessions', 'error', NETWORK_MSG); }
    if (await sessionExpired(r)) return sessionsMessage('Cần đăng nhập để xem.');
    if (r.status === 404 || r.status === 501) {
      sessionsMessage('Chưa có dữ liệu.');
      return setNotice('sessions', 'info', 'Danh sách phiên đăng nhập chưa được kết nối với máy chủ. Vui lòng quay lại sau.');
    }
    sessionsMessage('Không tải được danh sách.');
    setNotice('sessions', 'error', errMsg(r, 'Chưa tải được danh sách phiên đăng nhập. Vui lòng thử lại.'));
  }

  sessionsList.addEventListener('click', async (e) => {
    const btn = e.target.closest('[data-revoke]');
    if (!btn) return;
    setNotice('sessions', null);
    btn.disabled = true;
    btn.textContent = 'Đang đăng xuất…';
    const r = await api('revokeSession', { method: 'DELETE', params: { id: btn.dataset.revoke } });
    if (r.ok || r.status === 404) { // 404: phiên đã không còn
      setNotice('sessions', 'success', 'Đã đăng xuất thiết bị đó khỏi tài khoản.');
      return loadSessions({ quiet: true });
    }
    btn.disabled = false;
    btn.textContent = 'Đăng xuất';
    if (r.network) return setNotice('sessions', 'error', NETWORK_MSG);
    if (await sessionExpired(r)) return;
    setNotice('sessions', 'error', errMsg(r, 'Chưa đăng xuất được thiết bị này. Vui lòng thử lại.'));
  });

  $('[data-sessions-reload]').addEventListener('click', () => loadSessions());

  /* =========================================================
     Dữ liệu giả (chỉ khi mock bật + chạy ở localhost) — cho phần BE1 chưa bàn giao
       Đổi mật khẩu: nhập mật khẩu hiện tại là  Wrong@123  để thử trường hợp sai; còn lại thành công.
     ========================================================= */
  const mockDb = {
    sessions: [
      { id: 's1', browser: 'Chrome', os: 'Windows 11', device: 'Máy tính', createdAt: new Date(Date.now() - 20 * 60e3).toISOString(), current: true },
      { id: 's2', browser: 'Safari', os: 'iPhone', device: 'iPhone 15', createdAt: new Date(Date.now() - 26 * 3600e3).toISOString(), current: false },
      { id: 's3', browser: 'Firefox', os: 'Ubuntu', device: 'Máy tính', createdAt: new Date(Date.now() - 5 * 86400e3).toISOString(), current: false },
    ],
  };
  const wait = (ms) => new Promise((r) => setTimeout(r, ms));
  const reply = (status, data) => ({ ok: status >= 200 && status < 300, status, data, network: false });

  async function mockApi(key, { method, body, params }) {
    await wait(450);
    if (key === 'changePassword') {
      return body && body.currentPassword === 'Wrong@123'
        ? reply(400, { error: 'INVALID_CURRENT_PASSWORD', message: 'Mật khẩu hiện tại không đúng.' })
        : reply(200, { message: 'Đổi mật khẩu thành công.' });
    }
    if (key === 'sessions' && method === 'GET') return reply(200, mockDb.sessions.map((s) => ({ ...s })));
    if (key === 'revokeSession' && method === 'DELETE') {
      const found = mockDb.sessions.find((s) => String(s.id) === String(params.id));
      if (found && found.current) return reply(400, { error: 'CANNOT_REVOKE_CURRENT', message: 'Không thể thu hồi phiên hiện tại.' });
      mockDb.sessions = mockDb.sessions.filter((s) => String(s.id) !== String(params.id));
      return reply(200, { message: 'Đã thu hồi phiên.' });
    }
    return reply(404, null);
  }

  /* =========================================================
     Khởi tạo
     ========================================================= */
  function init() {
    loadProfile().then((ok) => {
      if (ok) loadSessions();
      else sessionsMessage('Cần đăng nhập để xem.');
    });
  }

  // Liên kết theo cấu hình (config.links); không khai báo thì giữ href mặc định trong HTML
  $$('[data-link]').forEach((a) => { const u = CONFIG.links[a.dataset.link]; if (u) a.setAttribute('href', u); });
  if (CONFIG.links.products) $('[data-search]').setAttribute('action', CONFIG.links.products);
  $('[data-page-alert-login]').setAttribute('href', CONFIG.loginUrl);

  // Menu "Tài khoản" ở header
  const menu = $('[data-menu]'), menuBtn = $('[data-menu-btn]'), menuList = $('.menu__list', menu);
  const setMenu = (open) => { menuList.hidden = !open; menuBtn.setAttribute('aria-expanded', String(open)); };
  menuBtn.addEventListener('click', () => setMenu(menuList.hidden));
  document.addEventListener('click', (e) => { if (!menu.contains(e.target)) setMenu(false); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && !menuList.hidden) { setMenu(false); menuBtn.focus(); } });

  $$('[data-icon]').forEach((el) => { el.innerHTML = icon(el.dataset.icon); });
  $$('[data-toggle-pw]').forEach((b) => {
    setPwVisible(b, false);
    b.addEventListener('click', () => setPwVisible(b, b.getAttribute('aria-pressed') !== 'true'));
  });
  $$('[data-strength-slot]').forEach(bindStrength);

  const activeMocks = Object.keys(MOCK).filter((k) => MOCK[k]);
  if (activeMocks.length) {
    const names = { changePassword: 'đổi mật khẩu', sessions: 'phiên đăng nhập' };
    document.body.append(h('p', { class: 'mock-badge' }, `Dữ liệu giả: ${activeMocks.map((k) => names[k] || k).join(', ')}`));
  }

  init();
})();
