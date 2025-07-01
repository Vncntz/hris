package com.vincent.hris.master.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.vincent.hris.master.model.RefProvince;
import com.vincent.hris.master.repository.RefProvinceRepository;

@Service
public class RefProvinceService {

	private final RefProvinceRepository repository;

	public RefProvinceService(RefProvinceRepository repository) {
		this.repository = repository;
	}

	public List<RefProvince> getAll() {
		return repository.findAll();
	}

	public RefProvince getById(Integer id) {
		return repository.findById(id).orElse(null);
	}

	public List<RefProvince> getByRegCode(String regCode) {
		return repository.findByRegCode(regCode);
	}

	public RefProvince save(RefProvince refProvince) {
		return repository.save(refProvince);
	}

	public void deleteById(Integer id) {
		repository.deleteById(id);
	}

	public List<RefProvince> findAll() {
		return repository.findAll();
	}
}
