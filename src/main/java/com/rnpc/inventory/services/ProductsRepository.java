package com.rnpc.inventory.services;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rnpc.inventory.models.computer.Computers;

public interface ProductsRepository extends JpaRepository<Computers,Integer> {

}
