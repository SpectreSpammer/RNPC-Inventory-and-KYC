package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.StorageParts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoragePartsRepository extends JpaRepository<StorageParts, Integer> {
}
