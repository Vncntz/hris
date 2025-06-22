package com.vincent.hris.modules.userandroles.model;

import com.vincent.hris.modules.base.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class SubMenu extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String route;

	private String description;
	@ManyToOne
	private Modules module;
	private String icon;
	private int sequence;
	private boolean active;
}
