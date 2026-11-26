package com.sneakerhouse.backend.repository;
import com.sneakerhouse.backend.entity.HomeNews; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface HomeNewsRepository extends JpaRepository<HomeNews,Long>{ List<HomeNews> findTop8ByActiveTrueOrderByPublishedAtDesc(); }
