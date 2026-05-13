package com.example.lineofduty.domain.banner.repository;

import com.example.lineofduty.domain.banner.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByIsActiveTrueOrderByOrderIndexAsc();

    List<Banner> findAllByOrderByOrderIndexAsc();
}
