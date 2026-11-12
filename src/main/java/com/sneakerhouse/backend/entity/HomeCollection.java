package com.sneakerhouse.backend.entity;
import jakarta.persistence.*;
@Entity @Table(name="home_collections") public class HomeCollection {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=150) public String title;
 @Column(length=500) public String description;
 @Column(name="image_url",length=600) public String imageUrl;
 @Column(name="target_url",length=600) public String targetUrl;
 @Column(nullable=false) public Boolean active=true;
}
