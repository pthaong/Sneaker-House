package com.sneakerhouse.backend.repository;
import com.sneakerhouse.backend.entity.Brand; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface BrandRepository extends JpaRepository<Brand,Long>{ List<Brand> findByActiveTrueOrderByNameAsc(); }
