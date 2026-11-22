package com.sneakerhouse.backend.repository;
import com.sneakerhouse.backend.entity.HomeBanner; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface HomeBannerRepository extends JpaRepository<HomeBanner,Long>{ List<HomeBanner> findByActiveTrueOrderByPositionAsc(); List<HomeBanner> findByActiveTrueAndBannerTypeOrderByPositionAsc(String type); }
