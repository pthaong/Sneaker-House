package com.sneakerhouse.backend.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
@Entity @Table(name="users", uniqueConstraints={@UniqueConstraint(name="uk_users_email",columnNames="email"),@UniqueConstraint(name="uk_users_google_sub",columnNames="google_sub")})
public class UserAccount {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
@Column(nullable=false,length=254) private String email;
@Column(name="full_name",nullable=false,length=120) private String fullName;
@Column(name="password_hash",length=100) private String passwordHash;
@Column(name="google_sub",length=255,unique=true) private String googleSub;
@Column(nullable=false,length=30) private String role="CUSTOMER";
@Column(name="phone_number",length=20) private String phoneNumber;
@Column(name="birth_date") private LocalDate birthDate;
@Column(length=20) private String gender;
@Column(name="avatar_url",length=500) private String avatarUrl;
@Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
@PrePersist void onCreate(){createdAt=Instant.now();normalizeEmail();}
@PreUpdate void onUpdate(){normalizeEmail();}
private void normalizeEmail(){if(email!=null)email=email.trim().toLowerCase();}
public Long getId(){return id;} public String getEmail(){return email;} public void setEmail(String v){email=v;}
public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
public String getGoogleSub(){return googleSub;} public void setGoogleSub(String v){googleSub=v;}
public String getRole(){return role;} public Instant getCreatedAt(){return createdAt;}
public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String v){phoneNumber=v;}
public LocalDate getBirthDate(){return birthDate;} public void setBirthDate(LocalDate v){birthDate=v;}
public String getGender(){return gender;} public void setGender(String v){gender=v;}
public String getAvatarUrl(){return avatarUrl;} public void setAvatarUrl(String v){avatarUrl=v;}
}
