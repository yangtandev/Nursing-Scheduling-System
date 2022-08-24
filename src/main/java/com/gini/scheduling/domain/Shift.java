package com.gini.scheduling.domain;

import java.sql.Timestamp;
import java.time.LocalDate;
import javax.persistence.*;
import com.gini.scheduling.utils.UUIDGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@Entity
@PlanningEntity
@Table(name = "SGSHIFT", schema = "SG")
@IdClass(ShiftId.class)
public class Shift {
    @PlanningId
    @Id
    @Column(columnDefinition = "char(100) not null with default")
    private String id = UUIDGenerator.generateUUID22();

    @Id
    @Column(columnDefinition = "char(3) not null with default")
    private String hid = "2A0";
    @Column(columnDefinition = "char(100) not null with default")
    private String name;
    @Column(columnDefinition = "date not null with default '9999-12-31'")
    private LocalDate date;

    @Column(columnDefinition = "integer not null with default")
    private int week;

    @PlanningVariable(valueRangeProviderRefs = "staffRange")
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "staffid", referencedColumnName = "id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @JoinColumn(name = "staffhid", referencedColumnName = "hid", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private Staff staff;
    @UpdateTimestamp
    @Column(columnDefinition = "timestamp not null with default")
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
