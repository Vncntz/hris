package com.vincent.hris.master.service;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import com.vincent.hris.master.model.RefBrgy;
import com.vincent.hris.master.repository.RefBrgyRepository;

@Service
public class RefBrgyService {

	private final RefBrgyRepository refBrgyRepository;

	public RefBrgyService(RefBrgyRepository refBrgyRepository) {
		this.refBrgyRepository = refBrgyRepository;
	}

	public List<RefBrgy> getAll() {
		return refBrgyRepository.findAll();
	}

	public RefBrgy getById(Integer id) {
		return refBrgyRepository.findById(id).orElse(null);
	}

	public List<RefBrgy> getByCitymunCode(String citymunCode) {
		return refBrgyRepository.findByCitymunCode(citymunCode);
	}

	public List<RefBrgy> getByProvCode(String provCode) {
		return refBrgyRepository.findByProvCode(provCode);
	}

	public List<RefBrgy> getByRegCode(String regCode) {
		return refBrgyRepository.findByRegCode(regCode);
	}

	public RefBrgy save(RefBrgy refBrgy) {
		return refBrgyRepository.save(refBrgy);
	}

	public void deleteById(Integer id) {
		refBrgyRepository.deleteById(id);
	}

	public List<RefBrgy> findAll() {
		return refBrgyRepository.findAll();
	}
}
