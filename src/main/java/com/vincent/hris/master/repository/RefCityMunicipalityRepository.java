package com.vincent.hris.master.repository;

import com.vincent.hris.master.model.RefCityMunicipality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefCityMunicipalityRepository extends JpaRepository<RefCityMunicipality, Integer> {

	List<RefCityMunicipality> findByProvCode(String provCode);

	List<RefCityMunicipality> findByRegDesc(String regDesc);

	List<RefCityMunicipality> findByCitymunCode(String citymunCode);

	List<RefCityMunicipality> findByCitymunDescContainingIgnoreCase(String keyword);
}
