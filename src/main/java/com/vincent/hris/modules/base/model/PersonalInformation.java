package com.vincent.hris.modules.base.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class PersonalInformation extends BaseEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@OneToOne
	private Employee employee;

	private String employeeCode;
	private String title;
	private String firstName;
	private String middleName;
	private String lastName;
	private String ext;

	private LocalDate birthDate;
	private String birthPlace;
	private Integer age;
	private String gender;
	private String civilStatus;

	private LocalDate dateHired;
	private boolean isEmployeeActive;
	private LocalDate contractExpiry;
	private LocalDate dateSeparated;

	private String department;
	private String position;
	private String employmentType;
	private String division;

	private LocalDate probationaryDate;
	private LocalDate regularizationDate;

	private String religion;
	private Double height;
	private Double weight;

	private String branch;
}