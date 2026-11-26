
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.CategoryResponse;
import com.sneakerhouse.backend.dto.ProductResponse;

import com.sneakerhouse.backend.entity.Brand;
import com.sneakerhouse.backend.entity.Category;
import com.sneakerhouse.backend.entity.HomeBanner;
import com.sneakerhouse.backend.entity.HomeCollection;
import com.sneakerhouse.backend.entity.HomeNews;
import com.sneakerhouse.backend.entity.Product;
import org.hibernate.Hibernate;
import com.sneakerhouse.backend.repository.BrandRepository;
import com.sneakerhouse.backend.repository.CategoryRepository;
import com.sneakerhouse.backend.repository.HomeBannerRepository;
import com.sneakerhouse.backend.repository.HomeCollectionRepository;
import com.sneakerhouse.backend.repository.HomeNewsRepository;
import com.sneakerhouse.backend.repository.ProductRepository;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class HomepageController {

    // =====================================================
    // ENTITY MANAGER
    // Lay ID danh muc cha ma khong serialize Hibernate Proxy
    // =====================================================

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    // =====================================================
    // REPOSITORIES
    // =====================================================

    private final HomeBannerRepository banners;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductRepository products;
    private final HomeCollectionRepository collections;
    private final HomeNewsRepository news;

    public HomepageController(
            HomeBannerRepository banners,
            CategoryRepository categories,
            BrandRepository brands,
            ProductRepository products,
            HomeCollectionRepository collections,
            HomeNewsRepository news
    ) {
        this.banners = banners;
        this.categories = categories;
        this.brands = brands;
        this.products = products;
        this.collections = collections;
        this.news = news;
    }

    // =====================================================
    // 1. API TONG HOP DU LIEU TRANG CHU
    // GET /api/homepage
    // =====================================================

    @GetMapping("/homepage")
    public Map<String, Object> homepage() {

        Map<String, Object> response = new LinkedHashMap<>();

        // Banner chinh
        response.put(
                "mainBanners",
                banners.findByActiveTrueAndBannerTypeOrderByPositionAsc(
                        "MAIN"
                )
        );

        // Banner quang cao
        response.put(
                "advertisingBanners",
                banners.findByActiveTrueAndBannerTypeOrderByPositionAsc(
                        "AD"
                )
        );

        // Danh muc san pham
        // Tra DTO thay vi JPA Entity
        response.put(
                "categories",
                categoryTree()
        );

        // Thuong hieu
        response.put(
                "brands",
                brands.findByActiveTrueOrderByNameAsc()
        );

        // San pham noi bat
        response.put(
                "featuredProducts",
                map(
                        products
                                .findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc()
                )
        );

        // San pham moi ve
        response.put(
                "newArrivals",
                map(
                        products
                                .findTop8ByActiveTrueOrderByCreatedAtDesc()
                )
        );

        // San pham ban chay
        response.put(
                "bestSellers",
                map(
                        products
                                .findTop8ByActiveTrueOrderBySoldCountDesc()
                )
        );

        // Flash Sale con thoi han
        response.put(
                "flashSales",
                map(
                        products
                                .findByActiveTrueAndFlashSaleEndAfterOrderByFlashSaleEndAsc(
                                        LocalDateTime.now()
                                )
                )
        );

        // Bo suu tap
        response.put(
                "collections",
                collections.findByActiveTrueOrderByIdDesc()
        );

        // Tin tuc
        response.put(
                "news",
                news.findTop8ByActiveTrueOrderByPublishedAtDesc()
        );

        return response;
    }

    // =====================================================
    // 2. QUAN LY BANNER
    // POST   /api/banners
    // PUT    /api/banners/{id}
    // DELETE /api/banners/{id}
    //
    // Quyen ADMIN duoc kiem soat trong SecurityConfig
    // =====================================================

    @PostMapping("/banners")
    @Transactional
    public HomeBanner createBanner(
            @RequestBody HomeBanner input
    ) {

        // Khong cho client tu quyet dinh ID
        input.id = null;

        return banners.save(input);
    }

    @PutMapping("/banners/{id}")
    @Transactional
    public HomeBanner updateBanner(
            @PathVariable Long id,
            @RequestBody HomeBanner input
    ) {

        HomeBanner banner = banners.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Không tìm thấy banner"
                        )
                );

        banner.title = input.title;
        banner.imageUrl = input.imageUrl;
        banner.targetUrl = input.targetUrl;
        banner.bannerType = input.bannerType;
        banner.position = input.position;
        banner.active = input.active;

        return banners.save(banner);
    }

    @DeleteMapping("/banners/{id}")
    @Transactional
    public void deleteBanner(
            @PathVariable Long id
    ) {

        if (!banners.existsById(id)) {
            throw new NoSuchElementException(
                    "Không tìm thấy banner"
            );
        }

        banners.deleteById(id);
    }

    // =====================================================
    // 3. DANH SACH BANNER
    // GET /api/banners?type=ALL
    // GET /api/banners?type=MAIN
    // GET /api/banners?type=AD
    // =====================================================

    @GetMapping("/banners")
    public List<HomeBanner> banners(
            @RequestParam(defaultValue = "ALL") String type
    ) {

        if ("ALL".equalsIgnoreCase(type)) {
            return banners.findByActiveTrueOrderByPositionAsc();
        }

        return banners
                .findByActiveTrueAndBannerTypeOrderByPositionAsc(
                        type.toUpperCase(Locale.ROOT)
                );
    }

    // =====================================================
    // 4. DANH MUC SAN PHAM
    // GET /api/categories
    //
    // Tra DTO dang cay:
    // Danh muc cha -> Danh muc con
    // =====================================================

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {

        return categoryTree();
    }

    // =====================================================
    // 5. THUONG HIEU
    // GET /api/brands
    // =====================================================

    @GetMapping("/brands")
    public List<Brand> brands() {

        return brands.findByActiveTrueOrderByNameAsc();
    }

    // =====================================================
    // 6. SAN PHAM NOI BAT
    // GET /api/products/featured
    // =====================================================

    @GetMapping("/products/featured")
    public List<ProductResponse> featured() {

        return map(
                products
                        .findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc()
        );
    }

    // =====================================================
    // 7. SAN PHAM MOI VE
    // GET /api/products/new-arrivals
    // =====================================================

    @GetMapping("/products/new-arrivals")
    public List<ProductResponse> newest() {

        return map(
                products.findTop8ByActiveTrueOrderByCreatedAtDesc()
        );
    }

    // =====================================================
    // 8. SAN PHAM BAN CHAY
    // GET /api/products/best-sellers
    // =====================================================

    @GetMapping("/products/best-sellers")
    public List<ProductResponse> best() {

        return map(
                products.findTop8ByActiveTrueOrderBySoldCountDesc()
        );
    }

    // =====================================================
    // 9. FLASH SALE
    // GET /api/flash-sales
    // =====================================================

    @GetMapping("/flash-sales")
    public List<ProductResponse> flash() {

        return map(
                products
                        .findByActiveTrueAndFlashSaleEndAfterOrderByFlashSaleEndAsc(
                                LocalDateTime.now()
                        )
        );
    }

    // =====================================================
    // 10. BO SUU TAP
    // GET /api/collections
    // =====================================================

    @GetMapping("/collections")
    public List<HomeCollection> collections() {

        return collections.findByActiveTrueOrderByIdDesc();
    }

    // =====================================================
    // 11. TIN TUC
    // GET /api/news
    // =====================================================

    @GetMapping("/news")
    public List<HomeNews> news() {

        return news.findTop8ByActiveTrueOrderByPublishedAtDesc();
    }

    // =====================================================
    // 12. CHUYEN PRODUCT ENTITY SANG DTO
    // =====================================================

    private List<ProductResponse> map(List<Product> list) {

        return list.stream()
                .map(ProductResponse::from)
                .toList();
    }

    // =====================================================
    // 13. CHUYEN CATEGORY ENTITY SANG DTO
    //
    // Khong tra truc tiep JPA Entity Category.
    // Tranh loi Hibernate Lazy Proxy / ByteBuddyInterceptor.
    //
    // Danh muc goc co parentId = null.
    // Danh muc con duoc dua vao children cua danh muc cha.
    //
    // Dung chung cho:
    // GET /api/categories
    // GET /api/homepage
    // =====================================================


