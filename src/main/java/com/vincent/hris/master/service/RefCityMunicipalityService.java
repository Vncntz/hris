package com.vincent.hris.master.service;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import com.vincent.hris.master.model.RefCityMunicipality;
import com.vincent.hris.master.repository.RefCityMunicipalityRepository;

@Service
public class RefCityMunicipalityService {

	private final RefCityMunicipalityRepository repository;

	public RefCityMunicipalityService(RefCityMunicipalityRepository repository) {
		this.repository = repository;
	}

	public List<RefCityMunicipality> getAll() {
		return repository.findAll();
	}

	public RefCityMunicipality getById(Integer id) {
		return repository.findById(id).orElse(null);
	}

	public List<RefCityMunicipality> getByProvCode(String provCode) {
		return repository.findByProvCode(provCode);
	}

	public List<RefCityMunicipality> getByRegDesc(String regDesc) {
		return repository.findByRegDesc(regDesc);
	}

	public List<RefCityMunicipality> getByCitymunCode(String citymunCode) {
		return repository.findByCitymunCode(citymunCode);
	}

	public List<RefCityMunicipality> searchByDescription(String keyword) {
		return repository.findByCitymunDescContainingIgnoreCase(keyword);
	}

	public RefCityMunicipality save(RefCityMunicipality refCityMunicipality) {
		return repository.save(refCityMunicipality);
	}

	public void deleteById(Integer id) {
		repository.deleteById(id);
	}

	public List<RefCityMunicipality> findAll() {
		return repository.findAll();
	}
}
