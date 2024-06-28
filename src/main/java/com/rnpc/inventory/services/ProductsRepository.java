package com.rnpc.inventory.services;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rnpc.inventory.models.Products;

public interface ProductsRepository extends JpaRepository<Products,Integer> {

}
