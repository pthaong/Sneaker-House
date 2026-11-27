
package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "recipient_name", nullable = false, length = 120,
            columnDefinition = "NVARCHAR(120)")
    private String recipientName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 100,
            columnDefinition = "NVARCHAR(100)")
    private String province;

    @Column(nullable = false, length = 100,
            columnDefinition = "NVARCHAR(100)")
    private String district;

    @Column(nullable = false, length = 100,
            columnDefinition = "NVARCHAR(100)")
    private String ward;

    @Column(name = "detail_address", nullable = false, length = 255,
            columnDefinition = "NVARCHAR(255)")
    private String detailAddress;

    @Column(name = "default_address", nullable = false)
    private boolean defaultAddress = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }

    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String value) { recipientName = value; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String value) { phoneNumber = value; }

    public String getProvince() { return province; }
    public void setProvince(String value) { province = value; }

    public String getDistrict() { return district; }
    public void setDistrict(String value) { district = value; }

    public String getWard() { return ward; }
    public void setWard(String value) { ward = value; }

    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String value) { detailAddress = value; }

    public boolean isDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(boolean value) { defaultAddress = value; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
