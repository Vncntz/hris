package com.vincent.hris.master.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.vincent.hris.master.model.RefRegion;
import com.vincent.hris.master.repository.RefRegionRepository;

@Service
public class RefRegionService {

	private final RefRegionRepository repository;

	public RefRegionService(RefRegionRepository repository) {
		this.repository = repository;
	}

	public List<RefRegion> getAll() {
		return repository.findAll();
	}

	public RefRegion getById(Integer id) {
		return repository.findById(id).orElse(null);
	}

	public RefRegion save(RefRegion refRegion) {
		return repository.save(refRegion);
	}

	public void deleteById(Integer id) {
		repository.deleteById(id);
	}
}
