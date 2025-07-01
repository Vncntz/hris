package com.vincent.hris.master.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vincent.hris.master.model.Title;
import com.vincent.hris.master.repository.TitleRepository;

@Service
public class TitleService {

	@Autowired
	private TitleRepository titleRepository;

	public List<Title> findAll() {
		return titleRepository.findAll();
	}

	public void delete(Title module) {
		titleRepository.delete(module);
	}

	public void save(Title title) {
		titleRepository.save(title);
	}
}