private List<CategoryResponse> categoryTree() {

    List<Category> entities =
            categories.findByActiveTrueOrderByNameAsc();

    Map<Long, CategoryResponse> nodes =
            new LinkedHashMap<>();

    // Buoc 1: Chuyen Category Entity sang DTO

for (Category category : entities) {

    // Khoi tao Hibernate Proxy de doc duoc du lieu that
    Category loaded = Hibernate.unproxy(
            category,
            Category.class
    );

    Long id = (Long) entityManagerFactory
            .getPersistenceUnitUtil()
            .getIdentifier(loaded);

    if (id == null) {
        throw new IllegalStateException(
                "Danh muc khong co ID hop le"
        );
    }

    Long parentId = loaded.parent == null
            ? null
            : (Long) entityManagerFactory
                    .getPersistenceUnitUtil()
                    .getIdentifier(loaded.parent);

    CategoryResponse dto = new CategoryResponse(
            id,
            loaded.name,
            loaded.slug,
            loaded.imageUrl,
            parentId,
            loaded.active,
            new ArrayList<>()
    );

    nodes.put(id, dto);
}

    // Buoc 2: Xay dung cay danh muc
    List<CategoryResponse> roots = new ArrayList<>();

    for (CategoryResponse dto : nodes.values()) {

        Long parentId = dto.parentId();

        // Danh muc goc hoac du lieu tu tham chieu
        if (parentId == null || parentId.equals(dto.id())) {
            roots.add(dto);
            continue;
        }

        CategoryResponse parent = nodes.get(parentId);

        if (parent == null) {
            // Danh muc cha khong nam trong danh sach active
            roots.add(dto);
        } else {
            parent.children().add(dto);
        }
    }

    return roots;
}
}
