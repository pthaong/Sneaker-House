
package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Nationalized
    @Column(nullable = false, length = 120)
    public String name;

    @Nationalized
    @Column(name = "slug", length = 140, unique = true)
    public String slug;

    @Column(name = "image_url", length = 500)
    public String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    public Category parent;

    @Column(nullable = false)
    public Boolean active = true;
}
