
package com.sneakerhouse.backend.dto;

import java.util.List;

/**
 * DTO tra du lieu danh muc cho Frontend.
 *
 * id: ID danh muc
 * name: Ten danh muc
 * slug: Duong dan dinh danh
 * imageUrl: Anh danh muc
 * parentId: ID danh muc cha
 * active: Trang thai hoat dong
 * children: Danh sach danh muc con
 *
 * DTO khong chua JPA Entity hay Hibernate Proxy.
 */
public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String imageUrl,
        Long parentId,
        Boolean active,
        List<CategoryResponse> children
) {
}