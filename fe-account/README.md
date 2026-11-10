# FE1 — Bảo mật tài khoản (Sneaker House, issue #10)

Trang: `account-security.html` — nhánh `feature/fe-account-security` → PR vào `develop`.
HTML/CSS/JS thuần, không thêm thư viện. Giao diện theo **bản thiết kế của trưởng nhóm** (header cửa hàng, breadcrumb, sidebar "Tài khoản của tôi", các khối dạng thẻ). Font Be Vietnam Pro, đỏ `#E11D2A`.

## Chạy thử (PowerShell)
    # Terminal 1 — backend (đã đặt biến môi trường DB_*, MAIL_*, GOOGLE_CLIENT_ID như README gốc)
    mvn spring-boot:run

    # Terminal 2 — frontend
    cd fe-account
    python -m http.server 5500
    # mở http://localhost:5500/account-security.html

- Dùng **`localhost`** cho cả trang và API (đừng trộn `127.0.0.1` với `localhost`), nếu không trình duyệt có thể không gửi cookie phiên.
- Cổng `5500`/`5501` đã được `SecurityConfig` cho phép (CORS). Cổng khác thì BE1 phải thêm.
- Cần đăng nhập trước (trang `fe-auth/login.html`), vì trang này dùng cookie phiên `JSESSIONID`.

## Cấu hình: `js/account-config.js` (một chỗ duy nhất)
| Khóa | Ý nghĩa |
|---|---|
| `apiBase` | Gốc API, mặc định `http://localhost:8080/api` |
| `endpoints` | Đường dẫn từng API. BE1 đổi tên thì chỉ sửa ở đây |
| `changePasswordMethod` | `POST` (đổi thành `PUT`/`PATCH` nếu BE1 chọn khác) |
| `mock.changePassword`, `mock.sessions` | `true` = dùng dữ liệu giả cho phần **BE1 chưa bàn giao**. Chỉ chạy ở `localhost`. Khi bật, góc dưới phải có nhãn "Dữ liệu giả: …" |
| `loginUrl` | Trang đăng nhập (mặc định `../fe-auth/login.html`) |
| `links` | Ghi đè đường dẫn các trang/module khác (trang chủ, sản phẩm, giỏ hàng, đơn hàng, yêu thích, địa chỉ, mã giảm giá, thông báo, cài đặt, danh mục...). Chưa khai báo thì dùng href mặc định trong HTML — nhiều trang trong số này chưa tồn tại nên bấm vào sẽ 404 cho đến khi module đó có |

