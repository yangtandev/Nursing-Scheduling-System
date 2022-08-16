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

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.UpdateTimestamp;
import org.optaplanner.core.api.domain.lookup.PlanningId;

@Entity
@Table(schema = "NSS", uniqueConstraints = @UniqueConstraint(columnNames = { "name" }))
public class Shift {

	@PlanningId
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(nullable = false)
	private Long id;

	@NotBlank
	@Column(nullable = false)
	private String name;

	@UpdateTimestamp
	@Column(nullable = false)
	private Timestamp ZShift;

	public Shift() {
	}

	public Shift(String name) {
		this.name = name.trim();
	}

	public Shift(long id, String name) {
		this(name);
		this.id = id;
	}

	@Override
	public String toString() {
		return name;
	}

	// ************************************************************************
	// Getters and setters
	// ************************************************************************

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Timestamp getZShift() {
		return ZShift;
	}

	public void setZShift(Timestamp ZShift) {
		this.ZShift = ZShift;
	}
}
