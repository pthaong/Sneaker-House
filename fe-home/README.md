# FE1 — Trang chủ (Sneaker House, issue #15)

Trang: `index.html` — nhánh `feature/fe-homepage` → PR vào `develop`.
HTML/CSS/JS thuần, không thêm thư viện. Cùng bộ thiết kế với `fe-account` (font Be Vietnam Pro, đỏ `#E11D2A`, đen `#111`).

## Chạy thử (PowerShell)
    # Terminal 1 — backend (nếu đã có API của BE1)
    mvn spring-boot:run

    # Terminal 2 — frontend
    cd fe-home
    python -m http.server 5500
    # mở http://localhost:5500/index.html

- Dùng `localhost` cho cả trang và API (đừng trộn `127.0.0.1` với `localhost`) để cookie phiên hoạt động.
- Cổng `5500`/`5501` đã được `SecurityConfig` cho phép CORS.

## Cấu hình: `js/home-config.js` (một chỗ duy nhất)
| Khóa | Ý nghĩa |
|---|---|
| `apiBase` | Gốc API, mặc định `http://localhost:8080/api` |
| `mock` | `true` = dùng **dữ liệu giả** (`js/home-mock.js`) cho API BE1 **chưa bàn giao**. Chỉ chạy ở `localhost`; khi bật có nhãn "Dữ liệu giả" ở góc dưới trái. **Đặt `false` trước khi tạo PR và khi kiểm thử cuối** (issue #15 yêu cầu dữ liệu thật). Đăng nhập/đăng xuất luôn gọi API thật. |
| `endpoints` | Đường dẫn từng API. BE1 đổi tên thì chỉ sửa ở đây |
| `links`, `productUrl` | Đường dẫn sang trang khác (khóa = giá trị `data-link` trong `index.html`) |
| `filterOptions` | Size, màu, khoảng giá cho bộ lọc, số sản phẩm mỗi lần tải |
| `store` | Hotline, email, địa chỉ ở thanh trên và footer |

## Trạng thái API hiện tại
Trong `develop` mới có Auth / Profile / Account. **Chưa có** API danh mục, thương hiệu, banner, flash sale, sản phẩm, tìm kiếm, bộ sưu tập, tin tức, đánh giá, đăng ký nhận tin, giỏ hàng, yêu thích. Vì vậy các khu vực này đang chạy bằng mock (`mock: true`). Phần **thật**: `GET /api/profile` (tên trên header), `POST /api/auth/logout`.

## API contract ĐỀ XUẤT cho BE1 — cần BE1 xác nhận
Mọi request gửi `credentials: 'include'`; request ghi (POST/DELETE) gửi `X-XSRF-TOKEN` lấy từ `GET /api/auth/csrf`. Danh sách có thể là mảng, hoặc `{ items | content | data }`.
Các endpoint GET công khai cần được `permitAll()` trong `SecurityConfig` (hiện `anyRequest().authenticated()` sẽ trả 403 cho khách).

| Việc | Request | Response |
|---|---|---|
| Danh mục + mega-menu | `GET /api/categories` | `[{ id, name, slug, imageUrl, description, children:[{id,name,slug}], types:["Running"] }]` |
| Thương hiệu | `GET /api/brands` | `[{ id, name, slug, logoUrl }]` |
| Banner | `GET /api/home/banners` | `[{ id, position:"HERO"/"PROMO", title, subtitle, ctaText, link, imageUrl, tag? }]` |
| Flash Sale | `GET /api/flash-sale/current` | `{ endsAt (ISO), serverTime? (ISO), items:[sản phẩm + { stock, sold }] }` — 404 hoặc rỗng ⇒ FE ẩn khu vực |
| Nổi bật / mới / bán chạy | `GET /api/products/featured`, `/new`, `/best-sellers` | `[sản phẩm]` |
| Tìm kiếm + lọc | `GET /api/products?keyword&categoryId&brandId&minPrice&maxPrice&size&color&sort&page&sizePerPage` | `{ content, page, size, totalElements, totalPages, last }`; `sort` = `newest` / `best_selling` / `price_asc` / `price_desc`; `categoryId` và `brandId` sử dụng ID |
| Bộ sưu tập | `GET /api/collections` | `[{ id, name, slug, description, imageUrl }]` |
| Tin tức | `GET /api/news` | `[{ id, title, summary, imageUrl, publishedAt }]` |
| Đánh giá nổi bật | `GET /api/reviews/featured` | `[{ id, customerName, rating, content, productName }]` — rỗng ⇒ FE ẩn khu vực |
| Nhận ưu đãi | `POST /api/newsletter/subscribe` `{ email }` | `200` (`409` nếu đã đăng ký) |
| Giỏ hàng | `POST /api/cart/items` `{ productId, quantity }` | `200` |
| Yêu thích | `POST` / `DELETE /api/wishlist/{id}` | `200` |

Sản phẩm: `{ id, slug, name, code, brand (chuỗi hoặc {name}), price, salePrice, imageUrl, rating, reviewCount, stock|inStock }`. FE cũng nhận `discountPrice`, `image`, `thumbnail`, `averageRating`. `imageUrl` dạng `/uploads/...` được ghép với gốc của `apiBase`.
Lỗi nên có dạng `{ error, message }` (như các API hiện có). `404`/`501` ở giỏ hàng, yêu thích, đăng ký ⇒ FE báo "chưa được kết nối".

## Đối chiếu tiêu chí #15
- [x] Header + thanh ưu đãi, logo, tìm kiếm, tài khoản/yêu thích/giỏ hàng, 9 mục điều hướng; dùng lại đăng nhập/thông tin tài khoản (`/api/profile`, logout)
- [x] Mega-menu nền đen trong suốt + kính mờ: danh mục chính/con, loại giày, thương hiệu (logo nếu có), khoảng giá, banner; Desktop (hover/click/phím) và Mobile (toàn màn hình)
- [x] Banner chính tự chuyển slide (tạm dừng khi rê chuột/focus, tôn trọng `prefers-reduced-motion`), banner khuyến mãi, danh mục nổi bật, thương hiệu nổi bật
- [x] Flash Sale: đồng hồ đếm ngược theo `endsAt` (bù lệch giờ bằng `serverTime`), giá gốc/giá giảm/%, thanh đã bán, trạng thái còn hàng, nút Xem chi tiết / Thêm vào giỏ / Yêu thích
- [x] Sản phẩm nổi bật, mới về, bán chạy, bộ sưu tập, nút Xem tất cả
- [x] Tìm kiếm (tên/mã/thương hiệu), lọc danh mục/thương hiệu/giá/size/màu, sắp xếp, "Xem thêm", thông báo khi không có kết quả; đọc được tham số URL (`?category=nam&sort=newest#shop`)
- [x] Chatbox AI: nút nổi, mở/đóng/thu nhỏ, 4 gợi ý, responsive; có hook `window.SNEAKER_CHAT = { onSend }` để nối AI sau này (chưa tích hợp AI)
- [x] Tin tức, đánh giá (ẩn nếu không có dữ liệu), đăng ký nhận ưu đãi (kiểm tra email), footer
- [x] Responsive Desktop / Tablet / Mobile (đã chụp thử 1440px và 390px, không tràn ngang); loading (skeleton), dữ liệu rỗng, lỗi API/mạng có nút "Thử lại"
- [x] Không đụng tới `fe-auth` / `fe-account`
- [ ] **Chưa làm được:** nối API thật cho các khu vực trên — chờ BE1 bàn giao (xem bảng trên), sau đó đặt `mock: false` và kiểm thử lại với dữ liệu SQL Server
- [ ] Trang đích còn thiếu (sản phẩm, giỏ hàng, yêu thích, đơn hàng...): bấm vào sẽ 404 cho tới khi module đó có; sửa đường dẫn ở `links`

## Khác bản thiết kế / lưu ý
- Không hiển thị số trên giỏ hàng/yêu thích (chưa có module, không dùng số giả).
- Ảnh trong mock lấy từ ảnh poster của `fe-auth`; tên và giá sản phẩm mock chỉ để minh họa.
- Logo thương hiệu: khi API chưa có `logoUrl`, hiển thị tên thương hiệu bằng chữ.

## Tạo PR (PowerShell) — tự chạy khi đã sẵn sàng
    git checkout develop
    git pull
    git checkout -b feature/fe-homepage
    # chép thư mục fe-home vào gốc repo, đặt mock: false trong js/home-config.js, rồi:
    git add fe-home
    git commit -m "feat(fe1): add homepage"
    git push -u origin feature/fe-homepage
Sau đó tạo Pull Request vào `develop`, chuyển công việc sang **Review**, và **không tự merge**.
