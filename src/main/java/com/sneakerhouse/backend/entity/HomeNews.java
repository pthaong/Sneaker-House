package com.sneakerhouse.backend.entity;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="home_news") public class HomeNews {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=180) public String title;
 @Column(length=1000) public String summary;
 @Column(name="image_url",length=600) public String imageUrl;
 @Column(name="article_url",length=600) public String articleUrl;
 @Column(name="published_at") public LocalDateTime publishedAt;
 @Column(nullable=false) public Boolean active=true;
}
