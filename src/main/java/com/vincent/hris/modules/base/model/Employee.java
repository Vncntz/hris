package com.vincent.hris.modules.base.model;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class Employee extends BaseEntity {

	private static final long serialVersionUID = -1878486353344852888L;

	private String firstName;
	private String lastName;
	private String middleName;
	private String email;
	private String phone;
	private String address;

	public String getFullName() {
		return (firstName != null ? firstName : "") + " " +
			   (middleName != null ? middleName : "") + " " +
			   (lastName != null ? lastName : "");
	}
}
