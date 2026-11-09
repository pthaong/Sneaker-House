/* ==========================================================
   Sneaker House – Trang Thông tin cá nhân
   Tích hợp API theo "Sneaker House API Contract (Authentication + Profile)"
   - Xác thực: session cookie (JSESSIONID) -> fetch với credentials:'include'
   - Dữ liệu lấy thật từ Backend (SQL Server), KHÔNG dùng dữ liệu giả
   ========================================================== */
const CONFIG = {
  API_BASE: 'http://localhost:8080',
  ENDPOINTS: {
    profile: '/api/profile',
    avatar: '/api/profile/avatar',    // POST multipart/form-data, field "file"
    logout: '/api/auth/logout',             // POST: hủy session (Authentication đã có)
  },
  // NOTE: tài liệu API Contract ghi /api/profile; yêu cầu công việc ghi /api/users/profile.
  // Nếu BE chạy theo tài liệu thì đổi 2 dòng profile/avatar ở trên thành '/api/profile' và '/api/profile/avatar'.
  LOGIN_PAGE: '../fe-auth/login.html',            // trang đăng nhập của nhóm (Authentication)
  REQUEST_TIMEOUT: 15000,                   // ms
  SESSION_REDIRECT_DELAY: 1800,             // ms: chờ để người dùng đọc thông báo hết phiên
  MAX_AVATAR_SIZE: 5 * 1024 * 1024,
  AVATAR_TYPES: ['image/jpeg', 'image/png'],
};

/* Dữ liệu theo contract:
   GET  profile -> { id, fullName, email, phone, birthDate:"YYYY-MM-DD", gender, avatarUrl,
                     loginMethods:["EMAIL_PASSWORD","GOOGLE"], googleLinked }
   PUT  profile body { fullName, phone, birthDate, gender } -> { message, profile:{...} }
   POST avatar  (FormData "file") -> { message, avatarUrl }
   Lỗi -> { timestamp, status, error, message, details:{ field: "thông báo" } } */

const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];

const form = $('#profileForm');
const el = {
  fullName: $('#fullName'), email: $('#email'), phone: $('#phone'), dob: $('#dob'),
  avatarImg: $('#avatarPreview'), avatarInput: $('#avatarInput'), avatarError: $('#avatarError'),
  btnSave: $('#btnSave'), btnCancel: $('#btnCancel'), btnCam: $('#btnCam'),
  btnChangeAvatar: $('#btnChangeAvatar'), toast: $('#toast'),
};

let original = null;       // hồ sơ gần nhất lấy từ Backend (dùng cho Hủy)
let pendingAvatar = null;  // File ảnh đang chờ upload
let previewUrl = null;     // blob URL xem trước
let loaded = false;        // đã tải hồ sơ thành công chưa
let busy = false;          // đang gọi API
let sessionExpired = false;

/* ---------- Toast ---------- */
let toastTimer;
function toast(msg, type = '', ms = 3500) {
  el.toast.textContent = msg;
  el.toast.className = `toast show ${type}`;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (el.toast.className = 'toast'), ms);
}

/* ---------- Phiên đăng nhập ---------- */
function handleSessionExpired() {
  if (sessionExpired) return;
  sessionExpired = true;
  toast('Phiên đăng nhập đã hết hạn. Đang chuyển về trang đăng nhập...', 'error', 5000);
  form.classList.remove('is-loading');
  [el.btnSave, el.btnCancel, el.btnCam, el.btnChangeAvatar].forEach(b => (b.disabled = true));
  setTimeout(() => {
    location.href = `${CONFIG.LOGIN_PAGE}?redirect=${encodeURIComponent(location.pathname.split('/').pop() || 'profile.html')}`;
  }, CONFIG.SESSION_REDIRECT_DELAY);
}

/* ---------- Gọi API (session cookie) ---------- */
class ApiError extends Error {
  constructor(message, status = 0, details = null) {
    super(message); this.status = status; this.details = details; this.handled = false;
  }
}

