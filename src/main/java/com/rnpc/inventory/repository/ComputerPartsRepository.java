package com.rnpc.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rnpc.inventory.entity.Computers;
import org.springframework.stereotype.Repository;

@Repository
public interface ComputerPartsRepository extends JpaRepository<Computers, Integer> {

}
