package com.vincent.hris.master.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class RefCityMunicipality {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String psgcCode;

	private String citymunDesc;

	private String regDesc;

	private String provCode;

	private String citymunCode;
}