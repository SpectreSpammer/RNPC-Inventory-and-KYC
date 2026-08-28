package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.RamParts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RamPartsRepository extends JpaRepository<RamParts, Integer> {
}
