package com.gini.scheduling.domain;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import com.gini.scheduling.persistence.StaffRepository;
import com.gini.scheduling.utils.UUIDGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@PlanningEntity
@Entity
@Table(name = "SGSHIFT", schema = "SG")
public class Shift {
    @PlanningId
    @Id
    @Column(nullable = false, columnDefinition = "char(100) default")
    private String id = UUIDGenerator.generateUUID22();

    @NotBlank
    @Column(nullable = false, columnDefinition = "char(3) default")
    private String hid = "2A0";

    @NotBlank
    @Column(nullable = false, columnDefinition = "char(100) default")
    private String name;
    @Column(nullable = false, columnDefinition = "date default '9999-12-31'")
    private LocalDate date;

    @Column(nullable = false, columnDefinition = "integer default")
    private int week;

    @PlanningVariable(valueRangeProviderRefs = "staffRange")
    @ManyToOne
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private Staff staff;
    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "timestamp default current timestamp")
    private Timestamp zsgshift;

    public Shift() {
    }

    public Shift(String name, LocalDate date, int week, Staff staff) {
        this.name = name.trim();
        this.date = date;
        this.week = week;
        this.staff = staff;
    }

    @Override
    public String toString() {
        return id;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public Staff getStaff() {
        return staff;
    }


    public Timestamp getZsgshift() {
        return zsgshift;
    }

    public void setZsgshift(Timestamp zsgshift) {
        this.zsgshift = zsgshift;
    }


}
