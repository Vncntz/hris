package com.vincent.hris.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vincent.hris.master.model.RefBrgy;

@Repository
public interface RefBrgyRepository extends JpaRepository<RefBrgy, Integer> {

	List<RefBrgy> findByCitymunCode(String citymunCode);

	List<RefBrgy> findByProvCode(String provCode);

	List<RefBrgy> findByRegCode(String regCode);
}
