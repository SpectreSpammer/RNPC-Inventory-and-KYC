package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.CaseParts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CasePartsRepository extends JpaRepository<CaseParts, Integer> {
}
