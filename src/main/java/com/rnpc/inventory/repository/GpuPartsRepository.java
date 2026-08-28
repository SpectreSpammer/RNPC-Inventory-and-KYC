package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.GpuParts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GpuPartsRepository extends JpaRepository<GpuParts, Integer> {
}
