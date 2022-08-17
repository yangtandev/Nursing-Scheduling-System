/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gini.scheduling.domain;

import org.springframework.stereotype.Component;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import javax.persistence.ForeignKey;

import java.sql.Timestamp;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@Component
@PlanningEntity
@Entity
@Table(name = "SGSCHED", schema = "SG")
public class Schedule {

	@PlanningId
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false)
	private Long id;

	@PlanningVariable(valueRangeProviderRefs = "staffRange")
	@ManyToOne
	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
	private Staff staff;

	@PlanningVariable(valueRangeProviderRefs = "datesRange")
	@OneToMany(mappedBy="shift",cascade=CascadeType.PERSIST)
	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
	private Dates dates;

	@PlanningVariable(valueRangeProviderRefs = "shiftRange")
	@ManyToOne
	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
	private Shift shift;

	@UpdateTimestamp
	@Column(nullable = false)
	private Timestamp zsgsched;

	public Schedule() {
	}

	public Schedule(Staff staff, Shift shift, Dates dates) {
		this.staff = staff;
		this.shift = shift;
		this.dates = dates;
	}

	public Schedule(long id, Staff staff, Shift shift, Dates dates) {
		this.id = id;
		this.staff = staff;
		this.shift = shift;
		this.dates = dates;
	}

	// ************************************************************************
	// Getters and setters
	// ************************************************************************

	public Long getId() {
		return id;
	}

	public Staff getStaff() {
		return staff;
	}

	public void setStaff(Staff staff) {
		this.staff = staff;
	}

	public Dates getDates() {
		return dates;
	}

	public void setDates(Dates dates) {
		this.dates = dates;
	}

	public Shift getShift() {
		return shift;
	}

	public void setShift(Shift shift) {
		this.shift = shift;
	}

	public Timestamp getzsgsched() {
		return zsgsched;
	}

	public void setzsgsched(Timestamp zsgsched) {
		this.zsgsched = zsgsched;
	}
}
