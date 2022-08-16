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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import javax.persistence.ForeignKey;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.UpdateTimestamp;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@PlanningEntity
@Entity
@Table(schema = "NSS")
public class Staff {

	@PlanningId
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(nullable = false)
	private Long id;

	@NotBlank
	@Column(nullable = false)
	private String cardID;
	@NotBlank
	@Column(nullable = false)
	private String name;
	@NotBlank
	@Column(nullable = false)
	private String staffGroup;

	@PlanningVariable(valueRangeProviderRefs = "timeslotRange")
	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
	private Timeslot timeslot;

	@PlanningVariable(valueRangeProviderRefs = "shiftRange")
	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
	private Shift shift;

	@UpdateTimestamp
	@Column(nullable = false)
	private Timestamp ZStaff;

	public Staff() {
	}

	public Staff(String cardID, String name, String staffGroup) {
		this.cardID = cardID.trim();
		this.name = name.trim();
		this.staffGroup = staffGroup.trim();
	}

	public Staff(long id, String cardID, String name, String staffGroup, Shift shift, Timeslot timeslot) {
		this(cardID, name, staffGroup);
		this.id = id;
		this.shift = shift;
		this.timeslot = timeslot;
	}

	@Override
	public String toString() {
		return cardID + "(" + id + ")";
	}

	// ************************************************************************
	// Getters and setters
	// ************************************************************************

	public Long getId() {
		return id;
	}

	public String getCardID() {
		return cardID;
	}

	public String getName() {
		return name;
	}

	public String getStaffGroup() {
		return staffGroup;
	}

	public Timeslot getTimeslot() {
		return timeslot;
	}

	public void setTimeslot(Timeslot timeslot) {
		this.timeslot = timeslot;
	}

	public Shift getShift() {
		return shift;
	}

	public void setShift(Shift shift) {
		this.shift = shift;
	}

	public Timestamp getZStaff() {
		return ZStaff;
	}

	public void setZStaff(Timestamp ZStaff) {
		this.ZStaff = ZStaff;
	}
}
