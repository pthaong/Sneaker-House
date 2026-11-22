package com.sneakerhouse.backend.entity;
import jakarta.persistence.*;
@Entity @Table(name="home_collections") public class HomeCollection {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @org.hibernate.annotations.Nationalized
@Column(nullable = false, length = 150, columnDefinition = "NVARCHAR(150)")
public String title;

@org.hibernate.annotations.Nationalized
@Column(length = 500, columnDefinition = "NVARCHAR(500)")
public String description;
 @Column(name="image_url",length=600) public String imageUrl;
 @Column(name="target_url",length=600) public String targetUrl;
 @Column(nullable=false) public Boolean active=true;
}
