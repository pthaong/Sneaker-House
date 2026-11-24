# FE Product Catalog — Sneaker House

Phạm vi: danh sách sản phẩm, chi tiết sản phẩm, tìm kiếm/lọc/sắp xếp. Không triển khai lại giỏ hàng hoặc thanh toán.

## Chạy thử

1. Chạy Backend Spring Boot trên `http://localhost:8080` và kết nối SQL Server có dữ liệu thật.
2. Từ thư mục gốc repository, chạy:

   ```powershell
   py -m http.server 5500
   ```

3. Mở `http://localhost:5500/fe-product-catalog/products.html`.
4. Tìm kiếm tại `http://localhost:5500/fe-product-catalog/search.html`.
5. Trang chi tiết được mở từ thẻ sản phẩm hoặc qua `product.html?id=<id>`.

API base được cấu hình tại `js/catalog-config.js`. Các request dùng `credentials: include`; request ghi sử dụng token từ `GET /api/auth/csrf`.

## API được tích hợp

| Chức năng | API | Ghi chú |
|---|---|---|
| Danh mục / mega-menu | `GET /api/categories/tree` | Cần nhánh BE Product Catalog |
| Thương hiệu | `GET /api/brands` | Cần nhánh BE Product Catalog |
| Danh sách / tìm kiếm / lọc / phân trang | `GET /api/products` | Query: `keyword`, `categoryId`, `brandId`, `minPrice`, `maxPrice`, `size`, `color`, `sort`, `page`, `sizePerPage` |
| Chi tiết + biến thể | `GET /api/product-details/{id}` hoặc `/slug/{slug}` | Response có `images`, `sizes`, `colors`, `variants` |
| Sản phẩm liên quan | `GET /api/product-details/{id}/related` | Dữ liệu thật từ Backend |
| Đánh giá | `GET /api/products/{id}/reviews` và `/summary` | Hiển thị trạng thái rỗng nếu chưa có đánh giá |
| Thêm vào giỏ | `POST /api/cart/items` | Body: `{ productId, quantity, size, color }`; dùng API giỏ hàng của BE, không tạo giỏ hàng mới |
| Phiên đăng nhập | `GET /api/profile`, `POST /api/auth/logout` | Hiển thị tài khoản và xử lý yêu cầu đăng nhập |

## Các điểm phụ thuộc cần xác nhận trước nghiệm thu

- Giao diện dùng chung CSS, logo, ảnh và Header/Navbar/mega-menu từ `fe-home` (nhánh `feature/fe-homepage`). Thư mục `fe-home` có trong ZIP nguồn người dùng gửi nhưng chưa nằm trong `develop`; cần merge PR FE Homepage trước hoặc đồng bộ thư mục này vào nhánh đích trước khi nghiệm thu catalog. Nhánh catalog không thêm/sửa lại trang chủ để tránh mở rộng phạm vi.

- Nhánh `feature/be-product-catalog` có các API danh mục, thương hiệu, chi tiết sản phẩm, biến thể, đánh giá và controller thêm giỏ. Các API này chưa có trong `develop` tại thời điểm tạo nhánh FE này; cần merge PR Backend tương ứng trước khi nghiệm thu tích hợp.
- SecurityConfig trong nhánh Backend hiện chưa permit công khai `GET /api/product-details/**`; BE cần bổ sung matcher GET này vào nhóm public trước khi khách chưa đăng nhập có thể xem chi tiết sản phẩm.
- API danh sách hiện hỗ trợ từ khóa, danh mục, thương hiệu, khoảng giá, size, màu, sắp xếp và phân trang. API chưa có tham số lọc riêng `newOnly` hoặc `inStock`; hai lựa chọn này được hiển thị nhưng khóa lại, không giả lập kết quả lọc ở Frontend.
- Response chi tiết hiện tạo `images` từ một `imageUrl`, nên Frontend hỗ trợ thư viện nhiều ảnh nhưng số ảnh thực tế phụ thuộc dữ liệu Backend.
- Chưa thấy API wishlist trong hợp đồng Backend hiện tại. Nút yêu thích được giữ trong UI và thông báo rõ khi API chưa được cung cấp; không ghi dữ liệu giả.
- Source hiện tại chưa xác định được URL/file trang giỏ hàng FE2; Header và nút Mua ngay không tự đoán đường dẫn hoặc tạo lại trang giỏ. Sau khi FE2 cung cấp route, cập nhật link trong `catalog-common.js`.
- API danh sách sản phẩm hiện không trả `rating`/`reviewCount`; đánh giá được lấy từ API review ở trang chi tiết.
- `Mua ngay` và `Thêm vào giỏ` gọi cùng API thêm giỏ của FE2/BE. Trang giỏ hàng/luồng thanh toán vẫn thuộc phạm vi FE2.

## Kiểm tra

- JavaScript: kiểm tra cú pháp bằng `node --check`.
- Frontend không chứa bộ dữ liệu sản phẩm giả; trạng thái loading, rỗng, lỗi API và hết hàng được xử lý ở giao diện.
- Cần chạy thử với Backend + SQL Server thực tế để xác nhận CORS, session/CSRF, dữ liệu ảnh, tồn kho và thêm giỏ hàng.
