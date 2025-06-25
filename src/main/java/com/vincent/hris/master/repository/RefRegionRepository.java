package com.vincent.hris.master.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vincent.hris.master.model.RefRegion;

@Repository
public interface RefRegionRepository extends JpaRepository<RefRegion, Integer> {

	RefRegion findByRegCode(String regCode);
}
