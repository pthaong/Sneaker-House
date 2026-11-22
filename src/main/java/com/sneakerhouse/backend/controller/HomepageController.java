
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.ProductResponse;

import com.sneakerhouse.backend.entity.Brand;
import com.sneakerhouse.backend.entity.Category;
import com.sneakerhouse.backend.entity.HomeBanner;
import com.sneakerhouse.backend.entity.HomeCollection;
import com.sneakerhouse.backend.entity.HomeNews;
import com.sneakerhouse.backend.entity.Product;

import com.sneakerhouse.backend.repository.BrandRepository;
import com.sneakerhouse.backend.repository.CategoryRepository;
import com.sneakerhouse.backend.repository.HomeBannerRepository;
import com.sneakerhouse.backend.repository.HomeCollectionRepository;
import com.sneakerhouse.backend.repository.HomeNewsRepository;
import com.sneakerhouse.backend.repository.ProductRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class HomepageController {

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
        response.put(
                "categories",
                categories.findByActiveTrueOrderByNameAsc()
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
    // POST /api/banners
    // PUT /api/banners/{id}
    // DELETE /api/banners/{id}
    // =====================================================

    // Tao banner
    @PostMapping("/banners")
    @Transactional
    public HomeBanner createBanner(
            @RequestBody HomeBanner input
    ) {

        // Khong cho client tu quyet dinh ID khi tao moi
        input.id = null;

        return banners.save(input);
    }

    // Cap nhat banner
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

    // Xoa banner
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
    // 3. LAY DANH SACH BANNER
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
    // =====================================================

    @GetMapping("/categories")
    public List<Category> categories() {

        return categories.findByActiveTrueOrderByNameAsc();
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
}
