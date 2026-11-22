
package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "brands")
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Nationalized
    @Column(nullable = false, length = 100)
    public String name;

    @Column(name = "logo_url", length = 500)
    public String logoUrl;

    @Column(nullable = false)
    public Boolean active = true;
}
