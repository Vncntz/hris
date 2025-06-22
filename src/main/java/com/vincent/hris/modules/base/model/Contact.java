package com.vincent.hris.modules.base.model;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Contact extends BaseEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@OneToOne
	private Employee employee;

	private String email;
	private String telephoneNo;
	private String cellphoneNo;

	private String zipCode;
	private String area;

	private String presentBarangay;
	private String presentCity;

	private String permanentBarangay;
	private String permanentCity;
}
