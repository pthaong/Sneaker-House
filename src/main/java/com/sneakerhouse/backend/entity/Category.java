package com.sneakerhouse.backend.entity;
import jakarta.persistence.*; import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@Entity @Table(name="categories") public class Category {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=120) public String name;
 @Column(name="slug",length=140,unique=true) public String slug;
 @Column(name="image_url",length=500) public String imageUrl;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parent_id") @JsonIgnoreProperties({"parent"}) public Category parent;
 @Column(nullable=false) public Boolean active=true;
}
