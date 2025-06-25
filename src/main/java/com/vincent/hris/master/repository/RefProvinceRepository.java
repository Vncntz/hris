package com.vincent.hris.master.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vincent.hris.master.model.RefProvince;

@Repository
public interface RefProvinceRepository extends JpaRepository<RefProvince, Integer> {

	List<RefProvince> findByRegCode(String regCode);
}
