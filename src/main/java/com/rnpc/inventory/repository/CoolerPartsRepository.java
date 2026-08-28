package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.CoolerParts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoolerPartsRepository extends JpaRepository<CoolerParts, Integer> {
}
