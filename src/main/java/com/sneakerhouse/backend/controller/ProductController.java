
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.ProductDetailResponse;
import com.sneakerhouse.backend.dto.ProductResponse;
import com.sneakerhouse.backend.entity.Product;
import com.sneakerhouse.backend.repository.ProductRepository;

import jakarta.persistence.criteria.JoinType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repo;

    public ProductController(ProductRepository repo) {
        this.repo = repo;
    }

    // =====================================================
    // 1. DANH SÁCH, TÌM KIẾM, LỌC VÀ PHÂN TRANG SẢN PHẨM
    // =====================================================

    @GetMapping
    @Transactional(readOnly = true)
    public Map<String, Object> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int sizePerPage
    ) {

        // Kiểm tra phân trang
        if (page < 0 || sizePerPage < 1 || sizePerPage > 100) {
            throw new IllegalArgumentException(
                    "page phải >= 0 và sizePerPage từ 1 đến 100"
            );
        }

        // Kiểm tra khoảng giá
        if (minPrice != null && minPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "minPrice không được nhỏ hơn 0"
            );
        }

        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "maxPrice không được nhỏ hơn 0"
            );
        }

        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException(
                    "minPrice không được lớn hơn maxPrice"
            );
        }

        // Chỉ lấy sản phẩm đang hoạt động
        Specification<Product> spec = (root, query, cb) ->
                cb.isTrue(root.get("active"));

        // Tìm kiếm theo tên, SKU hoặc thương hiệu
        if (keyword != null && !keyword.isBlank()) {

            String searchKeyword =
                    "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(
                                    cb.lower(root.get("name")),
                                    searchKeyword
                            ),
                            cb.like(
                                    cb.lower(root.get("sku")),
                                    searchKeyword
                            ),
                            cb.like(
                                    cb.lower(
                                            root.join(
                                                    "brand",
                                                    JoinType.LEFT
                                            ).get("name")
                                    ),
                                    searchKeyword
                            )
                    )
            );
        }

        // Lọc theo danh mục được chọn và danh mục con trực tiếp
        if (categoryId != null) {

            spec = spec.and((root, query, cb) -> {

                var category = root.join(
                        "category",
                        JoinType.LEFT
                );

                var parent = category.join(
                        "parent",
                        JoinType.LEFT
                );

                return cb.or(
                        cb.equal(category.get("id"), categoryId),
                        cb.equal(parent.get("id"), categoryId)
                );
            });
        }

        // Lọc theo thương hiệu
        if (brandId != null) {

            spec = spec.and((root, query, cb) ->
                    cb.equal(
                            root.get("brand").get("id"),
                            brandId
                    )
            );
        }

        // Lọc giá tối thiểu
        if (minPrice != null) {

            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(
                            cb.coalesce(
                                    root.<BigDecimal>get("salePrice"),
                                    root.<BigDecimal>get("originalPrice")
                            ),
                            minPrice
                    )
            );
        }

        // Lọc giá tối đa
        if (maxPrice != null) {

            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(
                            cb.coalesce(
                                    root.<BigDecimal>get("salePrice"),
                                    root.<BigDecimal>get("originalPrice")
                            ),
                            maxPrice
                    )
            );
        }

        // Lọc size chính xác
        if (size != null && !size.isBlank()) {

            String searchSize = "%," + size.trim() + ",%";

            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.concat(
                                    cb.concat(",", root.<String>get("sizes")),
                                    ","
                            ),
                            searchSize
                    )
            );
        }

        // Lọc màu sắc chính xác, không phân biệt chữ hoa/thường
        if (color != null && !color.isBlank()) {

            String selectedColor =
                    color.trim().toLowerCase(Locale.ROOT);

            spec = spec.and((root, query, cb) -> {

                var colorsColumn = cb.lower(
                        root.<String>get("colors")
                );

                return cb.or(
                        cb.equal(colorsColumn, selectedColor),
                        cb.like(colorsColumn, selectedColor + ",%"),
                        cb.like(colorsColumn, "%," + selectedColor + ",%"),
                        cb.like(colorsColumn, "%," + selectedColor)
                );
            });
        }

        // Sắp xếp sản phẩm
        Sort order;

        if ("price_asc".equalsIgnoreCase(sort)
                || "price_desc".equalsIgnoreCase(sort)) {

            boolean ascending = "price_asc".equalsIgnoreCase(sort);

            spec = spec.and((root, query, cb) -> {

                // Không áp dụng orderBy cho truy vấn COUNT
                if (query != null
                        && !Long.class.equals(query.getResultType())) {

                    var effectivePrice = cb.coalesce(
                            root.<BigDecimal>get("salePrice"),
                            root.<BigDecimal>get("originalPrice")
                    );

                    query.orderBy(
                            ascending
                                    ? cb.asc(effectivePrice)
                                    : cb.desc(effectivePrice),
                            cb.asc(root.get("id"))
                    );
                }

                return cb.conjunction();
            });

            order = Sort.unsorted();

        } else {

            order = switch (sort.toLowerCase(Locale.ROOT)) {

                case "best_selling" ->
                        Sort.by("soldCount").descending();

                case "newest" ->
                        Sort.by("createdAt").descending();

                default ->
                        throw new IllegalArgumentException(
                                "sort chỉ nhận newest, price_asc, "
                                        + "price_desc, best_selling"
                        );
            };
        }

        // Truy vấn và phân trang
        Page<Product> result = repo.findAll(
                spec,
                PageRequest.of(page, sizePerPage, order)
        );

        // Tạo JSON response
        Map<String, Object> response = new LinkedHashMap<>();

        response.put(
                "content",
                result.getContent()
                        .stream()
                        .map(ProductResponse::from)
                        .toList()
        );

        response.put("page", result.getNumber());
        response.put("size", result.getSize());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("last", result.isLast());

        return response;
    }

    // =====================================================
    // 2. CHI TIẾT SẢN PHẨM THEO ID
    // =====================================================

    @GetMapping("/{id:\\d+}")
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(
            @PathVariable Long id
    ) {

        Product product = repo.findById(id)
                .filter(p -> Boolean.TRUE.equals(p.active))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm"
                ));

        return ProductDetailResponse.from(product);
    }

    // =====================================================
    // 3. CHI TIẾT SẢN PHẨM THEO SLUG
    // =====================================================

    @GetMapping("/slug/{slug}")
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductBySlug(
            @PathVariable String slug
    ) {

        Product product = repo.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm"
                ));

        return ProductDetailResponse.from(product);
    }
}
