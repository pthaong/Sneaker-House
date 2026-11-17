package com.sneakerhouse.backend.repository;
import com.sneakerhouse.backend.entity.HomeCollection; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface HomeCollectionRepository extends JpaRepository<HomeCollection,Long>{ List<HomeCollection> findByActiveTrueOrderByIdDesc(); }