const STATUS_MESSAGES = {
  400: 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.',
  403: 'Bạn không có quyền thực hiện thao tác này.',
  404: 'Không tìm thấy dữ liệu yêu cầu.',
  409: 'Dữ liệu bị trùng hoặc xung đột. Vui lòng kiểm tra lại.',
  413: 'Ảnh vượt quá dung lượng tối đa 5MB.',
  415: 'Định dạng ảnh không được hỗ trợ (chỉ JPG, PNG).',
  429: 'Bạn thao tác quá nhanh. Vui lòng thử lại sau.',
  500: 'Máy chủ gặp lỗi. Vui lòng thử lại sau.',
};

const getCookie = (name) => {
  const m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
  return m ? decodeURIComponent(m[1]) : null;
};

async function api(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = { Accept: 'application/json', ...(options.headers || {}) };
  // JSON -> tự đặt Content-Type; FormData -> để trình duyệt tự thêm boundary
  if (options.body && !(options.body instanceof FormData)) headers['Content-Type'] = 'application/json';
  // CSRF (chỉ gửi nếu Spring Security có cấp cookie XSRF-TOKEN)
if (method !== 'GET') {
  const csrfResponse = await fetch(
    CONFIG.API_BASE + '/api/auth/csrf',
    { credentials: 'include' }
  );

  if (!csrfResponse.ok) {
    throw new ApiError('Không thể lấy CSRF Token.', csrfResponse.status);
  }

  const csrfData = await csrfResponse.json();

  headers[csrfData.headerName || 'X-XSRF-TOKEN'] = csrfData.token;
}


  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), CONFIG.REQUEST_TIMEOUT);
  let res;
  try {
    res = await fetch(CONFIG.API_BASE + path, { ...options, method, headers, credentials: 'include', signal: ctrl.signal });
  } catch (e) {
    throw new ApiError(e.name === 'AbortError'
      ? 'Máy chủ phản hồi quá lâu. Vui lòng thử lại.'
      : 'Không thể kết nối tới máy chủ. Hãy kiểm tra mạng hoặc Backend đã chạy chưa.');
  } finally {
    clearTimeout(timer);
  }

  // Hết phiên: 401, hoặc Spring redirect sang trang login (trả HTML)
  const isJson = (res.headers.get('content-type') || '').includes('json');
if (
  res.status === 401 ||
  (res.status === 403 &&
    method === 'GET' &&
    path === CONFIG.ENDPOINTS.profile) ||
  (res.redirected && !isJson)
) {

    handleSessionExpired();
    const err = new ApiError('Phiên đăng nhập đã hết hạn.', 401); err.handled = true; throw err;
  }

  const data = isJson ? await res.json().catch(() => ({})) : {};
  if (!res.ok) {
    throw new ApiError(data.message || STATUS_MESSAGES[res.status] || 'Có lỗi xảy ra, vui lòng thử lại.',
      res.status, data.details || data.errors || null);
  }
  if (!isJson) throw new ApiError('Phản hồi từ máy chủ không hợp lệ.', res.status);
  return data;
}

/* ---------- Helpers UI ---------- */
// Tên field của BE -> data-error-for trên giao diện
const FIELD_MAP = { birthDate: 'dob', dateOfBirth: 'dob', fullName: 'fullName', phone: 'phone', gender: 'gender' };

function setFieldError(name, msg) {
  const p = $(`[data-error-for="${FIELD_MAP[name] || name}"]`);
  if (!p) return false;
  p.textContent = msg || '';
  const field = p.closest('.field');
  if (field) field.classList.toggle('invalid', !!msg);
  return true;
}
const clearErrors = () => {
  $$('[data-error-for]').forEach(p => setFieldError(p.dataset.errorFor, ''));
  el.avatarError.textContent = '';
};

function setBusy(state, label) {
  busy = state;
  form.classList.toggle('is-loading', state && label === 'load');
  el.btnSave.disabled = state || !loaded;
  el.btnCancel.disabled = state;
  el.btnCam.disabled = state;
  el.btnChangeAvatar.disabled = state;
  el.btnSave.textContent = state && label === 'save' ? 'Đang lưu...' : 'Lưu thay đổi';
}

const getGender = () => (form.querySelector('input[name="gender"]:checked') || {}).value || '';
const setGender = (v) => $$('input[name="gender"]').forEach(r => (r.checked = r.value === v));