**Trước khi tạo PR và khi kiểm thử cuối:** đặt cả hai `mock.*` thành `false` (issue #10 yêu cầu không dùng dữ liệu giả).

## Phần nào đang gọi API thật, phần nào còn mock
| Khu vực | Nguồn dữ liệu hiện tại |
|---|---|
| Phương thức đăng nhập | **Thật** — `GET /api/profile` (`loginMethod` = `EMAIL_PASSWORD` / `GOOGLE` / `BOTH`, `googleLinked`) |
| Khôi phục mật khẩu (OTP) | **Thật** — `/api/auth/forgot-password`, `/verify-otp`, `/resend-otp`, `/reset-password` |
| Header, email hiển thị, Đăng xuất | **Thật** — `GET /api/profile`, `POST /api/auth/logout` |
| Đổi mật khẩu | **Mock** cho đến khi BE1 xong issue #11 |
| Danh sách / thu hồi phiên | **Mock** cho đến khi BE1 xong issue #11 |
| Lưu ý bảo mật | Nội dung tĩnh |

Thử mock đổi mật khẩu: nhập mật khẩu hiện tại là `Wrong@123` để thấy lỗi "sai mật khẩu hiện tại"; giá trị khác sẽ thành công.

## API contract ĐỀ XUẤT cho BE1 (issue #11) — cần BE1 xác nhận
Mọi request gửi `credentials: 'include'` (cookie phiên) và header `X-XSRF-TOKEN` lấy từ `GET /api/auth/csrf`.

| Việc | Request | Response thành công |
|---|---|---|
| Đổi mật khẩu | `POST /api/account/password` `{ "currentPassword", "newPassword" }` | `200` + `{ "message" }` |
| Danh sách phiên | `GET /api/account/sessions` | `200` + `[ { "id", "device", "browser", "os", "createdAt", "current": true/false } ]` |
| Thu hồi phiên khác | `DELETE /api/account/sessions/{id}` | `200` (hoặc `204`) |

Lỗi nên có dạng `{ "error": "MÃ_LỖI", "message": "..." }` (giống các API hiện có). FE nhận biết:
- Sai mật khẩu hiện tại: status `400`/`401`/`403` và `error` chứa `CURRENT`/`WRONG`/`INCORRECT`, hoặc `message` chứa "hiện tại" → hiện lỗi dưới ô "Mật khẩu hiện tại".
- Mật khẩu mới trùng mật khẩu cũ: `error` chứa `SAME`/`REUSE` → lỗi dưới ô "Mật khẩu mới".
- `429` → hiện `message` của BE. `404`/`501` → báo "chưa kết nối với máy chủ".
- Danh sách phiên: FE cũng chấp nhận `{ "sessions": [...] }`, và các tên `sessionId`, `isCurrent`, `loginAt`, `deviceName`, `platform`.

Lưu ý cho BE1:
- Hiện chưa đăng nhập thì BE trả **403** (không phải 401) vì chưa cấu hình entry point. FE đã xử lý cả hai, nhưng 401 sẽ rõ nghĩa hơn.
- Spec issue #8 (FE2) ghi `/api/users/profile`, còn code BE là `/api/profile`. FE1 đang dùng `/api/profile` theo code thật.
- Tài khoản chỉ có Google (`loginMethod = GOOGLE`): FE ẩn form đổi mật khẩu và hướng người dùng sang mục Khôi phục mật khẩu. Hiện `reset-password` của BE đặt được mật khẩu cho tài khoản này; nếu BE1 làm khác ở #11 thì báo FE1 sửa lại câu chữ.

## Đối chiếu tiêu chí #10
- [x] Trang Bảo mật tài khoản, 5 khu vực, bố cục theo bản thiết kế; responsive Desktop / Tablet / Mobile (đã chụp thử 1672px và 390px, không tràn ngang)
- [x] Đổi mật khẩu: 3 trường, ẩn/hiện, kiểm tra bắt buộc + độ mạnh + khớp + khác mật khẩu cũ, nút Hủy/Cập nhật, trạng thái đang xử lý/thành công/thất bại, không lưu mật khẩu vào localStorage, không đưa mật khẩu vào thông báo lỗi
- [x] Phương thức đăng nhập: lấy từ BE thật; không có nút hủy liên kết Google
- [x] Khôi phục mật khẩu: hiển thị email, gửi OTP, nhập/xác minh OTP, mật khẩu mới, báo OTP sai/hết hạn, gửi lại (chờ 60 giây); OTP chỉ nằm trong ô nhập
- [x] Phiên đăng nhập: thiết bị, trình duyệt, thời gian, nhãn "Phiên hiện tại", nút Đăng xuất cho phiên khác, tải lại danh sách sau khi thu hồi
- [x] Lưu ý bảo mật
- [x] Xử lý loading, validation, lỗi API, lỗi mạng, phiên hết hạn (hiện thông báo + nút "Đăng nhập lại")
- [ ] **Chưa làm được:** nối API thật cho đổi mật khẩu và phiên đăng nhập — chờ BE1 bàn giao #11, sau đó tắt mock và kiểm thử lại
- [x] Header, Navbar, Sidebar, breadcrumb nằm ngay trong trang này (trang Thông tin cá nhân của FE2 chưa có trong `develop`). Khi FE2 merge, thống nhất tách thành thành phần dùng chung để hai trang không lặp mã

## Khác bản thiết kế (cố ý) — báo trưởng nhóm nếu muốn đổi
- **Không có số trên giỏ hàng / mục Thông báo** (bản thiết kế ghi "3"): chưa có module giỏ hàng, thông báo nên không hiển thị số giả.
- **Logo**: dùng `assets/logo-dark.png` của dự án (bố cục xếp chồng). Có file logo ngang như thiết kế thì thay vào cùng tên.
- **Khôi phục mật khẩu** có thêm dòng "Email đã đăng ký: …" (issue #10 yêu cầu hiển thị email) và các bước OTP mở ngay trong khối.
- **Phiên đăng nhập** hiển thị thời gian đăng nhập cho cả phiên hiện tại, và có nút nhỏ "Tải lại danh sách".
- Lưu ý bảo mật nhắc "ký tự đặc biệt" theo thiết kế, nhưng chỉ **bắt buộc** 8–72 ký tự, chữ hoa, chữ thường, số (4 yêu cầu hiển thị trên form, khớp backend).
- "Danh mục sản phẩm", nút Yêu thích, Giỏ hàng là liên kết sang module khác (xem `links`).

## Tạo PR (PowerShell) — tự chạy khi đã sẵn sàng
    git checkout develop
    git pull
    git checkout -b feature/fe-account-security
    # chép thư mục fe-account vào gốc repo, rồi:
    git add fe-account
    git commit -m "feat(fe1): add account security page"
    git push -u origin feature/fe-account-security
Sau đó tạo Pull Request vào `develop`, chuyển công việc sang **Review**, và **không tự merge**.
