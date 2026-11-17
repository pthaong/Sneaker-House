package com.sneakerhouse.backend.entity;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="home_banners") public class HomeBanner {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=150) public String title;
 @Column(name="image_url",nullable=false,length=600) public String imageUrl;
 @Column(name="target_url",length=600) public String targetUrl;
 @Column(name="banner_type",nullable=false,length=30) public String bannerType="MAIN";
 @Column(nullable=false) public Integer position=0;
 @Column(nullable=false) public Boolean active=true;
 @Column(name="created_at") public LocalDateTime createdAt=LocalDateTime.now();
}