function resolveAvatarUrl(url, bust) {
  if (!url) return '';
  const abs = /^(https?:|data:|blob:)/i.test(url) ? url : CONFIG.API_BASE + (url.startsWith('/') ? '' : '/') + url;
  return bust ? abs + (abs.includes('?') ? '&' : '?') + 't=' + Date.now() : abs;
}

/* ---------- Render dữ liệu từ Backend ---------- */
function render(p, { bustAvatar = false } = {}) {
  el.fullName.value = p.fullName || '';
  el.email.value = p.email || '';
  el.phone.value = p.phoneNumber || '';
  el.dob.value = (p.birthDate || '').slice(0, 10);
  setGender(p.gender || '');
  const src = resolveAvatarUrl(p.avatarUrl, bustAvatar);
  if (src) el.avatarImg.src = src; // không có ảnh -> giữ ảnh mặc định
  renderMethods(p);
}


function renderMethods(p) {
  const method = p.loginMethod || '';

  const state = {
    PASSWORD: method === 'EMAIL_PASSWORD' || method === 'BOTH',
    GOOGLE: method === 'GOOGLE' || method === 'BOTH'
  };

  $$('#methodList .method').forEach(li => {
    const type = li.dataset.type;
    const on = !!state[type];
    const tag = $('[data-status]', li);

    tag.textContent = on
      ? (type === 'PASSWORD' ? 'Đang sử dụng' : 'Đã liên kết')
      : 'Chưa liên kết';

    tag.classList.toggle('is-off', !on);
  });
}


/* ---------- Validate phía FE ---------- */
const PHONE_RE = /^(0|\+84)(3|5|7|8|9)\d{8}$/;

function validate() {
  clearErrors();
  let ok = true;
  const bad = (n, m) => { setFieldError(n, m); ok = false; };

  const name = el.fullName.value.trim().replace(/\s+/g, ' ');
  if (!name) bad('fullName', 'Vui lòng nhập họ và tên.');
  else if (name.length < 2) bad('fullName', 'Họ và tên phải có ít nhất 2 ký tự.');
  else if (/\d/.test(name)) bad('fullName', 'Họ và tên không được chứa chữ số.');

  const phone = el.phone.value.replace(/[\s.-]/g, '');
  if (!phone) bad('phone', 'Vui lòng nhập số điện thoại.');
  else if (!PHONE_RE.test(phone)) bad('phone', 'Số điện thoại không hợp lệ (VD: 0901234567).');

  if (!el.dob.value) bad('dob', 'Vui lòng chọn ngày sinh.');
  else {
    const d = new Date(el.dob.value), now = new Date();
    if (isNaN(d) || d > now) bad('dob', 'Ngày sinh không được ở tương lai.');
    else if (d.getFullYear() < 1900) bad('dob', 'Ngày sinh không hợp lệ.');
    else if (now.getFullYear() - d.getFullYear() < 10) bad('dob', 'Bạn phải từ 10 tuổi trở lên.');
  }

  if (!getGender()) bad('gender', 'Vui lòng chọn giới tính.');

  if (!ok) { const first = $('.field.invalid input'); if (first) first.focus(); }
  return ok;
}

['fullName', 'phone', 'dob'].forEach(id => el[id].addEventListener('input', () => setFieldError(id, '')));
$$('input[name="gender"]').forEach(r => r.addEventListener('change', () => setFieldError('gender', '')));

/* ---------- Ảnh đại diện: chọn + xem trước ---------- */
const openPicker = () => el.avatarInput.click();
el.btnChangeAvatar.addEventListener('click', openPicker);
el.btnCam.addEventListener('click', openPicker);

function discardPendingAvatar() {
  pendingAvatar = null;
  el.avatarInput.value = '';
  if (previewUrl) { URL.revokeObjectURL(previewUrl); previewUrl = null; }
}

el.avatarInput.addEventListener('change', () => {
  const file = el.avatarInput.files[0];
  el.avatarError.textContent = '';
  if (!file) return;
  if (!CONFIG.AVATAR_TYPES.includes(file.type)) {
    el.avatarError.textContent = 'Chỉ hỗ trợ định dạng JPG, PNG.'; el.avatarInput.value = ''; return;
  }
  if (file.size > CONFIG.MAX_AVATAR_SIZE) {
    el.avatarError.textContent = 'Ảnh vượt quá 5MB, vui lòng chọn ảnh nhỏ hơn.'; el.avatarInput.value = ''; return;
  }
  if (previewUrl) URL.revokeObjectURL(previewUrl);
  pendingAvatar = file;
  previewUrl = URL.createObjectURL(file);
  el.avatarImg.src = previewUrl; // xem trước, chỉ gửi lên khi bấm "Lưu thay đổi"
});

