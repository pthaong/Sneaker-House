
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.ProductResponse;
import com.sneakerhouse.backend.entity.Product;
import com.sneakerhouse.backend.repository.ProductRepository;

import jakarta.persistence.criteria.JoinType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repo;

    public ProductController(ProductRepository repo) {
        this.repo = repo;
    }

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

        // 1. Kiem tra phan trang
        if (page < 0 || sizePerPage < 1 || sizePerPage > 100) {
            throw new IllegalArgumentException(
                    "page phải >= 0 và sizePerPage từ 1 đến 100"
            );
        }

        // 2. Kiem tra khoang gia
        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException(
                    "minPrice không được lớn hơn maxPrice"
            );
        }

        // 3. Chi hien thi san pham dang hoat dong
        Specification<Product> spec = (root, query, cb) ->
                cb.isTrue(root.get("active"));

        // 4. Tim kiem theo ten san pham, SKU hoac thuong hieu
        if (keyword != null && !keyword.isBlank()) {

            String searchKeyword =
                    "%" + keyword.trim().toLowerCase() + "%";

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

        // 5. Loc theo danh muc cha va danh muc con truc tiep
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
                        // San pham thuoc danh muc duoc chon
                        cb.equal(
                                category.get("id"),
                                categoryId
                        ),

                        // San pham thuoc danh muc con cua danh muc duoc chon
                        cb.equal(
                                parent.get("id"),
                                categoryId
                        )
                );
            });
        }

        // 6. Loc theo thuong hieu
        if (brandId != null) {

            spec = spec.and((root, query, cb) ->
                    cb.equal(
                            root.get("brand").get("id"),
                            brandId
                    )
            );
        }

        // 7. Loc theo gia toi thieu
        if (minPrice != null) {

            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(
                            cb.coalesce(
                                    root.get("salePrice"),
                                    root.get("originalPrice")
                            ),
                            minPrice
                    )
            );
        }

        // 8. Loc theo gia toi da
        if (maxPrice != null) {

            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(
                            cb.coalesce(
                                    root.get("salePrice"),
                                    root.get("originalPrice")
                            ),
                            maxPrice
                    )
            );
        }


        // 9. Loc theo size chinh xac
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



        // 10. Loc theo mau sac chinh xac, ho tro Unicode
        if (color != null && !color.isBlank()) {

            String selectedColor = color.trim()
                    .toLowerCase(java.util.Locale.ROOT);

            spec = spec.and((root, query, cb) -> {

                var colorsColumn = cb.lower(
                        root.<String>get("colors")
                );

                return cb.or(
                        // Chi co mot mau
                        cb.equal(colorsColumn, selectedColor),

                        // Mau o dau danh sach
                        cb.like(
                                colorsColumn,
                                selectedColor + ",%"
                        ),

                        // Mau o giua danh sach
                        cb.like(
                                colorsColumn,
                                "%," + selectedColor + ",%"
                        ),

                        // Mau o cuoi danh sach
                        cb.like(
                                colorsColumn,
                                "%," + selectedColor
                        )
                );
            });
        }


        // 11. Sap xep san pham theo gia ban thuc te
        Sort order;

        if ("price_asc".equalsIgnoreCase(sort)
                || "price_desc".equalsIgnoreCase(sort)) {

            boolean ascending = "price_asc".equalsIgnoreCase(sort);

            spec = spec.and((root, query, cb) -> {

                // Khong sap xep trong cau truy van COUNT
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

            order = switch (sort.toLowerCase(java.util.Locale.ROOT)) {

                case "best_selling" ->
                        Sort.by("soldCount").descending();

                case "newest" ->
                        Sort.by("createdAt").descending();

                default ->
                        throw new IllegalArgumentException(
                                "sort chỉ nhận newest, price_asc, price_desc, best_selling"
                        );
            };
        }

        // 12. Truy van va phan trang
        Page<Product> result = repo.findAll(
                spec,
                PageRequest.of(page, sizePerPage, order)
        );

        // 13. Tao JSON Response
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
}
