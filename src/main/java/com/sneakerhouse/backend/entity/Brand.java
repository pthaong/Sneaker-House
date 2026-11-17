package com.sneakerhouse.backend.entity;
import jakarta.persistence.*;
@Entity @Table(name="brands") public class Brand {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=100) public String name;
 @Column(length=500) public String logoUrl;
 @Column(nullable=false) public Boolean active=true;
}