/* ---------- GET: tải hồ sơ ---------- */
async function loadProfile({ silent = false } = {}) {
  setBusy(true, 'load');
  try {
    const p = await api(CONFIG.ENDPOINTS.profile);
    original = p;
    loaded = true;
    render(p, { bustAvatar: silent });
    return p;
  } catch (e) {
    if (!e.handled) {
      toast(e.message + ' Bấm "Hủy" để tải lại.', 'error', 6000);
    }
    return null;
  } finally {
    setBusy(false);
  }
}

/* ---------- PUT + POST avatar: lưu thay đổi ---------- */
form.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (busy || !loaded || !validate()) return;

  const payload = {
    fullName: el.fullName.value.trim().replace(/\s+/g, ' '),
    phoneNumber: el.phone.value.replace(/[\s.-]/g, ''),
    birthDate: el.dob.value,
    gender: getGender(),
  }; // không gửi email (chỉ đọc)

  setBusy(true, 'save');
  try {
    // 1) Upload ảnh đại diện nếu có chọn ảnh mới
    if (pendingAvatar) {
      const fd = new FormData();
      fd.append('file', pendingAvatar);
      try {
        await api(CONFIG.ENDPOINTS.avatar, { method: 'POST', body: fd });
      } catch (err) {
        if (!err.handled) { el.avatarError.textContent = err.message; toast('Không thể cập nhật ảnh đại diện. Thông tin chưa được lưu.', 'error'); }
        throw Object.assign(err, { handled: true });
      }
    }

    // 2) Cập nhật thông tin cá nhân
    await api(CONFIG.ENDPOINTS.profile, { method: 'PUT', body: JSON.stringify(payload) });
    discardPendingAvatar();

    // 3) Tải lại từ Backend để xác nhận dữ liệu đã lưu
    const fresh = await loadProfile({ silent: true });
    if (!fresh) { toast('Đã lưu, nhưng chưa tải lại được dữ liệu mới. Hãy tải lại trang để kiểm tra.', 'error', 6000); return; }

    const same = fresh.fullName === payload.fullName && (fresh.phoneNumber || '') === payload.phoneNumber &&
                 (fresh.birthDate || '').slice(0, 10) === payload.birthDate && fresh.gender === payload.gender;
    if (same) toast('Cập nhật thông tin thành công!', 'success');
    else toast('Đã lưu nhưng dữ liệu máy chủ trả về khác dữ liệu bạn nhập. Vui lòng kiểm tra lại.', 'error', 6000);
  } catch (err) {
    if (err.handled) return;
    // Lỗi theo từng ô (400/409 có details)
    let shown = false;
    if (err.details && typeof err.details === 'object') {
      Object.entries(err.details).forEach(([k, v]) => { if (setFieldError(k, String(v))) shown = true; });
    }
    toast(err.message, 'error');
    if (shown) { const first = $('.field.invalid input'); if (first) first.focus(); }
  } finally {
    setBusy(false);
  }
});

/* ---------- Hủy ---------- */
el.btnCancel.addEventListener('click', async () => {
  if (busy) return;
  discardPendingAvatar();
  clearErrors();
  if (!loaded || !original) { await loadProfile(); return; }   // lần tải đầu lỗi -> thử lại
  render(original);
  toast('Đã hủy các thay đổi.');
});

/* ---------- Đăng xuất (dùng API Authentication) ---------- */
$('#btnLogout').addEventListener('click', async () => {
  try { await api(CONFIG.ENDPOINTS.logout, { method: 'POST' }); } catch (_) { /* dù lỗi vẫn rời trang */ }
  if (!sessionExpired) location.href = CONFIG.LOGIN_PAGE;
});

/* ---------- Khởi động ---------- */
el.btnSave.disabled = true;
loadProfile();
