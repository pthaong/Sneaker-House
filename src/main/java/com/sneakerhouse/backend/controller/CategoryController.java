
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.CategoryResponse;
import com.sneakerhouse.backend.entity.Category;
import com.sneakerhouse.backend.repository.CategoryRepository;
import com.sneakerhouse.backend.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryController(
            CategoryRepository categoryRepository,
            ProductRepository productRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    // Danh sách danh mục
    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {

        return categoryRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Chi tiết danh mục
    @GetMapping("/{id:\\d+}")
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(@PathVariable Long id) {

        Category category = categoryRepository.findById(id)
                .filter(c -> Boolean.TRUE.equals(c.active))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy danh mục"
                ));

        return toResponse(category);
    }

    // Danh mục dạng cây cha - con
    @GetMapping("/tree")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {

        List<Category> categories =
                categoryRepository.findByActiveTrueOrderByNameAsc();

        Map<Long, List<Category>> childrenMap = categories.stream()
                .filter(c -> c.parent != null)
                .collect(Collectors.groupingBy(c -> c.parent.id));

        return categories.stream()
                .filter(c -> c.parent == null)
                .map(c -> toTreeResponse(c, childrenMap))
                .toList();
    }

    private CategoryResponse toTreeResponse(
            Category category,
            Map<Long, List<Category>> childrenMap
    ) {

        List<CategoryResponse> children = childrenMap
                .getOrDefault(category.id, List.of())
                .stream()
                .map(child -> toTreeResponse(child, childrenMap))
                .toList();

        return new CategoryResponse(
                category.id,
                category.name,
                category.slug,
                category.imageUrl,
                category.parent == null ? null : category.parent.id,
                productRepository.countByCategory_IdAndActiveTrue(category.id),
                children
        );
    }

    private CategoryResponse toResponse(Category category) {

        return new CategoryResponse(
                category.id,
                category.name,
                category.slug,
                category.imageUrl,
                category.parent == null ? null : category.parent.id,
                productRepository.countByCategory_IdAndActiveTrue(category.id),
                List.of()
        );
    }
}
