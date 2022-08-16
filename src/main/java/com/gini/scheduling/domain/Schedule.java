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
import java.time.DayOfWeek;
import java.time.LocalTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.UpdateTimestamp;
import org.optaplanner.core.api.domain.lookup.PlanningId;

@Entity
@Table(name="SGSCHED",schema = "SG", uniqueConstraints = @UniqueConstraint(columnNames = { "dayOfWeek",
		"startTime", "endTime" }))
public class Schedule {

	@PlanningId
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(nullable = false)
	private Long id;

	@Column(nullable = false)
	private DayOfWeek dayOfWeek;
	@Column(nullable = false)
	private LocalTime startTime;
	@Column(nullable = false)
	private LocalTime endTime;

	@UpdateTimestamp
	@Column(nullable = false)
	private Timestamp ZSchedule;

	public Schedule() {
	}

	public Schedule(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
		this.dayOfWeek = dayOfWeek;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public Schedule(long id, DayOfWeek dayOfWeek, LocalTime startTime) {
		this(dayOfWeek, startTime, startTime.plusMinutes(50));
		this.id = id;
	}

	@Override
	public String toString() {
		return dayOfWeek + " " + startTime;
	}

	// ************************************************************************
	// Getters and setters
	// ************************************************************************

	public Long getId() {
		return id;
	}

	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public Timestamp getZSchedule() {
		return ZSchedule;
	}

	public void setZSchedule(Timestamp ZSchedule) {
		this.ZSchedule = ZSchedule;
	}
}
