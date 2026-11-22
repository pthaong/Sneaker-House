-- Sneaker House - Homepage development/test seed
-- Version: 001 | SQL Server
-- Run after V001__homepage_nvarchar.sql.
-- DEV/TEST ONLY: never run this seed against the production database.
-- Images below use DEMO paths. Frontend must provide matching files, or replace URLs.
-- Idempotent by brand name, category slug, product SKU, and content title.

-- IMPORTANT:
-- Chi chay tren database DEV/TEST.
-- Trong SSMS, chon database DEV/TEST truoc khi Execute.
-- Thay placeholder bang ten database DEV/TEST dang su dung.
-- Tuyet doi khong chay tren database Production.

SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @ExpectedDatabase SYSNAME =
    N'REPLACE_WITH_DEV_TEST_DATABASE_NAME';

IF @ExpectedDatabase = N'REPLACE_WITH_DEV_TEST_DATABASE_NAME'
   OR DB_NAME() <> @ExpectedDatabase
BEGIN
    THROW 51001, 'STOP: Database DEV/TEST khong hop le. Huy Seed.', 1;
END;

BEGIN TRY
    BEGIN TRANSACTION;

    -- 1. BRANDS
    INSERT INTO dbo.brands (name, logo_url, active)
    SELECT v.name, v.logo_url, 1
    FROM (VALUES
        (N'Nike', '/images/demo/brands/nike.png'),
        (N'Adidas', '/images/demo/brands/adidas.png'),
        (N'New Balance', '/images/demo/brands/new-balance.png'),
        (N'Puma', '/images/demo/brands/puma.png'),
        (N'Converse', '/images/demo/brands/converse.png')
    ) AS v(name, logo_url)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.brands b WHERE b.name = v.name);

    -- 2. PARENT CATEGORIES
    INSERT INTO dbo.categories (name, slug, image_url, parent_id, active)
    SELECT v.name, v.slug, v.image_url, NULL, 1
    FROM (VALUES
        (N'Giày nam', 'giay-nam', '/images/demo/categories/giay-nam.jpg'),
        (N'Giày nữ', 'giay-nu', '/images/demo/categories/giay-nu.jpg'),
        (N'Giày unisex', 'giay-unisex', '/images/demo/categories/giay-unisex.jpg')
    ) AS v(name, slug, image_url)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.categories c WHERE c.slug = v.slug);

    -- 3. CHILD CATEGORIES - links to actual parent IDs
    INSERT INTO dbo.categories (name, slug, image_url, parent_id, active)
    SELECT v.name, v.slug, v.image_url, p.id, 1
    FROM (VALUES
        (N'Giày chạy bộ nam', 'chay-bo-nam', '/images/demo/categories/chay-bo-nam.jpg', 'giay-nam'),
        (N'Giày bóng rổ nam', 'bong-ro-nam', '/images/demo/categories/bong-ro-nam.jpg', 'giay-nam'),
        (N'Giày thời trang nữ', 'thoi-trang-nu', '/images/demo/categories/thoi-trang-nu.jpg', 'giay-nu'),
        (N'Giày chạy bộ nữ', 'chay-bo-nu', '/images/demo/categories/chay-bo-nu.jpg', 'giay-nu'),
        (N'Sneaker cổ thấp', 'sneaker-co-thap', '/images/demo/categories/sneaker-co-thap.jpg', 'giay-unisex'),
        (N'Sneaker cổ cao', 'sneaker-co-cao', '/images/demo/categories/sneaker-co-cao.jpg', 'giay-unisex')
    ) AS v(name, slug, image_url, parent_slug)
    JOIN dbo.categories p ON p.slug = v.parent_slug
    WHERE NOT EXISTS (SELECT 1 FROM dbo.categories c WHERE c.slug = v.slug);

    -- 4. HOME BANNERS (do not overwrite existing security-test banner)
    INSERT INTO dbo.home_banners
        (title, image_url, target_url, banner_type, [position], active, created_at)
    SELECT v.title, v.image_url, v.target_url, v.banner_type, v.position_value, 1, SYSDATETIME()
    FROM (VALUES
        (N'Khám phá bộ sưu tập Sneaker mới', '/images/demo/banners/main-new.jpg', '/products?sort=newest', 'MAIN', 10),
        (N'Flash Sale: Ưu đãi giày thể thao', '/images/demo/banners/main-sale.jpg', '/products?sort=price_asc', 'MAIN', 20),
        (N'Phong cách năng động mỗi ngày', '/images/demo/banners/advert.jpg', '/products', 'AD', 30)
    ) AS v(title, image_url, target_url, banner_type, position_value)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.home_banners h WHERE h.title = v.title);

    -- 5. COLLECTIONS
    INSERT INTO dbo.home_collections
        (title, description, image_url, target_url, active)
    SELECT v.title, v.description, v.image_url, v.target_url, 1
    FROM (VALUES
        (N'Sneaker năng động', N'Những mẫu giày linh hoạt cho học tập và đi chơi.', '/images/demo/collections/nang-dong.jpg', '/products'),
        (N'Chạy bộ mỗi ngày', N'Khám phá giày chạy bộ êm ái, phù hợp luyện tập.', '/images/demo/collections/chay-bo.jpg', '/products?sort=newest'),
        (N'Phong cách tối giản', N'Những đôi sneaker dễ phối cùng trang phục.', '/images/demo/collections/toi-gian.jpg', '/products')
    ) AS v(title, description, image_url, target_url)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.home_collections c WHERE c.title = v.title);

    -- 6. NEWS
    INSERT INTO dbo.home_news
        (title, summary, image_url, article_url, published_at, active)
    SELECT v.title, v.summary, v.image_url, v.article_url,
           DATEADD(DAY, -v.days_ago, SYSDATETIME()), 1
    FROM (VALUES
        (N'Cách chọn size giày thể thao phù hợp', N'Hướng dẫn chọn kích thước giày vừa chân khi mua trực tuyến.', '/images/demo/news/size-giay.jpg', '/news/chon-size-giay', 1),
        (N'Mẹo bảo quản sneaker luôn sạch đẹp', N'Các bước vệ sinh và bảo quản giày đơn giản tại nhà.', '/images/demo/news/bao-quan.jpg', '/news/bao-quan-sneaker', 3),
        (N'Xu hướng sneaker phong cách tối giản', N'Gợi ý phối đồ với những mẫu giày cơ bản, dễ sử dụng.', '/images/demo/news/xu-huong.jpg', '/news/xu-huong-sneaker', 5)
    ) AS v(title, summary, image_url, article_url, days_ago)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.home_news n WHERE n.title = v.title);

    -- 7. PRODUCTS
    -- sale_days > 0 = active flash sale; sale_days < 0 = expired sale;
    -- NULL = not a flash sale. sale_price NULL tests ordinary-price handling.
    INSERT INTO dbo.products
        (name, sku, description, image_url, original_price, sale_price,
         stock_quantity, sizes, colors, sold_count, featured, active,
         flash_sale_end, brand_id, category_id, created_at)
    SELECT
        v.name, v.sku, v.description, v.image_url,
        v.original_price, v.sale_price, v.stock_quantity,
        v.sizes, v.colors, v.sold_count, v.featured, 1,
        CASE WHEN v.sale_days IS NULL THEN NULL
             ELSE DATEADD(DAY, v.sale_days, SYSDATETIME()) END,
        b.id, c.id, DATEADD(DAY, -v.created_days_ago, SYSDATETIME())
    FROM (VALUES
        (N'Nike Air Max Pulse', 'SH-NK-PULSE-001', N'Giày nam êm ái, phù hợp đi chơi và vận động.', '/images/demo/products/nike-pulse.jpg', 2900000.00, 2390000.00, 25, '39,40,41,42,43', N'Đen,Trắng', 120, 1, 4, 9, N'Nike', 'chay-bo-nam'),
        (N'Adidas Ultraboost Light', 'SH-AD-UB-002', N'Giày chạy bộ hỗ trợ bàn chân, thiết kế năng động.', '/images/demo/products/adidas-ultraboost.jpg', 4200000.00, 3390000.00, 18, '38,39,40,41,42', N'Trắng,Xám', 150, 1, 5, 3, N'Adidas', 'chay-bo-nam'),
        (N'Nike Precision 7', 'SH-NK-PRE-003', N'Giày bóng rổ có độ bám tốt cho sân tập.', '/images/demo/products/nike-precision.jpg', 2190000.00, NULL, 20, '40,41,42,43,44', N'Đen,Đỏ', 50, 0, NULL, 6, N'Nike', 'bong-ro-nam'),
        (N'New Balance 530', 'SH-NB-530-004', N'Sneaker nữ cổ điển, phối màu thanh lịch.', '/images/demo/products/nb-530.jpg', 2650000.00, 2190000.00, 35, '36,37,38,39,40', N'Bạc,Trắng', 185, 1, NULL, 2, N'New Balance', 'thoi-trang-nu'),
        (N'Puma Velocity Nitro', 'SH-PM-NITRO-005', N'Giày chạy bộ nữ phù hợp tập luyện hằng ngày.', '/images/demo/products/puma-nitro.jpg', 3100000.00, 2550000.00, 16, '36,37,38,39,40', N'Xanh,Hồng', 80, 0, 6, 8, N'Puma', 'chay-bo-nu'),
        (N'Converse Chuck Taylor Low', 'SH-CV-LOW-006', N'Sneaker cổ thấp cơ bản, dễ phối trang phục.', '/images/demo/products/converse-low.jpg', 1500000.00, NULL, 40, '36,37,38,39,40,41,42', N'Trắng,Đen', 260, 1, NULL, 15, N'Converse', 'sneaker-co-thap'),
        (N'Converse Chuck 70 High', 'SH-CV-HIGH-007', N'Sneaker cổ cao phong cách đường phố.', '/images/demo/products/converse-high.jpg', 2150000.00, 1790000.00, 12, '37,38,39,40,41,42', N'Đen,Be', 96, 0, -2, 17, N'Converse', 'sneaker-co-cao'),
        (N'Adidas Grand Court', 'SH-AD-COURT-008', N'Giày cổ thấp gọn nhẹ, thích hợp đi học.', '/images/demo/products/adidas-grand-court.jpg', 1850000.00, NULL, 50, '36,37,38,39,40,41,42', N'Trắng,Xanh', 220, 1, NULL, 12, N'Adidas', 'sneaker-co-thap'),
        (N'New Balance 574', 'SH-NB-574-009', N'Sneaker cổ điển với chất liệu mềm mại.', '/images/demo/products/nb-574.jpg', 2750000.00, 2290000.00, 22, '38,39,40,41,42', N'Xám,Đen', 110, 0, 3, 4, N'New Balance', 'sneaker-co-thap'),
        (N'Nike Court Vision Mid', 'SH-NK-CVM-010', N'Sneaker cổ cao mang phong cách thể thao.', '/images/demo/products/nike-court-mid.jpg', 2350000.00, NULL, 28, '39,40,41,42,43', N'Trắng,Đỏ', 75, 0, NULL, 10, N'Nike', 'sneaker-co-cao'),
        (N'Puma Smash V2', 'SH-PM-SMASH-011', N'Giày đi chơi đơn giản, dễ kết hợp trang phục.', '/images/demo/products/puma-smash.jpg', 1450000.00, 1190000.00, 30, '37,38,39,40,41,42', N'Đen,Trắng', 145, 0, NULL, 13, N'Puma', 'sneaker-co-thap'),
        (N'Adidas Runfalcon', 'SH-AD-RUN-012', N'Giày chạy nhẹ, phù hợp sử dụng hằng ngày.', '/images/demo/products/adidas-runfalcon.jpg', 1700000.00, NULL, 32, '38,39,40,41,42,43', N'Xanh,Đen', 65, 0, NULL, 1, N'Adidas', 'chay-bo-nam')
    ) AS v(name, sku, description, image_url, original_price, sale_price,
           stock_quantity, sizes, colors, sold_count, featured,
           sale_days, created_days_ago, brand_name, category_slug)
    JOIN dbo.brands b ON b.name = v.brand_name
    JOIN dbo.categories c ON c.slug = v.category_slug
    WHERE NOT EXISTS (SELECT 1 FROM dbo.products p WHERE p.sku = v.sku);

    COMMIT TRANSACTION;

    PRINT 'HOMEPAGE DEV/TEST SEED SUCCESS';

    SELECT 'brands' AS TableName, COUNT(*) AS RowCount FROM dbo.brands
    UNION ALL SELECT 'categories', COUNT(*) FROM dbo.categories
    UNION ALL SELECT 'products', COUNT(*) FROM dbo.products
    UNION ALL SELECT 'home_banners', COUNT(*) FROM dbo.home_banners
    UNION ALL SELECT 'home_collections', COUNT(*) FROM dbo.home_collections
    UNION ALL SELECT 'home_news', COUNT(*) FROM dbo.home_news;

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
