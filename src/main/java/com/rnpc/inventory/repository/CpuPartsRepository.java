package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.CpuParts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CpuPartsRepository extends JpaRepository<CpuParts, Integer> {
}
