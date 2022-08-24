package com.gini.scheduling.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

import com.gini.scheduling.utils.UUIDGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.optaplanner.core.api.domain.lookup.PlanningId;

@Entity
@Table(name = "SGSTAFF", schema = "SG")
@IdClass(StaffId.class)
public class Staff implements Serializable {
    @PlanningId
    @Id
    @Column(columnDefinition = "char(100) not null with default")
    private String id = UUIDGenerator.generateUUID22();
    @Id
    @Column(columnDefinition = "char(3) not null with default")
    private String hid = "2A0";

    @Column(columnDefinition = "char(100) not null with default")
    private String cardId;

    @Column(columnDefinition = "char(100) not null with default")
    private String name;

    @Column(columnDefinition = "char(100) not null with default")
    private String team;

    @UpdateTimestamp
    @Column(columnDefinition = "timestamp not null with default")
    private Timestamp zsgstaff;


    public Staff() {
    }

    public Staff(String cardId, String name, String team) {
        this.cardId = cardId.trim();
        this.name = name.trim();
        this.team = team.trim();
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
    public String getCardId() {
        return cardId;
    }

    public String getName() {
        return name;
    }

    public String getTeam() {
        return team;
    }


    public Timestamp getZsgstaff() {
        return zsgstaff;
    }

    public void setZsgstaff(Timestamp zsgstaff) {
        this.zsgstaff = zsgstaff;
    }
}
