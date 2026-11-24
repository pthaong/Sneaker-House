
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.ProductResponse;
import com.sneakerhouse.backend.entity.HomeBanner;
import com.sneakerhouse.backend.entity.HomeCollection;
import com.sneakerhouse.backend.entity.HomeNews;
import com.sneakerhouse.backend.entity.Product;

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
    private final ProductRepository products;
    private final HomeCollectionRepository collections;
    private final HomeNewsRepository news;

    public HomepageController(
            HomeBannerRepository banners,
            CategoryRepository categories,
            ProductRepository products,
            HomeCollectionRepository collections,
            HomeNewsRepository news
    ) {
        this.banners = banners;
        this.categories = categories;
        this.products = products;
        this.collections = collections;
        this.news = news;
    }

    // 1. API tong hop du lieu trang chu
    // GET /api/homepage

    @GetMapping("/homepage")
    public Map<String, Object> homepage() {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put(
                "mainBanners",
                banners.findByActiveTrueAndBannerTypeOrderByPositionAsc("MAIN")
        );

        response.put(
                "advertisingBanners",
                banners.findByActiveTrueAndBannerTypeOrderByPositionAsc("AD")
        );

        // Chuyen Category Entity sang Map de tranh loi Hibernate Proxy
        List<Map<String, Object>> categoryList =
                categories.findByActiveTrueOrderByNameAsc()
                        .stream()
                        .map(category -> {
                            Map<String, Object> item = new LinkedHashMap<>();

                            item.put("id", category.id);
                            item.put("name", category.name);
                            item.put("slug", category.slug);
                            item.put("imageUrl", category.imageUrl);

                            item.put(
                                    "parentId",
                                    category.parent == null
                                            ? null
                                            : category.parent.id
                            );

                            return item;
                        })
                        .toList();

        response.put("categories", categoryList);

        response.put(
                "featuredProducts",
                map(products.findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc())
        );

        response.put(
                "newArrivals",
                map(products.findTop8ByActiveTrueOrderByCreatedAtDesc())
        );

        response.put(
                "bestSellers",
                map(products.findTop8ByActiveTrueOrderBySoldCountDesc())
        );

        response.put(
                "flashSales",
                map(products.findByActiveTrueAndFlashSaleEndAfterOrderByFlashSaleEndAsc(
                        LocalDateTime.now()
                ))
        );

        response.put(
                "collections",
                collections.findByActiveTrueOrderByIdDesc()
        );

        response.put(
                "news",
                news.findTop8ByActiveTrueOrderByPublishedAtDesc()
        );

        return response;
    }

    // 2. Quan ly banner

    @PostMapping("/banners")
    @Transactional
    public HomeBanner createBanner(@RequestBody HomeBanner input) {

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
                        new NoSuchElementException("Không tìm thấy banner")
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
    public void deleteBanner(@PathVariable Long id) {

        if (!banners.existsById(id)) {
            throw new NoSuchElementException("Không tìm thấy banner");
        }

        banners.deleteById(id);
    }

    // 3. Lay danh sach banner

    @GetMapping("/banners")
    public List<HomeBanner> banners(
            @RequestParam(defaultValue = "ALL") String type
    ) {

        if ("ALL".equalsIgnoreCase(type)) {
            return banners.findByActiveTrueOrderByPositionAsc();
        }

        return banners.findByActiveTrueAndBannerTypeOrderByPositionAsc(
                type.toUpperCase(Locale.ROOT)
        );
    }

    // 4. San pham noi bat

    @GetMapping("/products/featured")
    public List<ProductResponse> featured() {

        return map(
                products.findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc()
        );
    }

    // 5. San pham moi ve

    @GetMapping("/products/new-arrivals")
    public List<ProductResponse> newest() {

        return map(products.findTop8ByActiveTrueOrderByCreatedAtDesc());
    }

    // 6. San pham ban chay

    @GetMapping("/products/best-sellers")
    public List<ProductResponse> best() {

        return map(products.findTop8ByActiveTrueOrderBySoldCountDesc());
    }

    // 7. Flash Sale

    @GetMapping("/flash-sales")
    public List<ProductResponse> flash() {

        return map(
                products.findByActiveTrueAndFlashSaleEndAfterOrderByFlashSaleEndAsc(
                        LocalDateTime.now()
                )
        );
    }

    // 8. Bo suu tap

    @GetMapping("/collections")
    public List<HomeCollection> collections() {

        return collections.findByActiveTrueOrderByIdDesc();
    }

    // 9. Tin tuc

    @GetMapping("/news")
    public List<HomeNews> news() {

        return news.findTop8ByActiveTrueOrderByPublishedAtDesc();
    }

    // 10. Chuyen Product Entity sang DTO

    private List<ProductResponse> map(List<Product> list) {

        return list.stream()
                .map(ProductResponse::from)
                .toList();
    }
}
