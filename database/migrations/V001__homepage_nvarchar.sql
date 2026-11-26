
-- =========================================================
-- SNEAKER HOUSE - HOMEPAGE
-- Migration V001: VARCHAR -> NVARCHAR
-- Tables: brands, categories, home_banners,
--         home_collections, home_news, products
--
-- Luu y:
-- 1. Tao backup database truoc khi ap dung.
-- 2. Chon dung database trong SSMS.
-- 3. Thay REPLACE_WITH_DATABASE_NAME bang ten database.
-- 4. Script co the chay lai neu cot da la NVARCHAR.
-- =========================================================

SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @ExpectedDatabase SYSNAME =
    N'REPLACE_WITH_DATABASE_NAME';

IF @ExpectedDatabase = N'REPLACE_WITH_DATABASE_NAME'
   OR DB_NAME() <> @ExpectedDatabase
BEGIN
    THROW 50001, 'Database khong khop. Huy migration.', 1;
END;

BEGIN TRY
    BEGIN TRANSACTION;

    -- 1. BRANDS
    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.brands')
          AND name = N'name'
          AND system_type_id = 167
    )
        ALTER TABLE dbo.brands
        ALTER COLUMN name NVARCHAR(100) NOT NULL;

    -- 2. CATEGORIES
    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.categories')
          AND name = N'name'
          AND system_type_id = 167
    )
        ALTER TABLE dbo.categories
        ALTER COLUMN name NVARCHAR(120) NOT NULL;

    -- 3. HOME BANNERS
    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.home_banners')
          AND name = N'title'
          AND system_type_id = 167
    )
        ALTER TABLE dbo.home_banners
        ALTER COLUMN title NVARCHAR(150) NOT NULL;

    -- 4. HOME COLLECTIONS
    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.home_collections')
          AND name = N'title'
          AND system_type_id = 167
    )
        ALTER TABLE dbo.home_collections
        ALTER COLUMN title NVARCHAR(150) NOT NULL;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.home_collections')
          AND name = N'description'
          AND system_type_id = 167
    )
        ALTER TABLE dbo.home_collections
        ALTER COLUMN description NVARCHAR(500) NULL;

    -- 5. HOME NEWS
    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.home_news')
          AND name = N'title'
          AND system_type_id = 167
    )
        ALTER TABLE dbo.home_news
        ALTER COLUMN title NVARCHAR(180) NOT NULL;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.home_news')
          AND name = N'summary'
          AND system_type_id = 167
    )
        ALTER TABLE dbo.home_news
        ALTER COLUMN summary NVARCHAR(1000) NULL;

    -- 6. PRODUCTS
    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.products')
          AND name = N'name'
          AND system_type_id = 167
    )
        ALTER TABLE dbo.products
        ALTER COLUMN name NVARCHAR(180) NOT NULL;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.products')
          AND name = N'description'
          AND system_type_id = 167
    )
        ALTER TABLE dbo.products
        ALTER COLUMN description NVARCHAR(1000) NULL;

    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.products')
          AND name = N'colors'
          AND system_type_id = 167
    )
        ALTER TABLE dbo.products
        ALTER COLUMN colors NVARCHAR(300) NULL;

    -- Kiem tra du 10 cot va dung kieu NVARCHAR
    DECLARE @ValidColumns INT;

    SELECT @ValidColumns = COUNT(*)
    FROM sys.tables t
    JOIN sys.columns c ON t.object_id = c.object_id
    JOIN sys.types ty ON c.user_type_id = ty.user_type_id
    WHERE ty.name = N'nvarchar'
      AND (
        (t.name = N'brands' AND c.name = N'name')
        OR (t.name = N'categories' AND c.name = N'name')
        OR (t.name = N'home_banners' AND c.name = N'title')
        OR (t.name = N'home_collections'
            AND c.name IN (N'title', N'description'))
        OR (t.name = N'home_news'
            AND c.name IN (N'title', N'summary'))
        OR (t.name = N'products'
            AND c.name IN (N'name', N'description', N'colors'))
      );

    IF @ValidColumns <> 10
        THROW 50002, 'Chua du 10 cot NVARCHAR. Rollback.', 1;

    COMMIT TRANSACTION;

    PRINT 'MIGRATION NVARCHAR SUCCESS - 10/10 COLUMNS';

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
