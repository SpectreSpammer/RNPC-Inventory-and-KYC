package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.MotherboardParts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotherboardPartsRepository extends JpaRepository<MotherboardParts, Integer> {
}
