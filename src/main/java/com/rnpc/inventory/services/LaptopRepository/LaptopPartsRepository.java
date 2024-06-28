package com.rnpc.inventory.services.LaptopRepository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rnpc.inventory.models.laptop.LaptopParts;

public interface LaptopPartsRepository extends JpaRepository<LaptopParts,Integer> {

}
