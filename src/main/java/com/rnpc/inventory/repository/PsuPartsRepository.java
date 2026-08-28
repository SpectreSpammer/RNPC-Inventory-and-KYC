package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.PsuParts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PsuPartsRepository extends JpaRepository<PsuParts, Integer> {
}
