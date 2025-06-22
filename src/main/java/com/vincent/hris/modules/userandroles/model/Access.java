package com.vincent.hris.modules.userandroles.model;

import com.vincent.hris.modules.base.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Access extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne
	private Role role;

	@ManyToOne
	private Modules module;
}
