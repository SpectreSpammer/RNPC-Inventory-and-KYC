package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.SavedBuild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedBuildRepository extends JpaRepository<SavedBuild, Long> {
    List<SavedBuild> findByUser_UsernameOrderBySavedBuildIdDesc(String username);
}
