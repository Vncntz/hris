package com.vincent.hris.modules.userandroles.model;

import com.vincent.hris.modules.base.model.BaseEntity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class Modules extends BaseEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private String description;
	private boolean active = true;
	private String icon;
	private int sequence;

}
